package com.mall.exam.attempt.dto;

import com.fasterxml.jackson.databind.JsonNode;
import javax.validation.constraints.*;

public class SaveAnswerRequest {
    @NotNull @Min(1) public Long paperId;
    @NotNull @Min(1) public Long paperItemId;
    /** Zero means unsaved; every successful save increments it once. */
    @NotNull @Min(0) public Integer answerVersion;
    /** Null clears an answer; objective answers accept a scalar or single-element array. */
    public JsonNode answerContent;
}
