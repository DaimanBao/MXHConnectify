package com.example.mxhconnectify.service;

import com.example.mxhconnectify.entity.Follow;
import com.example.mxhconnectify.entity.User;
import com.example.mxhconnectify.repository.FollowRepository;
import com.example.mxhconnectify.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Autowired
    public FollowService(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    /**
     * Kiểm tra xem người dùng hiện tại đã theo dõi mục tiêu chưa
     */
    public boolean isFollowing(Long followerId, Long followingId) {
        if (followerId == null || followingId == null) return false;
        return followRepository.existsByFollower_IdAndFollowing_Id(followerId, followingId);
    }

    /**
     * Đếm số lượng người theo dõi (Followers) của một User
     */
    public long getFollowerCount(Long userId) {
        return followRepository.countByFollowing_Id(userId);
    }

    /**
     * Đếm số lượng người mà User đó đang theo dõi (Following)
     */
    public long getFollowingCount(Long userId) {
        return followRepository.countByFollower_Id(userId);
    }

    /**
     * Xử lý bật/tắt (Toggle) Follow và Unfollow trong một Transaction an toàn
     */
    @Transactional
    public Map<String, Object> toggleFollow(User currentUser, String targetUsername) {
        // 1. Tìm kiếm đối tượng được theo dõi
        User targetUser = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // 2. Ngăn chặn tự follow chính mình
        if (currentUser.getId().equals(targetUser.getId())) {
            throw new IllegalArgumentException("Bạn không thể tự theo dõi chính mình");
        }

        // 3. Kiểm tra trạng thái follow hiện tại
        Optional<Follow> existingFollow = followRepository.findByFollower_IdAndFollowing_Id(currentUser.getId(), targetUser.getId());
        boolean nowFollowing;

        if (existingFollow.isPresent()) {
            // Nếu đã tồn tại -> Thực hiện xóa bản ghi (Unfollow)
            followRepository.delete(existingFollow.get());
            nowFollowing = false;
        } else {
            // Nếu chưa tồn tại -> Tạo mới bản ghi (Follow)
            Follow follow = new Follow();
            follow.setFollower(currentUser);
            follow.setFollowing(targetUser);
            followRepository.save(follow);
            nowFollowing = true;
        }

        // 4. Lấy số lượng follower mới nhất sau khi thay đổi để trả về giao diện
        long newFollowerCount = followRepository.countByFollowing_Id(targetUser.getId());

        // Trả ra cấu trúc Map gọn gàng cho API
        return Map.of(
                "success", true,
                "isFollowing", nowFollowing,
                "followerCount", newFollowerCount
        );
    }
}
