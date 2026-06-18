package com.example.mxhconnectify;

import com.example.mxhconnectify.security.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {


    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**") // Chặn tất cả
                .excludePathPatterns(
                        "/login",
                        "/login/**",
                        "/register",
                        "/register/**",
                        "/verify-email",
                        "/forgot-password",
                        "/reset-password",
                        "/logout", 
                        "/css/**",
                        "/js/**",
                        "/images/**" // Trừ các trang Auth và file tĩnh
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:E:/MXHConnectify/uploads/");
    }
}