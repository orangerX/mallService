package com.mall.exam.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.exam.admin.dto.AdminExamRequests.*;
import com.mall.exam.importer.ExamImportParser;
import com.mall.exam.importer.dto.ImportPreviewResponse.Item;
import com.mall.exam.importer.dto.ImportPreviewResponse.Group;
import com.mall.exam.question.mapper.*;
import com.mall.exam.question.model.*;
import com.mall.exam.question.service.QuestionGovernanceService;
import com.mall.exam.source.mapper.QuestionSourceMapper;
import com.mall.exam.source.model.QuestionSourceEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/** Transactional draft editing; review transitions remain owned by QuestionGovernanceService. */
@Service
@Transactional(rollbackFor=Exception.class)
public class AdminExamManagementService {
    private static final Logger log=LoggerFactory.getLogger(AdminExamManagementService.class);
    private final QuestionSourceMapper sources;
    private final QuestionMapper questions;
    private final AdminExamMapper mapper;
    private final QuestionGovernanceService governance;
    private final ObjectMapper json=new ObjectMapper().setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
    private final ExamImportParser parser=new ExamImportParser();
    public AdminExamManagementService(QuestionSourceMapper sources,QuestionMapper questions,AdminExamMapper mapper,QuestionGovernanceService governance) {
        this.sources=sources; this.questions=questions; this.mapper=mapper; this.governance=governance;
    }
    public long createSource(Source request,long adminId) {
        actor(adminId); QuestionSourceEntity source=source(request); source.setReviewStatus("DRAFT");
        changed(sources.insert(source)); audit("source.create",source.getId(),adminId); return source.getId();
    }
    public void updateSource(SourceUpdate request,long adminId) {
        actor(adminId); QuestionSourceEntity existing=requiredSource(request.sourceId);
        editable(existing.getReviewStatus());
        QuestionSourceEntity source=source(request); source.setId(request.sourceId);
        changed(sources.update(source)); audit("source.update",request.sourceId,adminId);
    }
    public void sourceStatus(long id,int enabled,long adminId) {
        actor(adminId); status(enabled); changed(mapper.sourceStatus(id,enabled)); audit("source.status",id,adminId);
    }
    public void questionStatus(long id,int enabled,long adminId) {
        actor(adminId); governance.changeQuestionStatus(id,enabled); audit("question.status",id,adminId);
    }
    public void blueprintStatus(long id,String status,long adminId) {
        actor(adminId);
        if(!"ENABLED".equals(status) && !"DISABLED".equals(status)) throw invalid();
        changed(mapper.blueprintStatus(id,status)); audit("blueprint.status",id,adminId);
    }
    public long createQuestion(Question request,long adminId) {
        actor(adminId); requiredSource(request.sourceId);
        List<Item> items=items(request); Long groupId=null;
        if(items.get(0).getGroup()!=null) {
            QuestionGroupEntity group=group(items.get(0),request.sourceId);
            changed(questions.insertGroup(group)); groupId=group.getId();
        }
        Long firstId=null; int order=0;
        for(Item item:items) {
            QuestionEntity q=question(item,request.sourceId,groupId,groupId==null?null:++order);
            changed(questions.insert(q)); if(firstId==null) firstId=q.getId();
        }
        audit("question.create",firstId,adminId); return firstId;
    }
    public void updateQuestion(QuestionUpdate request,long adminId) {
        actor(adminId); requiredSource(request.sourceId);
        QuestionEntity original=questions.findById(request.questionId);
        if(original==null) throw missing();
        editable(original.getReviewStatus());
        List<Item> items=items(request);
        if(original.getGroupId()==null) {
            if(items.size()!=1 || items.get(0).getGroup()!=null) throw conflict();
            QuestionEntity replacement=question(items.get(0),request.sourceId,null,null);
            replacement.setId(original.getId()); changed(mapper.updateDraftQuestion(replacement));
        } else {
            QuestionGroupEntity existing=questions.findGroupByIdForUpdate(original.getGroupId());
            if(existing==null) throw missing();
            editable(existing.getReviewStatus());
            List<QuestionEntity> members=questions.findByGroupIdForUpdate(existing.getId());
            members.sort(Comparator.comparing(QuestionEntity::getGroupSortOrder,Comparator.nullsLast(Integer::compareTo)).thenComparing(QuestionEntity::getId));
            if(items.size()!=members.size() || items.get(0).getGroup()==null || mapper.countGroupSnapshots(existing.getId())>0) throw conflict();
            QuestionGroupEntity replacement=group(items.get(0),request.sourceId); replacement.setId(existing.getId());
            changed(mapper.updateDraftGroup(replacement));
            for(int i=0;i<members.size();i++) {
                editable(members.get(i).getReviewStatus());
                QuestionEntity q=question(items.get(i),request.sourceId,existing.getId(),members.get(i).getGroupSortOrder());
                q.setId(members.get(i).getId()); changed(mapper.updateDraftQuestion(q));
            }
        }
        audit("question.update",request.questionId,adminId);
    }
    private List<Item> items(Question request) {
        if(request.content==null) throw invalid();
        List<Item> items=parser.validateContent(json.valueToTree(request.content));
        Group group=items.get(0).getGroup();
        if(group!=null && "READING".equals(group.getGroupType()) && items.size()!=5) throw invalid();
        Set<String> unique=new HashSet<>();
        for(Item item:items) if(!unique.add(item.getFingerprint())) throw invalid();
        return items;
    }
    private QuestionSourceEntity requiredSource(Long id) {
        QuestionSourceEntity result=id==null?null:sources.findByIdForUpdate(id);
        if(result==null) throw missing(); return result;
    }
    private static QuestionSourceEntity source(Source r) {
        QuestionSourceEntity s=new QuestionSourceEntity(); s.setSourceName(r.sourceName); s.setSourceType(r.sourceType);
        s.setOriginalUrl(r.originalUrl); s.setCopyrightStatus(r.copyrightStatus); s.setCopyrightNote(r.copyrightNote); return s;
    }
    private static QuestionGroupEntity group(Item i,long sourceId) {
        Group g=i.getGroup(); QuestionGroupEntity q=new QuestionGroupEntity();
        q.setGroupType(g.getGroupType());q.setTitle(g.getTitle());q.setContent(g.getContent());q.setInstruction(g.getInstruction());
        q.setSharedOptions(g.getSharedOptions());q.setSourceId(sourceId);q.setDifficulty(i.getDifficulty());
        q.setKnowledgePoints(i.getKnowledgePoints());q.setContentFingerprint(g.getFingerprint());q.setReviewStatus("DRAFT");q.setEnabled(1);return q;
    }
    private static QuestionEntity question(Item i,long sourceId,Long groupId,Integer order) {
        QuestionEntity q=new QuestionEntity();q.setSourceId(sourceId);q.setGroupId(groupId);q.setGroupSortOrder(order);
        q.setQuestionType(i.getQuestionType());q.setStem(i.getStem());q.setOptions(i.getOptions());q.setCorrectAnswer(i.getCorrectAnswer());
        q.setExplanation(i.getExplanation());q.setReferenceAnswer(i.getReferenceAnswer());q.setSampleAnswer(i.getSampleAnswer());
        q.setScoringRubric(i.getScoringRubric());q.setDifficulty(i.getDifficulty());q.setKnowledgePoints(i.getKnowledgePoints());
        q.setContentFingerprint(i.getFingerprint());q.setReviewStatus("DRAFT");q.setEnabled(1);return q;
    }
    private static void editable(String status) { if(!Arrays.asList("DRAFT","PENDING","REJECTED").contains(status)) throw conflict(); }
    private static void status(int status) { if(status!=0 && status!=1) throw invalid(); }
    private static void actor(long id) { if(id<1) throw invalid(); }
    private static void changed(int rows) { if(rows!=1) throw conflict(); }
    private static void audit(String action,Long id,long adminId) { log.info("Exam admin action={} entityId={} adminId={}",action,id,adminId); }
    private static BusinessException conflict() { return new BusinessException(HttpStatus.CONFLICT,ErrorCode.DATA_CONFLICT); }
    private static BusinessException missing() { return new BusinessException(HttpStatus.NOT_FOUND,ErrorCode.EXAM_NOT_FOUND); }
    private static BusinessException invalid() { return new BusinessException(HttpStatus.BAD_REQUEST,ErrorCode.VALIDATION_ERROR); }
}
