package com.example.mxhconnectify.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    // --- CÁC TRƯỜNG MỚI PHỤC VỤ PROFILE V1 ---

    @Column(name = "full_name", length = 100)
    private String fullName; // Tên hiển thị (Ví dụ: Nguyễn Văn Bảo)

    @Column(name = "headline", length = 150)
    private String headline; // Định danh nghề nghiệp/Trường học

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl; // Đường dẫn ảnh, mặc định null

    @Column(name = "community_links", columnDefinition = "TEXT")
    private String communityLinks; // Lưu chuỗi các link ngăn cách bởi dấu phẩy

    // --- TOKENS BẢO MẬT (AUTH) ---

    @Column(name = "email_token")
    private String emailToken;

    @Column(name = "email_token_expiry")
    private LocalDateTime emailTokenExpiry;

    @Column(name = "forgot_password_token")
    private String forgotPasswordToken;

    @Column(name = "forgot_password_token_expiry")
    private LocalDateTime forgotPasswordTokenExpiry;

    // --- HELPER METHOD: Tự động chuyển đổi chuỗi liên kết thành List để Thymeleaf duyệt lặp ---
    public List<String> getCommunityLinksList() {
        if (this.communityLinks == null || this.communityLinks.trim().isEmpty()) {
            return new ArrayList<>();
        }
        // Cắt chuỗi bằng dấu phẩy và loại bỏ khoảng trắng thừa
        return Arrays.stream(this.communityLinks.split(","))
                .map(String::trim)
                .toList();
    }
}