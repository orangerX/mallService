package com.mall.banner.dto;

public class BannerResponse {

    private final Long id;
    private final String title;
    private final String imageUrl;
    private final Integer sortOrder;

    public BannerResponse(Long id, String title, String imageUrl, Integer sortOrder) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
}
