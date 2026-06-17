package com.example.mxhconnectify.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // Công tắc 1: Báo cho Spring biết đây là class cấu hình
public class SecurityConfig {

    @Bean // Công tắc 2: Ra lệnh cho Spring khởi tạo và quản lý đối tượng này
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Cho phép tất cả mọi thứ đi qua lớp Security
                        // Để nhiệm vụ kiểm tra "đã đăng nhập chưa" cho AuthInterceptor lo liệu
                        .anyRequest().permitAll()
                )
                // Tắt hẳn form login của Spring Security để không xung đột với AuthController
                .formLogin(form -> form.disable());
        return http.build();
    }
}