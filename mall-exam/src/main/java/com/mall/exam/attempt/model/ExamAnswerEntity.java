package com.mall.exam.attempt.model;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public class ExamAnswerEntity {
    private Long id;
    private Long paperId;
    private Long paperItemId;
    private String answerContent;
    private Integer answerVersion;
    private LocalDateTime savedAt;
    private Boolean objectivelyCorrect;
    private BigDecimal earnedScore;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPaperId() { return paperId; }
    public void setPaperId(Long paperId) { this.paperId = paperId; }
    public Long getPaperItemId() { return paperItemId; }
    public void setPaperItemId(Long paperItemId) { this.paperItemId = paperItemId; }
    public String getAnswerContent() { return answerContent; }
    public void setAnswerContent(String answerContent) { this.answerContent = answerContent; }
    public Integer getAnswerVersion() { return answerVersion; }
    public void setAnswerVersion(Integer answerVersion) { this.answerVersion = answerVersion; }
    public LocalDateTime getSavedAt() { return savedAt; }
    public void setSavedAt(LocalDateTime savedAt) { this.savedAt = savedAt; }
    public Boolean getObjectivelyCorrect() { return objectivelyCorrect; }
    public void setObjectivelyCorrect(Boolean objectivelyCorrect) { this.objectivelyCorrect = objectivelyCorrect; }
    public BigDecimal getEarnedScore() { return earnedScore; }
    public void setEarnedScore(BigDecimal earnedScore) { this.earnedScore = earnedScore; }
}
