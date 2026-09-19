package com.mall.exam.admin.service;

import com.mall.common.api.*;
import com.mall.common.exception.BusinessException;
import com.mall.exam.admin.dto.AdminExamResponses.*;
import com.mall.exam.admin.dto.AdminExamResponses.Record;
import com.mall.exam.source.model.QuestionSourceEntity;
import com.mall.exam.question.mapper.*;
import com.mall.exam.question.model.QuestionEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional(readOnly=true)
public class AdminExamQueryService {
    private final AdminExamMapper mapper;
    private final QuestionMapper questions;
    public AdminExamQueryService(AdminExamMapper mapper, QuestionMapper questions) { this.mapper=mapper; this.questions=questions; }
    public PageResponse<QuestionSourceEntity> sources(String keyword,String sourceType,String reviewStatus,String copyrightStatus,Integer enabled,int page,int size) {
        Map<String,Object> p=page(page,size); p.put("keyword",keyword); p.put("sourceType",sourceType); p.put("reviewStatus",reviewStatus); p.put("copyrightStatus",copyrightStatus); p.put("enabled",enabled);
        return new PageResponse<>(mapper.sources(p),page,size,mapper.countSources(p));
    }
    public PageResponse<QuestionSummary> questions(String questionType,String reviewStatus,Integer enabled,Long sourceId,String knowledgePoint,String keyword,int page,int size) {
        Map<String,Object> p=page(page,size); p.put("questionType",questionType); p.put("reviewStatus",reviewStatus); p.put("enabled",enabled); p.put("sourceId",sourceId); p.put("knowledgePoint",knowledgePoint); p.put("keyword",keyword);
        return new PageResponse<>(mapper.questions(p),page,size,mapper.countQuestions(p));
    }
    public QuestionDetail detail(long questionId) {
        QuestionEntity q=questions.findById(questionId);
        if(q==null) throw new BusinessException(HttpStatus.NOT_FOUND,ErrorCode.EXAM_NOT_FOUND);
        return q.getGroupId()==null ? new QuestionDetail(null,Collections.singletonList(q))
            : new QuestionDetail(questions.findGroupById(q.getGroupId()),mapper.groupQuestions(q.getGroupId()));
    }
    public PageResponse<Record> records(Long userId,String paperNo,String status,int page,int size) {
        Map<String,Object> p=page(page,size); p.put("userId",userId); p.put("paperNo",paperNo); p.put("status",status);
        return new PageResponse<>(mapper.records(p),page,size,mapper.countRecords(p));
    }
    public List<Blueprint> blueprints() {
        List<Blueprint> result=mapper.blueprints();
        for(Blueprint b:result) {
            b.sections=mapper.capacities(b.id); long capacity=Long.MAX_VALUE;
            for(Capacity s:b.sections) {
                long available;
                if("DIALOGUE".equals(s.questionType)) {
                    Map<Integer,Long> counts=new HashMap<>();
                    for(DialogueGroupCount count:mapper.dialogueGroupCounts()) counts.put(count.memberCount,count.groupCount);
                    available=com.mall.exam.blueprint.DialogueCapacityCalculator.maximumPapers(counts);
                }
                else if(s.groupCount>0) available=s.availableGroups/s.groupCount;
                else available=s.answerItemCount>0?s.availableItems/s.answerItemCount:0;
                capacity=Math.min(capacity,available);
            }
            b.completePaperCapacity=capacity==Long.MAX_VALUE?0:capacity;
        }
        return result;
    }
    private static Map<String,Object> page(int page,int size) {
        if(page<1 || size<1 || size>100) throw new BusinessException(HttpStatus.BAD_REQUEST,ErrorCode.VALIDATION_ERROR);
        Map<String,Object> p=new HashMap<>(); p.put("size",size); p.put("offset",((long)page-1)*size); return p;
    }
}
