package com.mall.exam.attempt.model;

import java.time.LocalDateTime;

public class ExamWrongRecordEntity {
    private Long id;
    private Long userId;
    private Long paperId;
    private Long paperItemId;
    private Long originalQuestionId;
    private String wrongAnswer;
    private String knowledgePoints;
    private LocalDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPaperId() { return paperId; }
    public void setPaperId(Long paperId) { this.paperId = paperId; }
    public Long getPaperItemId() { return paperItemId; }
    public void setPaperItemId(Long paperItemId) { this.paperItemId = paperItemId; }
    public Long getOriginalQuestionId() { return originalQuestionId; }
    public void setOriginalQuestionId(Long originalQuestionId) { this.originalQuestionId = originalQuestionId; }
    public String getWrongAnswer() { return wrongAnswer; }
    public void setWrongAnswer(String wrongAnswer) { this.wrongAnswer = wrongAnswer; }
    public String getKnowledgePoints() { return knowledgePoints; }
    public void setKnowledgePoints(String knowledgePoints) { this.knowledgePoints = knowledgePoints; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
