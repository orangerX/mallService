package com.mall.admin.exam;

import com.mall.common.api.ApiResponse;
import com.mall.exam.admin.dto.AdminExamResponses.FixedPaper;
import com.mall.exam.admin.service.AdminExamQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/api/exam/fixed-papers")
public class AdminExamFixedPaperController {
    private final AdminExamQueryService query;

    public AdminExamFixedPaperController(AdminExamQueryService query) {
        this.query = query;
    }

    @GetMapping
    public ApiResponse<List<FixedPaper>> list() {
        return ApiResponse.success(query.fixedPapers());
    }
}
