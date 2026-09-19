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

import com.mall.exam.admin.dto.AdminExamResponses.Record;
@RestController @Validated @RequestMapping("/admin/api/exam/records")
public class AdminExamRecordController {
    private final AdminExamQueryService query;
    public AdminExamRecordController(AdminExamQueryService query) { this.query=query; }
    @GetMapping public ApiResponse<PageResponse<Record>> list(
        @RequestParam(required=false) @Min(1) Long userId,@RequestParam(required=false) String paperNo,
        @RequestParam(required=false) @Pattern(regexp="IN_PROGRESS|SUBMITTED|AUTO_SUBMITTED") String status,
        @RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(query.records(userId,paperNo,status,page,size));
    }
}
