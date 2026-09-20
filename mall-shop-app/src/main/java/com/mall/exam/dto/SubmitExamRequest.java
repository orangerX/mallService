package com.mall.exam.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/** Submission mode is derived from the server's deadline, never from the request. */
public class SubmitExamRequest {
    @NotNull
    @Positive
    public Long paperId;
}
