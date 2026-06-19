package com.example.mxhconnectify.repository;

import com.example.mxhconnectify.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    // Kiểm tra xem đã theo dõi chưa (để hiển thị nút Follow/Unfollow)
    boolean existsByFollower_IdAndFollowing_Id(Long followerId, Long followingId);

    // Tìm bản ghi follow cụ thể (để xóa khi Unfollow)
    Optional<Follow> findByFollower_IdAndFollowing_Id(Long followerId, Long followingId);

    // Đếm số người đang theo dõi (Following count)
    long countByFollower_Id(Long userId);

    // Đếm số người theo dõi mình (Follower count)
    long countByFollowing_Id(Long userId);

    // Xóa mối quan hệ follow
    void deleteByFollower_IdAndFollowing_Id(Long followerId, Long followingId);
}
