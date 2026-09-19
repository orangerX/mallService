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

    int insertGroup(QuestionGroupEntity group);

    int insert(QuestionEntity question);

    int updateGroupReview(@Param("groupId") Long groupId, @Param("reviewStatus") String reviewStatus);

    int updateReviewByGroupId(@Param("groupId") Long groupId,
                              @Param("reviewStatus") String reviewStatus,
                              @Param("reviewedBy") Long reviewedBy,
                              @Param("reviewedAt") LocalDateTime reviewedAt,
                              @Param("reviewComment") String reviewComment);

    int updateReview(@Param("id") Long id,
                     @Param("reviewStatus") String reviewStatus,
                     @Param("reviewedBy") Long reviewedBy,
                     @Param("reviewedAt") LocalDateTime reviewedAt,
                     @Param("reviewComment") String reviewComment);

    int updateGroupStatus(@Param("groupId") Long groupId, @Param("enabled") Integer enabled);

    int updateStatusByGroupId(@Param("groupId") Long groupId, @Param("enabled") Integer enabled);

    int updateStatus(@Param("id") Long id, @Param("enabled") Integer enabled);

    long countPaperItemSnapshots(@Param("id") Long id);

    int deleteUnreferencedById(@Param("id") Long id);
}
