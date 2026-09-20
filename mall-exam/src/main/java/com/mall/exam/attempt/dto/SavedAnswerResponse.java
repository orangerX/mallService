package com.mall.exam.attempt.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.mall.exam.attempt.model.ExamAnswerEntity;
import java.time.LocalDateTime;

/** Contains only the student's own answer, never the grading fields. */
public final class SavedAnswerResponse {
    public final Long paperId, paperItemId;
    public final Integer answerVersion;
    public final JsonNode answerContent;
    public final LocalDateTime savedAt, dueAt, serverTime;

    public SavedAnswerResponse(long paperId, long paperItemId, ExamAnswerEntity answer,
                               JsonNode content, LocalDateTime dueAt, LocalDateTime serverTime) {
        this.paperId = paperId;
        this.paperItemId = paperItemId;
        this.answerVersion = answer == null ? 0 : answer.getAnswerVersion();
        this.answerContent = content;
        this.savedAt = answer == null ? null : answer.getSavedAt();
        this.dueAt = dueAt;
        this.serverTime = serverTime;
    }
}
