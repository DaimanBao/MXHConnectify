package com.example.mxhconnectify.repository;

import com.example.mxhconnectify.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    // Kiểm tra xem đã like chưa
    Optional<Like> findByUser_IdAndPost_Id(Long userId, Long postId);

    // Đếm số lượng like (dù chúng ta có likeCount trong bảng Post,
    // phương thức này vẫn hữu ích để kiểm tra dữ liệu hoặc chạy batch job đồng bộ)
    long countByPost_Id(Long postId);

    // Xóa like trực tiếp
    void deleteByUser_IdAndPost_Id(Long userId, Long postId);
}
