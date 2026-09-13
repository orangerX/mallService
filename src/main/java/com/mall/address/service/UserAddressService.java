package com.mall.address.service;

import com.mall.address.dto.AddressResponse;
import com.mall.address.dto.AddressSaveRequest;
import com.mall.address.dto.AddressUpdateRequest;
import com.mall.address.mapper.UserAddressMapper;
import com.mall.address.model.UserAddressEntity;
import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserAddressService {

    private final UserAddressMapper addressMapper;

    public UserAddressService(UserAddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<AddressResponse> list(Long userId) {
        return addressMapper.findByUserId(userId).stream()
                .map(AddressResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public AddressResponse getDefault(Long userId) {
        UserAddressEntity address = addressMapper.findDefaultByUserId(userId);
        if (address == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.ADDRESS_NOT_FOUND);
        }
        return AddressResponse.from(address);
    }

    @Transactional(rollbackFor = Exception.class)
    public AddressResponse create(Long userId, AddressSaveRequest request) {
        boolean makeDefault = Boolean.TRUE.equals(request.getDefaultAddress())
                || addressMapper.findDefaultByUserId(userId) == null;
        if (makeDefault) {
            addressMapper.clearDefaultByUserId(userId);
        }

        LocalDateTime now = LocalDateTime.now();
        UserAddressEntity address = new UserAddressEntity();
        address.setUserId(userId);
        applyRequest(address, request);
        address.setDefaultAddress(makeDefault);
        address.setCreatedAt(now);
        address.setUpdatedAt(now);
        addressMapper.insert(address);
        return AddressResponse.from(address);
    }

    @Transactional(rollbackFor = Exception.class)
    public AddressResponse update(Long userId, AddressUpdateRequest request) {
        UserAddressEntity address = requireOwnedAddress(userId, request.getAddressId());
        boolean makeDefault = Boolean.TRUE.equals(request.getDefaultAddress());
        if (makeDefault && !Boolean.TRUE.equals(address.getDefaultAddress())) {
            addressMapper.clearDefaultByUserId(userId);
            address.setDefaultAddress(true);
        }
        applyRequest(address, request);
        address.setUpdatedAt(LocalDateTime.now());
        addressMapper.update(address);
        return AddressResponse.from(address);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long addressId) {
        UserAddressEntity address = requireOwnedAddress(userId, addressId);
        addressMapper.delete(addressId, userId);
        if (Boolean.TRUE.equals(address.getDefaultAddress())) {
            List<UserAddressEntity> remaining = addressMapper.findByUserId(userId);
            if (!remaining.isEmpty()) {
                addressMapper.setDefault(remaining.get(0).getId(), userId);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public AddressResponse setDefault(Long userId, Long addressId) {
        UserAddressEntity address = requireOwnedAddress(userId, addressId);
        if (!Boolean.TRUE.equals(address.getDefaultAddress())) {
            addressMapper.clearDefaultByUserId(userId);
            addressMapper.setDefault(addressId, userId);
            address.setDefaultAddress(true);
        }
        return AddressResponse.from(address);
    }

    private UserAddressEntity requireOwnedAddress(Long userId, Long addressId) {
        UserAddressEntity address = addressMapper.findByIdAndUserId(addressId, userId);
        if (address == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.ADDRESS_NOT_FOUND);
        }
        return address;
    }

    private static void applyRequest(UserAddressEntity address, AddressSaveRequest request) {
        address.setReceiverName(request.getReceiverName().trim());
        address.setReceiverPhone(request.getReceiverPhone().trim());
        address.setProvince(request.getProvince().trim());
        address.setCity(request.getCity().trim());
        address.setDistrict(request.getDistrict().trim());
        address.setDetailAddress(request.getDetailAddress().trim());
        String label = request.getAddressLabel();
        address.setAddressLabel(label == null || label.trim().isEmpty() ? null : label.trim());
    }
}
