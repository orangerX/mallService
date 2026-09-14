package com.mall.admin.config;

import com.mall.admin.mapper.AdminMapper;
import com.mall.admin.model.AdminEntity;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapTest {
    @Test
    void requiresCredentialsOnlyWhenNoAdminExists() {
        AdminMapper mapper = mock(AdminMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(mapper.count()).thenReturn(0L);
        assertThrows(IllegalStateException.class,
                () -> new AdminBootstrap(mapper, encoder, "", "").run(null));

        when(mapper.count()).thenReturn(1L);
        new AdminBootstrap(mapper, encoder, "", "").run(null);
        verify(mapper, never()).insert(any(AdminEntity.class));
    }

    @Test
    void createsFirstAdminWithBcryptHash() {
        AdminMapper mapper = mock(AdminMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(mapper.count()).thenReturn(0L);
        when(encoder.encode("123456")).thenReturn("bcrypt-hash");
        new AdminBootstrap(mapper, encoder, "admin", "123456").run(null);
        verify(mapper).insert(any(AdminEntity.class));
    }
}
