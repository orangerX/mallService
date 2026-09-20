package com.mall.exam.attempt.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.exam.attempt.dto.SavedAnswerResponse;
import org.springframework.http.HttpStatus;

/** The HTTP adapter must put latestAnswer in the conflict response data. */
public final class ExamAnswerConflictException extends BusinessException {
    private final SavedAnswerResponse latestAnswer;

    public ExamAnswerConflictException(SavedAnswerResponse latestAnswer) {
        super(HttpStatus.CONFLICT, ErrorCode.EXAM_ANSWER_CONFLICT);
        this.latestAnswer = latestAnswer;
    }

    public SavedAnswerResponse getLatestAnswer() { return latestAnswer; }
}
