package com.mall.exam.controller;

import com.mall.common.api.ApiResponse;
import com.mall.common.api.PageResponse;
import com.mall.exam.attempt.dto.ExamResultResponse;
import com.mall.exam.attempt.dto.ExamReviewResponse;
import com.mall.exam.attempt.dto.SaveAnswerRequest;
import com.mall.exam.attempt.dto.SavedAnswerResponse;
import com.mall.exam.attempt.dto.SelfScoreRequest;
import com.mall.exam.attempt.dto.WrongSummaryResponse;
import com.mall.exam.attempt.service.ExamAttemptService;
import com.mall.exam.dto.SubmitExamRequest;
import com.mall.exam.paper.dto.ExamPaperResponse;
import com.mall.exam.paper.dto.GenerateFixedPaperRequest;
import com.mall.exam.paper.service.ExamPaperService;
import com.mall.exam.paper.template.dto.FixedPaperSummary;
import com.mall.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/exams")
public class ExamController {
    private final ExamPaperService papers;
    private final ExamAttemptService attempts;

    public ExamController(ExamPaperService papers, ExamAttemptService attempts) {
        this.papers = papers;
        this.attempts = attempts;
    }

    @PostMapping("/generate")
    public ApiResponse<ExamPaperResponse> generate(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(papers.generateOrResume(user.getUserId()));
    }

    @GetMapping("/fixed-papers")
    public ApiResponse<List<FixedPaperSummary>> fixedPapers() {
        return ApiResponse.success(papers.listFixedPapers());
    }

    @PostMapping("/fixed-papers/generate")
    public ApiResponse<ExamPaperResponse> generateFixed(@AuthenticationPrincipal AuthenticatedUser user,
                                                       @Valid @RequestBody GenerateFixedPaperRequest request) {
        return ApiResponse.success(papers.generateFixedPaper(user.getUserId(), request.paperCode));
    }

    @GetMapping("/current")
    public ApiResponse<ExamPaperResponse> current(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(attempts.current(user.getUserId()));
    }

    @PostMapping("/answers/save")
    public ApiResponse<SavedAnswerResponse> save(@AuthenticationPrincipal AuthenticatedUser user,
                                                @Valid @RequestBody SaveAnswerRequest request) {
        return ApiResponse.success(attempts.save(user.getUserId(), request));
    }

    @PostMapping("/submit")
    public ApiResponse<ExamResultResponse> submit(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @Valid @RequestBody SubmitExamRequest request) {
        return ApiResponse.success(attempts.submit(user.getUserId(), request.paperId, false));
    }

    @GetMapping("/records")
    public ApiResponse<PageResponse<ExamResultResponse>> records(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(attempts.records(user.getUserId(), page, size));
    }

    @GetMapping("/review")
    public ApiResponse<ExamReviewResponse> review(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @RequestParam @Positive long paperId) {
        return ApiResponse.success(attempts.review(user.getUserId(), paperId));
    }

    @PostMapping("/self-score")
    public ApiResponse<ExamResultResponse> selfScore(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @Valid @RequestBody SelfScoreRequest request) {
        return ApiResponse.success(attempts.selfScore(user.getUserId(), request));
    }

    @GetMapping("/wrong-summary")
    public ApiResponse<WrongSummaryResponse> wrongSummary(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(attempts.wrongSummary(user.getUserId()));
    }
}
