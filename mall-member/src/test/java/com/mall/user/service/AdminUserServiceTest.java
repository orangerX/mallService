package com.mall.user.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.user.dto.AdminUserCreateRequest;
import com.mall.user.dto.AdminUserUpdateRequest;
import com.mall.user.mapper.UserMapper;
import com.mall.user.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    private AdminUserService userService;

    @BeforeEach
    void setUp() {
        userService = new AdminUserService(userMapper, passwordEncoder);
    }

    @Test
    void createsUserWithDefaultPassword() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setUsername("  orange_user  ");
        request.setStatus(1);
        when(passwordEncoder.encode("123456")).thenReturn("hash");
        doAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(8L);
            return 1;
        }).when(userMapper).insert(any(UserEntity.class));
        when(userMapper.findById(8L)).thenReturn(user(8L, "orange_user", 1));

        assertEquals("orange_user", userService.create(request).getUsername());
        verify(userMapper).insert(org.mockito.ArgumentMatchers.argThat(user ->
                "orange_user".equals(user.getUsername()) && "hash".equals(user.getPasswordHash())));
    }

    @Test
    void rejectsDuplicateUsername() {
        when(userMapper.findByUsername("orange_user")).thenReturn(user(1L, "orange_user", 1));
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setUsername("orange_user");
        request.setStatus(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.create(request));

        assertEquals(ErrorCode.USERNAME_EXISTS, exception.getErrorCode());
        verify(userMapper, never()).insert(any(UserEntity.class));
    }

    @Test
    void updatesExistingUser() {
        AdminUserUpdateRequest request = new AdminUserUpdateRequest();
        request.setUserId(8L);
        request.setUsername("new_name");
        request.setStatus(0);
        when(userMapper.findById(8L))
                .thenReturn(user(8L, "old_name", 1), user(8L, "new_name", 0));

        assertEquals(0, userService.update(request).getStatus());
        verify(userMapper).updateAdminUser(8L, "new_name", 0);
    }

    @Test
    void resetsPasswordToDefault() {
        when(userMapper.findById(8L)).thenReturn(user(8L, "orange_user", 1));
        when(passwordEncoder.encode("123456")).thenReturn("new-hash");

        userService.resetPassword(8L);

        verify(userMapper).updatePassword(8L, "new-hash");
    }

    @Test
    void refusesToDeleteUserWithOrders() {
        when(userMapper.findById(8L)).thenReturn(user(8L, "orange_user", 1));
        when(userMapper.countOrders(8L)).thenReturn(2L);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.delete(8L));

        assertEquals(ErrorCode.USER_HAS_ORDERS, exception.getErrorCode());
        verify(userMapper, never()).deleteById(8L);
    }

    private static UserEntity user(Long id, String username, int status) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setStatus(status);
        return user;
    }
}
