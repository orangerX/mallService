package com.mall.address.dto;

import com.mall.address.model.UserAddressEntity;

public class AddressResponse {

    private final Long id;
    private final String receiverName;
    private final String receiverPhone;
    private final String province;
    private final String city;
    private final String district;
    private final String detailAddress;
    private final String fullAddress;
    private final String addressLabel;
    private final boolean defaultAddress;

    public AddressResponse(Long id, String receiverName, String receiverPhone,
                           String province, String city, String district, String detailAddress,
                           String fullAddress, String addressLabel, boolean defaultAddress) {
        this.id = id;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.province = province;
        this.city = city;
        this.district = district;
        this.detailAddress = detailAddress;
        this.fullAddress = fullAddress;
        this.addressLabel = addressLabel;
        this.defaultAddress = defaultAddress;
    }

    public static AddressResponse from(UserAddressEntity address) {
        return new AddressResponse(
                address.getId(), address.getReceiverName(), address.getReceiverPhone(),
                address.getProvince(), address.getCity(), address.getDistrict(), address.getDetailAddress(),
                address.getProvince() + address.getCity() + address.getDistrict() + address.getDetailAddress(),
                address.getAddressLabel(), Boolean.TRUE.equals(address.getDefaultAddress())
        );
    }

    public Long getId() {
        return id;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public String getProvince() {
        return province;
    }

    public String getCity() {
        return city;
    }

    public String getDistrict() {
        return district;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public String getFullAddress() {
        return fullAddress;
    }

    public String getAddressLabel() {
        return addressLabel;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }
}
