package com.example.mxhconnectify.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginDTO {

    @NotBlank(message = "Tên tài khoản hoặc email không được để trống")
    private String usernameOrEmail; // Đặt tên bao hàm vì Instagram cho phép đăng nhập bằng cả 2

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}