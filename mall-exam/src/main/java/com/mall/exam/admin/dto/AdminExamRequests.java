package com.mall.exam.admin.dto;

import com.fasterxml.jackson.databind.JsonNode;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

/** Only editable input; audit identity and review state are server-owned. */
public final class AdminExamRequests {
    private AdminExamRequests() {}
    public static class Source {
        @NotBlank @Size(max=128) public String sourceName;
        @NotBlank @Size(max=64) public String sourceType;
        @Size(max=1024) public String originalUrl;
        @NotNull @Pattern(regexp="ORIGINAL|AUTHORIZED|PUBLIC_OFFICIAL|UNVERIFIED|PROHIBITED") public String copyrightStatus;
        @Size(max=10000) public String copyrightNote;
    }
    public static class SourceUpdate extends Source { @NotNull @Min(1) public Long sourceId; }
    public static class SourceReview {
        @NotNull @Min(1) public Long sourceId;
        @NotNull public Boolean approved;
        @Pattern(regexp="ORIGINAL|AUTHORIZED|PUBLIC_OFFICIAL|UNVERIFIED|PROHIBITED") public String copyrightStatus;
        @NotBlank @Size(max=10000) public String reviewNote;
    }
    public static class SourceStatus { @NotNull @Min(1) public Long sourceId; @NotNull @Min(0) @Max(1) public Integer enabled; }
    public static class QuestionReview {
        @NotNull @Min(1) public Long questionId;
        @NotNull public Boolean approved;
        @NotBlank @Size(max=10000) public String reviewNote;
    }
    public static class QuestionStatus { @NotNull @Min(1) public Long questionId; @NotNull @Min(0) @Max(1) public Integer enabled; }
    public static class BlueprintStatus {
        @NotNull @Min(1) public Long blueprintId;
        @NotNull @Pattern(regexp="ENABLED|DISABLED") public String status;
    }
    public static class Question {
        @NotNull @Min(1) public Long sourceId;
        @NotNull @Valid public Content content;
    }
    public static class QuestionUpdate extends Question { @NotNull @Min(1) public Long questionId; }
    /** Same single-question / whole-group content shape as JSON import. */
    public static class Content {
        @Pattern(regexp="DIALOGUE_BLANK|READING|VOCABULARY|GRAMMAR|TRANSLATION|WRITING") public String questionType;
        @Size(max=10000) public String stem;
        public JsonNode options, correctAnswer, scoringRubric, knowledgePoints;
        @Size(max=10000) public String explanation;
        @Size(max=20000) public String referenceAnswer, sampleAnswer;
        @Min(1) @Max(5) public Integer difficulty;
        @Pattern(regexp="DIALOGUE|READING") public String groupType;
        @Size(max=255) public String title;
        @Size(max=10000) public String instruction;
        @Size(max=50000) public String content;
        public JsonNode sharedOptions;
        @Size(min=1,max=2000) public List<@Valid Content> questions;
    }
}
