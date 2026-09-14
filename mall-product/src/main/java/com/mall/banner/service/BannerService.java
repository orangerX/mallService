package com.mall.banner.service;

import com.mall.banner.dto.BannerResponse;
import com.mall.banner.mapper.BannerMapper;
import com.mall.banner.model.BannerEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BannerService {

    private final BannerMapper bannerMapper;

    public BannerService(BannerMapper bannerMapper) {
        this.bannerMapper = bannerMapper;
    }

    public List<BannerResponse> listHomeBanners(String baseUrl) {
        return bannerMapper.findEnabledHomeBanners().stream()
                .map(banner -> toResponse(banner, baseUrl))
                .collect(Collectors.toList());
    }

    private static BannerResponse toResponse(BannerEntity banner, String baseUrl) {
        return new BannerResponse(
                banner.getId(),
                banner.getTitle(),
                baseUrl + banner.getImagePath(),
                banner.getSortOrder()
        );
    }
}
