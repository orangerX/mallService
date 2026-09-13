package com.mall.address.mapper;

import com.mall.address.model.UserAddressEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserAddressMapper {

    List<UserAddressEntity> findByUserId(@Param("userId") Long userId);

    UserAddressEntity findDefaultByUserId(@Param("userId") Long userId);

    UserAddressEntity findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int insert(UserAddressEntity address);

    int update(UserAddressEntity address);

    int clearDefaultByUserId(@Param("userId") Long userId);

    int setDefault(@Param("id") Long id, @Param("userId") Long userId);

    int delete(@Param("id") Long id, @Param("userId") Long userId);
}
