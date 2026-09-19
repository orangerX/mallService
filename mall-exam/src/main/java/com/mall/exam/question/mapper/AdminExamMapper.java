package com.mall.exam.question.mapper;

import com.mall.exam.admin.dto.AdminExamResponses.*;
import com.mall.exam.admin.dto.AdminExamResponses.Record;
import com.mall.exam.source.model.QuestionSourceEntity;
import com.mall.exam.question.model.QuestionEntity;
import com.mall.exam.question.model.QuestionGroupEntity;
import org.apache.ibatis.annotations.*;
import java.util.*;

@Mapper
public interface AdminExamMapper {
    List<QuestionSourceEntity> sources(Map<String,Object> filters);
    long countSources(Map<String,Object> filters);
    List<QuestionSummary> questions(Map<String,Object> filters);
    long countQuestions(Map<String,Object> filters);
    List<Record> records(Map<String,Object> filters);
    long countRecords(Map<String,Object> filters);
    List<QuestionEntity> groupQuestions(@Param("groupId") long groupId);
    List<Blueprint> blueprints();
    List<Capacity> capacities(@Param("blueprintId") long blueprintId);
    List<DialogueGroupCount> dialogueGroupCounts();
    int sourceStatus(@Param("id") long id, @Param("enabled") int enabled);
    int blueprintStatus(@Param("id") long id, @Param("status") String status);
    int updateDraftQuestion(QuestionEntity question);
    int updateDraftGroup(QuestionGroupEntity group);
    long countGroupSnapshots(@Param("groupId") long groupId);
}
