package com.example.mxhconnectify.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterDTO {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Tên đầy đủ không được để trống")
    @Size(max = 100, message = "Tên đầy đủ không được vượt quá 100 ký tự")
    private String bio; // Tên đầy đủ hiển thị trên profile như thiết kế ban đầu

    @NotBlank(message = "Tên tài khoản không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3 đến 50 ký tự")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải chứa ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận lại mật khẩu")
    private String confirmPassword;
}