package com.mall.exam.attempt.dto;

import com.mall.exam.paper.model.ExamPaperEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ExamResultResponse {
    public final Long paperId;
    public final String paperNo, status;
    public final BigDecimal objectiveScore, selfScoreTotal;
    public final Integer translationScore, writingScore;
    public final LocalDateTime startedAt, dueAt, submittedAt, serverTime;

    public ExamResultResponse(ExamPaperEntity paper, LocalDateTime serverTime) {
        paperId = paper.getId();
        paperNo = paper.getPaperNo();
        status = paper.getStatus();
        objectiveScore = paper.getObjectiveScore();
        translationScore = paper.getTranslationSelfScore();
        writingScore = paper.getWritingSelfScore();
        selfScoreTotal = paper.getSelfScoreTotal();
        startedAt = paper.getStartedAt();
        dueAt = paper.getDueAt();
        submittedAt = paper.getSubmittedAt();
        this.serverTime = serverTime;
    }
}
