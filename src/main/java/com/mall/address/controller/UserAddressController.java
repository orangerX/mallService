package com.mall.address.controller;

import com.mall.address.dto.AddressIdRequest;
import com.mall.address.dto.AddressResponse;
import com.mall.address.dto.AddressSaveRequest;
import com.mall.address.dto.AddressUpdateRequest;
import com.mall.address.service.UserAddressService;
import com.mall.common.api.ApiResponse;
import com.mall.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/addresses")
public class UserAddressController {

    private final UserAddressService addressService;

    public UserAddressController(UserAddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ApiResponse<List<AddressResponse>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(addressService.list(user.getUserId()));
    }

    @GetMapping("/default")
    public ApiResponse<AddressResponse> getDefault(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(addressService.getDefault(user.getUserId()));
    }

    @PostMapping("/create")
    public ApiResponse<AddressResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody AddressSaveRequest request) {
        return ApiResponse.success(addressService.create(user.getUserId(), request));
    }

    @PostMapping("/update")
    public ApiResponse<AddressResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody AddressUpdateRequest request) {
        return ApiResponse.success(addressService.update(user.getUserId(), request));
    }

    @PostMapping("/delete")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthenticatedUser user,
                                    @Valid @RequestBody AddressIdRequest request) {
        addressService.delete(user.getUserId(), request.getAddressId());
        return ApiResponse.success();
    }

    @PostMapping("/set-default")
    public ApiResponse<AddressResponse> setDefault(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @Valid @RequestBody AddressIdRequest request) {
        return ApiResponse.success(addressService.setDefault(user.getUserId(), request.getAddressId()));
    }
}
