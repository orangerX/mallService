package com.mall.banner.mapper;

import com.mall.banner.model.BannerEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BannerMapper {

    List<BannerEntity> findEnabledHomeBanners();
}
