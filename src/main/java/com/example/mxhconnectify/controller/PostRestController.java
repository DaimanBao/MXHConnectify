package com.example.mxhconnectify.controller;

import com.example.mxhconnectify.entity.Post;
import com.example.mxhconnectify.entity.User;
import com.example.mxhconnectify.service.LikeService;
import com.example.mxhconnectify.service.PostService;
import com.example.mxhconnectify.service.SavedPostService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
public class PostRestController {

    private final PostService postService;
    private final LikeService likeService;
    private final SavedPostService savedPostService;

    @Autowired
    public PostRestController(PostService postService, LikeService likeService, SavedPostService savedPostService) {
        this.postService = postService;
        this.likeService = likeService;
        this.savedPostService = savedPostService;
    }

    /**
     * API: Lấy thông tin chi tiết của một bài viết (Dùng cho Modal chi tiết bài viết)
     * URL: GET /api/posts/{postId}
     */
    @GetMapping("/{postId}")
    public ResponseEntity<?> getPostDetail(@PathVariable Long postId, HttpServletRequest request) {
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        try {
            // 1. Tìm bài viết theo ID từ database
            Optional<Post> postOpt = postService.findById(postId);

            if (postOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Bài viết không tồn tại!"));
            }

            Post post = postOpt.get();

            // 2. Quét và thiết lập trạng thái Thích và Lưu bài viết nếu người dùng đã đăng nhập
            if (currentUser != null) {
                likeService.setLikeStatusForPosts(List.of(post), currentUser);
                savedPostService.setSaveStatusForPosts(List.of(post), currentUser);
            }

            // 3. Chuyển đổi sang cấu trúc Map sạch để tránh lỗi tuần tự hóa JSON vòng lặp (JPA Infinite Loop)
            Map<String, Object> responseData = convertSinglePostToMap(post);

            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi khi lấy chi tiết bài viết: " + e.getMessage()));
        }
    }

    // ==================== HÀM BỔ TRỢ CHUYỂN ĐỔI CẤU TRÚC DỮ LIỆU (MAPPING) ====================

    private Map<String, Object> convertSinglePostToMap(Post p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("content", p.getContent());
        map.put("likeCount", p.getLikeCount() != null ? p.getLikeCount() : 0);
        map.put("commentCount", p.getCommentCount() != null ? p.getCommentCount() : 0);
        map.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
        map.put("likedByCurrentUser", p.isLikedByCurrentUser());
        map.put("savedByCurrentUser", p.isSavedByCurrentUser());

        // Mapping thông tin User chủ bài viết
        Map<String, Object> userMap = new HashMap<>();
        if (p.getUser() != null) {
            userMap.put("id", p.getUser().getId());
            userMap.put("username", p.getUser().getUsername());
            userMap.put("avatarUrl", p.getUser().getAvatarUrl() != null && !p.getUser().getAvatarUrl().isEmpty()
                    ? p.getUser().getAvatarUrl()
                    : "https://cdn-icons-png.flaticon.com/512/149/149071.png");
        }
        map.put("user", userMap);

        // Mapping danh sách ảnh/video (Media) của bài viết
        List<Map<String, Object>> mediaList = new ArrayList<>();
        if (p.getMediaList() != null) {
            p.getMediaList().forEach(m -> {
                Map<String, Object> med = new HashMap<>();
                med.put("id", m.getId());
                med.put("url", m.getUrl());
                med.put("type", m.getType());
                mediaList.add(med);
            });
        }
        map.put("mediaList", mediaList);

        return map;
    }
}