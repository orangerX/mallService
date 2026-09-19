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

import java.util.List;
@RestController @Validated @RequestMapping("/admin/api/exam/blueprints")
public class AdminExamBlueprintController {
    private final AdminExamQueryService query; private final AdminExamManagementService management;
    public AdminExamBlueprintController(AdminExamQueryService query,AdminExamManagementService management) { this.query=query;this.management=management; }
    @GetMapping public ApiResponse<List<Blueprint>> list() { return ApiResponse.success(query.blueprints()); }
    @PostMapping("/status") public ApiResponse<Void> status(@Valid @RequestBody BlueprintStatus body,@AuthenticationPrincipal AdminPrincipal admin) { management.blueprintStatus(body.blueprintId,body.status,admin.getAdminId());return ApiResponse.success(); }
}
