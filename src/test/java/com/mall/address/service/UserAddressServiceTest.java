package com.mall.address.service;

import com.mall.address.dto.AddressResponse;
import com.mall.address.dto.AddressSaveRequest;
import com.mall.address.dto.AddressUpdateRequest;
import com.mall.address.mapper.UserAddressMapper;
import com.mall.address.model.UserAddressEntity;
import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAddressServiceTest {

    @Test
    void firstAddressBecomesDefaultAutomatically() {
        UserAddressMapper mapper = mock(UserAddressMapper.class);
        when(mapper.insert(any(UserAddressEntity.class))).thenAnswer(invocation -> {
            UserAddressEntity address = invocation.getArgument(0);
            address.setId(9L);
            return 1;
        });
        UserAddressService service = new UserAddressService(mapper);

        AddressResponse response = service.create(7L, request(false));

        assertEquals(9L, response.getId());
        assertTrue(response.isDefaultAddress());
        assertEquals("上海市上海市浦东新区示例路1号", response.getFullAddress());
        verify(mapper).clearDefaultByUserId(7L);
        verify(mapper).insert(any(UserAddressEntity.class));
    }

    @Test
    void requestedDefaultAddressReplacesExistingDefault() {
        UserAddressMapper mapper = mock(UserAddressMapper.class);
        when(mapper.findDefaultByUserId(7L)).thenReturn(address(1L, true));
        when(mapper.insert(any(UserAddressEntity.class))).thenAnswer(invocation -> {
            UserAddressEntity address = invocation.getArgument(0);
            address.setId(10L);
            return 1;
        });
        UserAddressService service = new UserAddressService(mapper);

        AddressResponse response = service.create(7L, request(true));

        assertTrue(response.isDefaultAddress());
        verify(mapper).clearDefaultByUserId(7L);
    }

    @Test
    void updateCannotModifyAnotherUsersAddress() {
        UserAddressMapper mapper = mock(UserAddressMapper.class);
        UserAddressService service = new UserAddressService(mapper);
        AddressUpdateRequest request = updateRequest(99L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.update(7L, request));

        assertEquals(ErrorCode.ADDRESS_NOT_FOUND, exception.getErrorCode());
        verify(mapper, never()).update(any(UserAddressEntity.class));
    }

    @Test
    void deletingDefaultAddressPromotesRemainingAddress() {
        UserAddressMapper mapper = mock(UserAddressMapper.class);
        when(mapper.findByIdAndUserId(1L, 7L)).thenReturn(address(1L, true));
        when(mapper.findByUserId(7L)).thenReturn(Collections.singletonList(address(2L, false)));
        UserAddressService service = new UserAddressService(mapper);

        service.delete(7L, 1L);

        verify(mapper).delete(1L, 7L);
        verify(mapper).setDefault(2L, 7L);
    }

    private static AddressSaveRequest request(boolean defaultAddress) {
        AddressSaveRequest request = new AddressSaveRequest();
        request.setReceiverName("测试用户");
        request.setReceiverPhone("13800138000");
        request.setProvince("上海市");
        request.setCity("上海市");
        request.setDistrict("浦东新区");
        request.setDetailAddress("示例路1号");
        request.setAddressLabel("家");
        request.setDefaultAddress(defaultAddress);
        return request;
    }

    private static AddressUpdateRequest updateRequest(Long addressId) {
        AddressSaveRequest source = request(false);
        AddressUpdateRequest request = new AddressUpdateRequest();
        request.setAddressId(addressId);
        request.setReceiverName(source.getReceiverName());
        request.setReceiverPhone(source.getReceiverPhone());
        request.setProvince(source.getProvince());
        request.setCity(source.getCity());
        request.setDistrict(source.getDistrict());
        request.setDetailAddress(source.getDetailAddress());
        request.setAddressLabel(source.getAddressLabel());
        request.setDefaultAddress(source.getDefaultAddress());
        return request;
    }

    private static UserAddressEntity address(Long id, boolean defaultAddress) {
        UserAddressEntity address = new UserAddressEntity();
        address.setId(id);
        address.setUserId(7L);
        address.setReceiverName("测试用户");
        address.setReceiverPhone("13800138000");
        address.setProvince("上海市");
        address.setCity("上海市");
        address.setDistrict("浦东新区");
        address.setDetailAddress("示例路1号");
        address.setDefaultAddress(defaultAddress);
        return address;
    }
}
