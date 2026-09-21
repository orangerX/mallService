package com.mall.exam.paper.template.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** Read-only frozen template data; never expose this model as a student response. */
public final class FixedPaperTemplateItem {
    private final Long id;
    private final Long fixedPaperId;
    private final Long fixedPaperGroupId;
    private final Long originalQuestionId;
    private final Long sourceId;
    private final String contentFingerprint;
    private final Integer sectionOrder;
    private final Integer itemOrder;
    private final String questionType;
    private final String stem;
    private final String options;
    private final String correctAnswer;
    private final String explanation;
    private final String referenceAnswer;
    private final String sampleAnswer;
    private final String scoringRubric;
    private final String knowledgePoints;
    private final BigDecimal score;
    private final LocalDateTime createdAt;

    public FixedPaperTemplateItem(
            Long id,
            Long fixedPaperId,
            Long fixedPaperGroupId,
            Long originalQuestionId,
            Long sourceId,
            String contentFingerprint,
            Integer sectionOrder,
            Integer itemOrder,
            String questionType,
            String stem,
            String options,
            String correctAnswer,
            String explanation,
            String referenceAnswer,
            String sampleAnswer,
            String scoringRubric,
            String knowledgePoints,
            BigDecimal score,
            LocalDateTime createdAt) {
        this.id = id;
        this.fixedPaperId = fixedPaperId;
        this.fixedPaperGroupId = fixedPaperGroupId;
        this.originalQuestionId = originalQuestionId;
        this.sourceId = sourceId;
        this.contentFingerprint = contentFingerprint;
        this.sectionOrder = sectionOrder;
        this.itemOrder = itemOrder;
        this.questionType = questionType;
        this.stem = stem;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.referenceAnswer = referenceAnswer;
        this.sampleAnswer = sampleAnswer;
        this.scoringRubric = scoringRubric;
        this.knowledgePoints = knowledgePoints;
        this.score = score;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }

    public Long getFixedPaperId() { return fixedPaperId; }

    public Long getFixedPaperGroupId() { return fixedPaperGroupId; }

    public Long getOriginalQuestionId() { return originalQuestionId; }

    public Long getSourceId() { return sourceId; }

    public String getContentFingerprint() { return contentFingerprint; }

    public Integer getSectionOrder() { return sectionOrder; }

    public Integer getItemOrder() { return itemOrder; }

    public String getQuestionType() { return questionType; }

    public String getStem() { return stem; }

    public String getOptions() { return options; }

    @JsonIgnore
    public String getCorrectAnswer() { return correctAnswer; }

    @JsonIgnore
    public String getExplanation() { return explanation; }

    @JsonIgnore
    public String getReferenceAnswer() { return referenceAnswer; }

    @JsonIgnore
    public String getSampleAnswer() { return sampleAnswer; }

    @JsonIgnore
    public String getScoringRubric() { return scoringRubric; }

    public String getKnowledgePoints() { return knowledgePoints; }

    public BigDecimal getScore() { return score; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
