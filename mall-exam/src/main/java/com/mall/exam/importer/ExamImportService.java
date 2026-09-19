package com.mall.exam.importer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.exam.importer.dto.ImportPreviewResponse;
import com.mall.exam.importer.mapper.ImportBatchMapper;
import com.mall.exam.importer.model.ImportBatchEntity;
import com.mall.exam.importer.model.ImportBatchItemEntity;
import com.mall.exam.question.mapper.QuestionMapper;
import com.mall.exam.question.model.QuestionEntity;
import com.mall.exam.question.model.QuestionGroupEntity;
import com.mall.exam.source.mapper.QuestionSourceMapper;
import com.mall.exam.source.model.QuestionSourceEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.util.*;

@Service
public class ExamImportService {
    private static final Set<String> REUSABLE = new HashSet<>(Arrays.asList("ORIGINAL", "AUTHORIZED", "PUBLIC_OFFICIAL"));
    private final QuestionSourceMapper sourceMapper; private final QuestionMapper questionMapper;
    private final ImportBatchMapper batchMapper; private final ExamImportParser parser; private final ObjectMapper json = new ObjectMapper();
    @Autowired
    public ExamImportService(QuestionSourceMapper sourceMapper, QuestionMapper questionMapper, ImportBatchMapper batchMapper) { this(sourceMapper, questionMapper, batchMapper, new ExamImportParser()); }
    ExamImportService(QuestionSourceMapper sourceMapper, QuestionMapper questionMapper, ImportBatchMapper batchMapper, ExamImportParser parser) { this.sourceMapper=sourceMapper; this.questionMapper=questionMapper; this.batchMapper=batchMapper; this.parser=parser; }

    @Transactional(rollbackFor = Exception.class)
    public ImportPreviewResponse preview(long sourceId, String filename, byte[] bytes) {
        ExamImportParser.ParsedPreview parsed=parser.parse(filename, bytes); List<String> warnings=new ArrayList<>(); List<ImportPreviewResponse.Item> accepted=new ArrayList<>(); Set<String> seen=new HashSet<>();
        for (ImportPreviewResponse.Item item: parsed.items) {
            boolean duplicate=!seen.add(item.getFingerprint()) || questionMapper.countQuestionsByContentFingerprint(item.getFingerprint())>0;
            if (item.getGroup()!=null && questionMapper.countGroupsByContentFingerprint(item.getGroup().getFingerprint())>0) duplicate=true;
            if (duplicate) warnings.add("row "+item.getRowNumber()+": duplicate content fingerprint"); else accepted.add(item);
        }
        ImportBatchEntity batch=new ImportBatchEntity(); batch.setFileName(filename); batch.setFileFormat(format(filename)); batch.setSourceId(sourceId); batch.setStatus(parsed.errors.isEmpty()?"VALIDATED":"REJECTED"); batch.setTotalRows(parsed.totalRows); batch.setSuccessRows(accepted.size()); batch.setFailedRows(parsed.errors.size()); batch.setStructureErrors(json(parsed.errors)); batch.setDuplicateWarnings(json(warnings)); require(batchMapper.insertBatch(batch));
        for (ImportPreviewResponse.Item item:accepted) { ImportBatchItemEntity stored=new ImportBatchItemEntity(); stored.setBatchId(batch.getId()); stored.setRowNumber(item.getRowNumber()); stored.setPayload(json(item)); stored.setQuestionFingerprint(item.getFingerprint()); stored.setGroupFingerprint(item.getGroup()==null?null:item.getGroup().getFingerprint()); stored.setStatus("PENDING"); require(batchMapper.insertItem(stored)); }
        return new ImportPreviewResponse(batch.getId(), parsed.totalRows, accepted.size(), warnings.size(), parsed.errors, accepted);
    }

