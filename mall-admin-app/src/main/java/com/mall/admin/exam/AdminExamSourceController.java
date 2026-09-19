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

import com.mall.exam.source.model.QuestionSourceEntity;
import com.mall.exam.question.service.*;
@RestController @Validated @RequestMapping("/admin/api/exam/sources")
public class AdminExamSourceController {
    private final AdminExamQueryService query; private final AdminExamManagementService management; private final QuestionGovernanceService governance;
    public AdminExamSourceController(AdminExamQueryService query,AdminExamManagementService management,QuestionGovernanceService governance) { this.query=query;this.management=management;this.governance=governance; }
    @GetMapping public ApiResponse<PageResponse<QuestionSourceEntity>> list(
        @RequestParam(required=false) String keyword,@RequestParam(required=false) String sourceType,
        @RequestParam(required=false) String reviewStatus,@RequestParam(required=false) String copyrightStatus,
        @RequestParam(required=false) @Min(0) @Max(1) Integer enabled,
        @RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(query.sources(keyword,sourceType,reviewStatus,copyrightStatus,enabled,page,size));
    }
    @PostMapping("/create") public ApiResponse<Long> create(@Valid @RequestBody Source body,@AuthenticationPrincipal AdminPrincipal admin) { return ApiResponse.success(management.createSource(body,admin.getAdminId())); }
    @PostMapping("/update") public ApiResponse<Void> update(@Valid @RequestBody SourceUpdate body,@AuthenticationPrincipal AdminPrincipal admin) { management.updateSource(body,admin.getAdminId());return ApiResponse.success(); }
    @PostMapping("/review") public ApiResponse<Void> review(@Valid @RequestBody SourceReview body,@AuthenticationPrincipal AdminPrincipal admin) {
        governance.reviewSource(body.sourceId,admin.getAdminId(),new SourceReviewCommand(body.approved,body.copyrightStatus,body.reviewNote));return ApiResponse.success();
    }
    @PostMapping("/status") public ApiResponse<Void> status(@Valid @RequestBody SourceStatus body,@AuthenticationPrincipal AdminPrincipal admin) { management.sourceStatus(body.sourceId,body.enabled,admin.getAdminId());return ApiResponse.success(); }
}
