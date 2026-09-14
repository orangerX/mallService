package com.mall.banner.controller;

import com.mall.banner.dto.BannerResponse;
import com.mall.banner.service.BannerService;
import com.mall.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping("/home")
    public ApiResponse<List<BannerResponse>> home() {
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .build()
                .toUriString();
        return ApiResponse.success(bannerService.listHomeBanners(baseUrl));
    }
}
