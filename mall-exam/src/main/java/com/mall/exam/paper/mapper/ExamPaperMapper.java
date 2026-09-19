package com.mall.exam.paper.mapper;
import com.mall.exam.paper.model.*;
import com.mall.exam.question.model.*;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface ExamPaperMapper {
    ExamPaperEntity findInProgress(@Param("userId") long userId);
    Long lockUser(@Param("userId") long userId);
    BlueprintRow findEnabledBlueprint(@Param("code") String code);
    List<AvailableGroup> findAvailableGroups();
    List<QuestionEntity> findAvailableQuestions();
    List<Long> findPreviouslyUsedQuestionIds(@Param("userId") long userId);
    List<Long> findPreviouslyUsedGroupIds(@Param("userId") long userId);
    int insertPaper(ExamPaperEntity paper);
    int insertGroupSnapshot(ExamPaperGroupSnapshotEntity group);
    int insertItemSnapshot(ExamPaperItemSnapshotEntity item);
    List<ExamPaperGroupSnapshotEntity> findGroupSnapshots(@Param("paperId") long paperId);
    List<ExamPaperItemSnapshotEntity> findItemSnapshots(@Param("paperId") long paperId);
    class BlueprintRow { public Long id;public String version; }
    class AvailableGroup extends QuestionGroupEntity {
        private Integer memberCount;
        public Integer getMemberCount(){return memberCount;}
        public void setMemberCount(Integer memberCount){this.memberCount=memberCount;}
    }
}
