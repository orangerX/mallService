package com.mall.exam.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.exam.admin.dto.AdminExamRequests.*;
import com.mall.exam.importer.ExamImportParser;
import com.mall.exam.importer.FingerprintCoordinator;
import com.mall.exam.importer.mapper.ImportBatchMapper;
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
    private final FingerprintCoordinator fingerprints;
    private final ObjectMapper json=new ObjectMapper().setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
    private final ExamImportParser parser=new ExamImportParser();
    public AdminExamManagementService(QuestionSourceMapper sources,QuestionMapper questions,AdminExamMapper mapper,QuestionGovernanceService governance,ImportBatchMapper reservations) {
        this.sources=sources; this.questions=questions; this.mapper=mapper; this.governance=governance;
        this.fingerprints=new FingerprintCoordinator(reservations);
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
        if(request.content.questionId!=null || memberInputs(request).stream().anyMatch(member->member.questionId!=null)) throw invalid();
        fingerprints.requireAll(fingerprints(items),Collections.emptySet());
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
        List<Item> items=items(request);
        if(original.getGroupId()==null) {
            original=mapper.questionForUpdate(request.questionId);
            if(original==null) throw missing();
            editable(original.getReviewStatus());
            if(questions.countPaperItemSnapshots(original.getId())>0) throw conflict();
            if(items.size()!=1 || items.get(0).getGroup()!=null) throw conflict();
            if(request.content.questionId!=null && !request.questionId.equals(request.content.questionId)) throw invalid();
            Set<String> previous=new HashSet<>();addFingerprint(previous,original.getContentFingerprint());
            Map<String,String> desired=fingerprints(items);
            fingerprints.requireAll(desired,previous);
            QuestionEntity replacement=question(items.get(0),request.sourceId,null,null);
            replacement.setId(original.getId()); changed(mapper.updateDraftQuestion(replacement));
            fingerprints.releaseObsolete(previous,desired.keySet());
        } else {
            QuestionGroupEntity existing=questions.findGroupByIdForUpdate(original.getGroupId());
            if(existing==null) throw missing();
            editable(existing.getReviewStatus());
            List<QuestionEntity> members=questions.findByGroupIdForUpdate(existing.getId());
            if(items.get(0).getGroup()==null || request.content.questionId!=null || mapper.countGroupSnapshots(existing.getId())>0) throw conflict();
            Map<Long,QuestionEntity> byId=new HashMap<>();Set<String> previous=new HashSet<>();
            addFingerprint(previous,existing.getContentFingerprint());
            for(QuestionEntity member:members) {
                editable(member.getReviewStatus());
                if(questions.countPaperItemSnapshots(member.getId())>0) throw conflict();
                byId.put(member.getId(),member);addFingerprint(previous,member.getContentFingerprint());
            }
            if(!byId.containsKey(request.questionId)) throw conflict();
            List<Content> submitted=memberInputs(request);Set<Long> retained=new HashSet<>();
            for(Content member:submitted) if(member.questionId!=null
                    && (!byId.containsKey(member.questionId) || !retained.add(member.questionId))) throw invalid();
            Map<String,String> desired=fingerprints(items);
            fingerprints.requireAll(desired,previous);
            QuestionGroupEntity replacement=group(items.get(0),request.sourceId); replacement.setId(existing.getId());
            changed(mapper.updateDraftGroup(replacement));
            for(QuestionEntity member:members) if(!retained.contains(member.getId()))
                changed(mapper.deleteDraftMember(member.getId(),existing.getId()));
            for(int i=0;i<items.size();i++) {
                QuestionEntity q=question(items.get(i),request.sourceId,existing.getId(),i+1);
                q.setId(submitted.get(i).questionId);
                if(q.getId()==null) changed(questions.insert(q)); else changed(mapper.updateDraftQuestion(q));
            }
            fingerprints.releaseObsolete(previous,desired.keySet());
        }
        audit("question.update",request.questionId,adminId);
    }
    private static List<Content> memberInputs(Question request) {
        return request.content.questions==null?Collections.singletonList(request.content):request.content.questions;
    }
    private static Map<String,String> fingerprints(List<Item> items) {
        Map<String,String> result=new TreeMap<>();
        for(Item item:items) {
            result.put(item.getFingerprint(),"QUESTION");
            if(item.getGroup()!=null) result.put(item.getGroup().getFingerprint(),"GROUP");
        }
        return result;
    }
    private static void addFingerprint(Set<String> keys,String key) { if(key!=null) keys.add(key); }
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
