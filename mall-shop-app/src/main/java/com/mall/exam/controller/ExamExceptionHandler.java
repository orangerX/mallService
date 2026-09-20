package com.mall.exam.controller;

import com.mall.common.api.ApiResponse;
import com.mall.exam.attempt.dto.SavedAnswerResponse;
import com.mall.exam.attempt.service.ExamAnswerConflictException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Shop-only adapter; all other exceptions retain the common handler's behavior. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ExamController.class)
public class ExamExceptionHandler {
    @ExceptionHandler(ExamAnswerConflictException.class)
    public ResponseEntity<ApiResponse<ConflictData>> answerConflict(ExamAnswerConflictException exception) {
        ApiResponse<ConflictData> response = new ApiResponse<>();
        response.setCode(exception.getErrorCode().getCode());
        response.setMessage(exception.getMessage());
        response.setData(new ConflictData(exception.getLatestAnswer()));
        return ResponseEntity.status(exception.getStatus()).body(response);
    }

    public static final class ConflictData {
        public final SavedAnswerResponse latestAnswer;

        public ConflictData(SavedAnswerResponse latestAnswer) {
            this.latestAnswer = latestAnswer;
        }
    }
}
