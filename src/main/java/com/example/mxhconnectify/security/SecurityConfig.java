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
                .csrf(csrf -> csrf.disable()) // Tắt CSRF bảo vệ để dễ test API (Postman/Frontend)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // Cho phép tất cả các request đi qua không cần đăng nhập
                )
                .formLogin(form -> form.disable()) // Tắt form login mặc định trong hình của bạn
                .httpBasic(basic -> basic.disable()); // Tắt luôn cổng đăng nhập basic qua popup browser

        return http.build();
    }
}