package com.mall.exam.attempt.dto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Aggregate counts only; contains neither student answers nor marking references. */
public final class WrongSummaryResponse {
    public final long totalWrong;
    public final Map<String, Long> byQuestionType;
    public final Map<String, Long> byKnowledgePoint;
    public final LocalDateTime serverTime;

    public WrongSummaryResponse(long totalWrong, Map<String, Long> byQuestionType,
                                Map<String, Long> byKnowledgePoint, LocalDateTime serverTime) {
        this.totalWrong = totalWrong;
        this.byQuestionType = Collections.unmodifiableMap(new TreeMap<>(byQuestionType));
        this.byKnowledgePoint = Collections.unmodifiableMap(new TreeMap<>(byKnowledgePoint));
        this.serverTime = serverTime;
    }
}
