package com.mall.exam.admin.dto;

import com.mall.exam.question.model.QuestionEntity;
import com.mall.exam.question.model.QuestionGroupEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminExamResponses {
    private AdminExamResponses() {}
    public static class QuestionSummary {
        public Long id, groupId, sourceId;
        public String questionType, stem, sourceName, groupTitle, knowledgePoints, reviewStatus;
        public Integer difficulty, enabled;
    }
    public static class QuestionDetail {
        public QuestionGroupEntity group;
        public List<QuestionEntity> questions;
        public QuestionDetail(QuestionGroupEntity group, List<QuestionEntity> questions) { this.group=group; this.questions=questions; }
    }
    public static class Record {
        public Long id, userId;
        public String username, paperNo, status;
        public LocalDateTime startedAt, dueAt, submittedAt;
        public BigDecimal objectiveScore, selfScoreTotal;
    }
    public static class Blueprint {
        public Long id;
        public String blueprintCode, blueprintName, version, status;
        public Integer durationMinutes;
        public BigDecimal totalScore;
        public List<Capacity> sections;
        public long completePaperCapacity;
    }
    public static class Capacity {
        public String questionType;
        public int sectionOrder, groupCount, answerItemCount;
        public long availableGroups, availableItems;
    }
    public static class DialogueGroupCount {
        public int memberCount;
        public long groupCount;
    }
    /** Inventory metadata only: never include template question or answer snapshots. */
    public static class FixedPaper {
        public Long id, blueprintId;
        public String paperCode, title, blueprintCode, blueprintName, blueprintVersionSnapshot;
        public String sourceDeclaration, status;
        public Integer version, displayOrder, durationMinutes;
        public BigDecimal totalScore;
        public LocalDateTime publishedAt;
        public long groupCount, itemCount, dialogueCount, readingCount, vocabularyCount, grammarCount, translationCount, writingCount;
        public boolean selectable;
    }
}
