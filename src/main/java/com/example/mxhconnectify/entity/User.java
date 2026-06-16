package com.example.mxhconnectify.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên tài khoản không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3 đến 50 ký tự")
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải chứa ít nhất 6 ký tự")
    @Column(nullable = false)
    private String password;

    @Size(max = 100, message = "Tiểu sử không được vượt quá 100 ký tự")
    @Column(length = 100)
    private String bio;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "link_community")
    private String linkCommunity;

    // ================= XÁC THỰC & BẢO MẬT =================

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

    @Column(name = "email_token")
    private String emailToken;

    @Column(name = "email_token_expiry")
    private LocalDateTime emailTokenExpiry;

    @Column(name = "forgot_password_token")
    private String forgotPasswordToken;

    @Column(name = "forgot_password_token_expiry")
    private LocalDateTime forgotPasswordTokenExpiry;
}