package com.mall.admin.config;

import com.mall.admin.mapper.AdminMapper;
import com.mall.admin.model.AdminEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final AdminMapper mapper;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;

    public AdminBootstrap(AdminMapper mapper, PasswordEncoder encoder,
                          @Value("${ADMIN_USERNAME:}") String username,
                          @Value("${ADMIN_PASSWORD:}") String password) {
        this.mapper = mapper; this.encoder = encoder; this.username = username; this.password = password;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        if (mapper.count() > 0) return;
        if (username.trim().isEmpty() || password.isEmpty()) {
            throw new IllegalStateException("sys_admin 为空：首次启动必须设置 ADMIN_USERNAME 和 ADMIN_PASSWORD");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("ADMIN_PASSWORD 的 UTF-8 长度不能超过 72 字节");
        }
        AdminEntity admin = new AdminEntity();
        admin.setUsername(username.trim());
        admin.setPasswordHash(encoder.encode(password));
        admin.setStatus(AdminEntity.STATUS_ENABLED);
        mapper.insert(admin);
    }
}
