package com.mall.exam.attempt.mapper;

import com.mall.exam.attempt.model.*;
import com.mall.exam.paper.model.*;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ExamAttemptMapper {
    ExamPaperEntity findPaperForUpdate(@Param("userId") long userId, @Param("paperId") long paperId);
    ExamPaperItemSnapshotEntity findPaperItem(@Param("paperId") long paperId, @Param("paperItemId") long paperItemId);
    ExamAnswerEntity findAnswerForUpdate(@Param("paperId") long paperId, @Param("paperItemId") long paperItemId);
    List<ExamAnswerEntity> findAnswers(@Param("paperId") long paperId);
    List<ExamWrongRecordEntity> findWrongRecords(@Param("userId") long userId, @Param("paperId") long paperId);
    int insertAnswer(ExamAnswerEntity answer);
    int updateAnswer(@Param("answer") ExamAnswerEntity answer, @Param("expectedVersion") int expectedVersion);
    int updateAnswerGrade(ExamAnswerEntity answer);
    int insertWrongRecord(ExamWrongRecordEntity record);
    int completePaper(ExamPaperEntity paper);
    int updateSelfScore(ExamPaperEntity paper);
}
