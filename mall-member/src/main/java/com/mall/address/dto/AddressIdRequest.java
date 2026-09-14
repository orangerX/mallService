package com.mall.address.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class AddressIdRequest {

    @NotNull(message = "地址 ID 不能为空")
    @Positive(message = "地址 ID 必须大于 0")
    private Long addressId;

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }
}
