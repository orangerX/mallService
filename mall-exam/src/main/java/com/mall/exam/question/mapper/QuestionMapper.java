package com.mall.exam.question.mapper;

import com.mall.exam.question.model.QuestionEntity;
import com.mall.exam.question.model.QuestionGroupEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface QuestionMapper {

    QuestionEntity findById(@Param("id") Long id);

    QuestionGroupEntity findGroupById(@Param("id") Long id);

    QuestionGroupEntity findGroupByIdForUpdate(@Param("id") Long id);

    java.util.List<QuestionEntity> findByGroupIdForUpdate(@Param("groupId") Long groupId);

    int insertGroup(QuestionGroupEntity group);

    int insert(QuestionEntity question);

    long countQuestionsByContentFingerprint(@Param("contentFingerprint") String contentFingerprint);

    long countGroupsByContentFingerprint(@Param("contentFingerprint") String contentFingerprint);

    int approveGroupIfReusable(@Param("groupId") Long groupId);

    int approveQuestionsByGroupIfReusable(@Param("groupId") Long groupId);

    int approveQuestionIfSourceReusable(@Param("id") Long id);

    int rejectGroup(@Param("groupId") Long groupId);

    int rejectQuestionsByGroupId(@Param("groupId") Long groupId);

    int rejectQuestion(@Param("id") Long id);

    int updateGroupStatus(@Param("groupId") Long groupId, @Param("enabled") Integer enabled);

    int updateStatusByGroupId(@Param("groupId") Long groupId, @Param("enabled") Integer enabled);

    int updateStatus(@Param("id") Long id, @Param("enabled") Integer enabled);

    long countPaperItemSnapshots(@Param("id") Long id);

    int deleteUnreferencedById(@Param("id") Long id);
}
