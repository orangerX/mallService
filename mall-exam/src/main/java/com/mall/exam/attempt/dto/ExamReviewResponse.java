package com.mall.exam.attempt.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.mall.exam.paper.dto.ExamPaperResponse;
import com.mall.exam.paper.model.ExamPaperItemSnapshotEntity;
import java.math.BigDecimal;
import java.util.*;

/** Constructed only after submission; correct objective items are deliberately absent. */
public final class ExamReviewResponse {
    public final ExamResultResponse result;
    public final List<ExamPaperResponse.Group> groups;
    public final List<WrongItem> wrongItems;
    public final List<SubjectiveItem> subjectiveItems;

    public ExamReviewResponse(ExamResultResponse result, List<ExamPaperResponse.Group> groups,
                              List<WrongItem> wrongItems, List<SubjectiveItem> subjectiveItems) {
        this.result = result;
        this.groups = List.copyOf(groups);
        this.wrongItems = List.copyOf(wrongItems);
        this.subjectiveItems = List.copyOf(subjectiveItems);
    }

    public static final class WrongItem {
        public final Long paperItemId, paperGroupSnapshotId;
        public final Integer sectionOrder, itemOrder;
        public final String questionType, stem, options, explanation, knowledgePoints;
        public final JsonNode answerContent, correctAnswer;
        public final BigDecimal score;
        public final boolean unanswered;

        public WrongItem(ExamPaperItemSnapshotEntity item, JsonNode answer, JsonNode correctAnswer, boolean unanswered) {
            paperItemId = item.getId(); paperGroupSnapshotId = item.getPaperGroupSnapshotId();
            sectionOrder = item.getSectionOrder(); itemOrder = item.getItemOrder();
            questionType = item.getQuestionType(); stem = item.getStem(); options = item.getOptions();
            explanation = item.getExplanation(); knowledgePoints = item.getKnowledgePoints(); score = item.getScore();
            answerContent = answer; this.correctAnswer = correctAnswer; this.unanswered = unanswered;
        }
    }

    public static final class SubjectiveItem {
        public final Long paperItemId;
        public final Integer sectionOrder, itemOrder;
        public final String questionType, stem, referenceAnswer, sampleAnswer, scoringRubric, explanation;
        public final JsonNode answerContent;
        public final BigDecimal score;

        public SubjectiveItem(ExamPaperItemSnapshotEntity item, JsonNode answer) {
            paperItemId = item.getId(); sectionOrder = item.getSectionOrder(); itemOrder = item.getItemOrder();
            questionType = item.getQuestionType(); stem = item.getStem(); score = item.getScore();
            referenceAnswer = item.getReferenceAnswer(); sampleAnswer = item.getSampleAnswer();
            scoringRubric = item.getScoringRubric(); explanation = item.getExplanation(); answerContent = answer;
        }
    }
}