    @Transactional(rollbackFor = Exception.class)
    public void commit(long batchId, long adminId) {
        ImportBatchEntity batch=batchMapper.findBatchByIdForUpdate(batchId); if (batch==null || !"VALIDATED".equals(batch.getStatus())) throw invalid();
        QuestionSourceEntity source=sourceMapper.findByIdForUpdate(batch.getSourceId()); if (source==null || !"APPROVED".equals(source.getReviewStatus()) || !REUSABLE.contains(source.getCopyrightStatus())) throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.EXAM_SOURCE_UNAPPROVED);
        List<ImportBatchItemEntity> stored=batchMapper.findItemsByBatchIdForUpdate(batchId); List<String> warnings=list(batch.getDuplicateWarnings()); int imported=0; Map<String,List<ImportBatchItemEntity>> units=new LinkedHashMap<>();
        for (ImportBatchItemEntity item:stored) if ("PENDING".equals(item.getStatus())) units.computeIfAbsent(item.getGroupFingerprint()==null?"#"+item.getId():item.getGroupFingerprint(), k->new ArrayList<>()).add(item);
        for (List<ImportBatchItemEntity> members:units.values()) imported+=persistUnit(batchId,batch.getSourceId(),members,warnings);
        require(batchMapper.markImported(batchId,adminId,imported,json(warnings)));
    }

    private int persistUnit(long batchId,long sourceId,List<ImportBatchItemEntity> stored,List<String> warnings) {
        List<ImportPreviewResponse.Item> members=new ArrayList<>(); for(ImportBatchItemEntity item:stored) members.add(read(item.getPayload())); ImportPreviewResponse.Group group=members.get(0).getGroup();
        if (group==null) return persistStandalone(batchId,sourceId,stored.get(0),members.get(0),warnings);
        if (batchMapper.reserveFingerprint(group.getFingerprint(),"GROUP",batchId)!=1) return duplicate(stored,warnings,"concurrent duplicate group");
        List<Integer> accepted=new ArrayList<>(); for(int i=0;i<members.size();i++) if(batchMapper.reserveFingerprint(members.get(i).getFingerprint(),"QUESTION",batchId)==1) accepted.add(i); else markDuplicate(stored.get(i),warnings,"concurrent duplicate question");
        if(accepted.isEmpty()){ batchMapper.releaseFingerprint(group.getFingerprint(),batchId); return 0; }
        QuestionGroupEntity entity=group(group,sourceId,members.get(accepted.get(0))); require(questionMapper.insertGroup(entity)); int count=0, order=0;
        for(Integer index:accepted){ ImportPreviewResponse.Item item=members.get(index); QuestionEntity question=question(item,sourceId,entity.getId(),item.getGroupSortOrder()==null?++order:item.getGroupSortOrder()); require(questionMapper.insert(question)); require(batchMapper.markItemImported(stored.get(index).getId())); count++; }
        return count;
    }
    private int persistStandalone(long batchId,long sourceId,ImportBatchItemEntity stored,ImportPreviewResponse.Item item,List<String>warnings){ if(batchMapper.reserveFingerprint(item.getFingerprint(),"QUESTION",batchId)!=1) return duplicate(Collections.singletonList(stored),warnings,"concurrent duplicate question"); require(questionMapper.insert(question(item,sourceId,null,null))); require(batchMapper.markItemImported(stored.getId())); return 1; }
    private int duplicate(List<ImportBatchItemEntity> items,List<String>warnings,String reason){for(ImportBatchItemEntity item:items) markDuplicate(item,warnings,reason); return 0;}
    private void markDuplicate(ImportBatchItemEntity item,List<String>warnings,String reason){require(batchMapper.markItemDuplicate(item.getId())); warnings.add("row "+item.getRowNumber()+": "+reason);}
    private QuestionGroupEntity group(ImportPreviewResponse.Group g,long sourceId,ImportPreviewResponse.Item item){QuestionGroupEntity e=new QuestionGroupEntity();e.setGroupType(g.getGroupType());e.setTitle(g.getTitle());e.setInstruction(g.getInstruction());e.setContent(g.getContent());e.setSharedOptions(g.getSharedOptions());e.setSourceId(sourceId);e.setDifficulty(item.getDifficulty());e.setKnowledgePoints(item.getKnowledgePoints());e.setContentFingerprint(g.getFingerprint());e.setReviewStatus("DRAFT");e.setEnabled(1);return e;}
    private QuestionEntity question(ImportPreviewResponse.Item i,long sourceId,Long groupId,Integer order){QuestionEntity q=new QuestionEntity();q.setGroupId(groupId);q.setQuestionType(i.getQuestionType());q.setStem(i.getStem());q.setOptions(i.getOptions());q.setCorrectAnswer(i.getCorrectAnswer());q.setExplanation(i.getExplanation());q.setReferenceAnswer(i.getReferenceAnswer());q.setSampleAnswer(i.getSampleAnswer());q.setScoringRubric(i.getScoringRubric());q.setSourceId(sourceId);q.setGroupSortOrder(order);q.setDifficulty(i.getDifficulty());q.setKnowledgePoints(i.getKnowledgePoints());q.setContentFingerprint(i.getFingerprint());q.setReviewStatus("DRAFT");q.setEnabled(1);return q;}
    private ImportPreviewResponse.Item read(String payload){try{JsonNode n=json.readTree(payload),g=n.get("group");ImportPreviewResponse.Group group=g==null||g.isNull()?null:new ImportPreviewResponse.Group(t(g,"groupType"),t(g,"title"),t(g,"instruction"),t(g,"content"),t(g,"sharedOptions"),t(g,"fingerprint"));return new ImportPreviewResponse.Item(n.path("rowNumber").asInt(),t(n,"questionType"),t(n,"stem"),t(n,"options"),t(n,"correctAnswer"),t(n,"explanation"),t(n,"referenceAnswer"),t(n,"sampleAnswer"),t(n,"scoringRubric"),n.path("difficulty").asInt(),t(n,"knowledgePoints"),group,n.hasNonNull("groupSortOrder")?n.get("groupSortOrder").asInt():null,t(n,"fingerprint"));}catch(IOException e){throw invalid();}}
    private String t(JsonNode n,String name){JsonNode v=n.get(name);return v==null||v.isNull()?null:v.asText();}
    private List<String> list(String value){try{return value==null?new ArrayList<>():json.readValue(value,json.getTypeFactory().constructCollectionType(List.class,String.class));}catch(IOException e){throw invalid();}}
    private String json(Object value){try{return json.writeValueAsString(value);}catch(IOException e){throw new IllegalStateException(e);}}
    private static String format(String filename){int i=filename==null?-1:filename.lastIndexOf('.');return i<0?"":filename.substring(i+1).toLowerCase(Locale.ROOT);}
    private static void require(int rows){if(rows!=1)throw new BusinessException(HttpStatus.CONFLICT,ErrorCode.DATA_CONFLICT);}
    private static BusinessException invalid(){return new BusinessException(HttpStatus.BAD_REQUEST,ErrorCode.EXAM_IMPORT_INVALID);}
}
