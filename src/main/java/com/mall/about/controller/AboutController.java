package com.mall.about.controller;

import com.mall.about.dto.AboutResponse;
import com.mall.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/about")
public class AboutController {

    @GetMapping
    public ApiResponse<AboutResponse> about() {
        return ApiResponse.success(new AboutResponse(
                "水果商城",
                "商城主要售卖新鲜、优质的时令水果。",
                "1.0.0"
        ));
    }
}
