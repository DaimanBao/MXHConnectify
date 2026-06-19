package com.example.mxhconnectify.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Kiểm tra xem trong Session có 'currentUser' không
        Object user = request.getSession().getAttribute("currentUser");

        if (user == null) {
            // Nếu chưa đăng nhập, đá về trang login
            response.sendRedirect("/login");
            return false; // Chặn request không cho vào Controller
        }
        return true; // Cho phép đi tiếp
    }
}