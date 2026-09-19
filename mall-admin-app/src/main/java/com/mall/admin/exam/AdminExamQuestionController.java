package com.mall.admin.exam;
import com.mall.admin.security.AdminPrincipal;
import com.mall.common.api.*;
import com.mall.exam.admin.dto.AdminExamRequests.*;
import com.mall.exam.admin.dto.AdminExamResponses.*;
import com.mall.exam.admin.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.mall.exam.question.service.*;
@RestController @Validated @RequestMapping("/admin/api/exam/questions")
public class AdminExamQuestionController {
    private final AdminExamQueryService query; private final AdminExamManagementService management; private final QuestionGovernanceService governance;
    public AdminExamQuestionController(AdminExamQueryService query,AdminExamManagementService management,QuestionGovernanceService governance) { this.query=query;this.management=management;this.governance=governance; }
    @GetMapping public ApiResponse<PageResponse<QuestionSummary>> list(
        @RequestParam(required=false) String questionType,@RequestParam(required=false) String reviewStatus,
        @RequestParam(required=false) @Min(0) @Max(1) Integer enabled,@RequestParam(required=false) @Min(1) Long sourceId,
        @RequestParam(required=false) String knowledgePoint,@RequestParam(required=false) String keyword,
        @RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(query.questions(questionType,reviewStatus,enabled,sourceId,knowledgePoint,keyword,page,size));
    }
    @GetMapping("/detail") public ApiResponse<QuestionDetail> detail(@RequestParam @Min(1) long questionId) { return ApiResponse.success(query.detail(questionId)); }
    @PostMapping("/create") public ApiResponse<Long> create(@Valid @RequestBody Question body,@AuthenticationPrincipal AdminPrincipal admin) { return ApiResponse.success(management.createQuestion(body,admin.getAdminId())); }
    @PostMapping("/update") public ApiResponse<Void> update(@Valid @RequestBody QuestionUpdate body,@AuthenticationPrincipal AdminPrincipal admin) { management.updateQuestion(body,admin.getAdminId());return ApiResponse.success(); }
    @PostMapping("/review") public ApiResponse<Void> review(@Valid @RequestBody QuestionReview body,@AuthenticationPrincipal AdminPrincipal admin) {
        governance.reviewQuestion(body.questionId,admin.getAdminId(),new QuestionReviewCommand(body.approved,body.reviewNote));return ApiResponse.success();
    }
    @PostMapping("/status") public ApiResponse<Void> status(@Valid @RequestBody QuestionStatus body,@AuthenticationPrincipal AdminPrincipal admin) { management.questionStatus(body.questionId,body.enabled,admin.getAdminId());return ApiResponse.success(); }
}
