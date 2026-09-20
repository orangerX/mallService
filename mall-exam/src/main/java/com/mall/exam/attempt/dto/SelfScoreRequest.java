package com.mall.exam.attempt.dto;

import com.fasterxml.jackson.databind.JsonNode;
import javax.validation.constraints.*;

public class SelfScoreRequest {
    @NotNull @Min(1) public Long paperId;
    // Preserve JSON number types: Jackson otherwise coerces fractional values to Integer.
    @NotNull public JsonNode translationScore;
    @NotNull public JsonNode writingScore;
}
