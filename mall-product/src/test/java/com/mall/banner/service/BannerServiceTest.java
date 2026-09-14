package com.mall.banner.service;

import com.mall.banner.dto.BannerResponse;
import com.mall.banner.mapper.BannerMapper;
import com.mall.banner.model.BannerEntity;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BannerServiceTest {

    @Test
    void returnsOrderedBannersWithAbsoluteImageUrls() {
        BannerMapper mapper = mock(BannerMapper.class);
        when(mapper.findEnabledHomeBanners()).thenReturn(Arrays.asList(
                banner(1L, "秋日石榴", "/images/banners/autumn-pomegranate.jpg", 1),
                banner(2L, "紫玉葡萄", "/images/banners/autumn-grapes.jpg", 2),
                banner(3L, "黄金秋梨", "/images/banners/autumn-asian-pear.jpg", 3)
        ));
        BannerService service = new BannerService(mapper);

        List<BannerResponse> response = service.listHomeBanners("https://mall.example.com");

        assertEquals(3, response.size());
        assertEquals("https://mall.example.com/images/banners/autumn-pomegranate.jpg",
                response.get(0).getImageUrl());
        assertEquals(3, response.get(2).getSortOrder());
    }

    private static BannerEntity banner(Long id, String title, String imagePath, int sortOrder) {
        BannerEntity banner = new BannerEntity();
        banner.setId(id);
        banner.setTitle(title);
        banner.setImagePath(imagePath);
        banner.setSortOrder(sortOrder);
        banner.setStatus(BannerEntity.STATUS_ENABLED);
        return banner;
    }
}
