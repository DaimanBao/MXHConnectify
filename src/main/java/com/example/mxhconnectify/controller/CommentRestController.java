package com.example.mxhconnectify.controller;

import com.example.mxhconnectify.entity.Post;
import com.example.mxhconnectify.entity.User;
import com.example.mxhconnectify.service.CommentService;
import com.example.mxhconnectify.service.LikeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentRestController {

    private final CommentService commentService;
    private final LikeService likeService;

    @Autowired
    public CommentRestController(CommentService commentService, LikeService likeService) {
        this.commentService = commentService;
        this.likeService = likeService;
    }

    /**
     * API 1: Lấy toàn bộ danh sách Bình luận cấp 1 (COMMENT) của một bài viết.
     * URL: GET /api/comments/post/{postId}
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<?> getCommentsByPost(@PathVariable Long postId, HttpServletRequest request) {
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        try {
            List<Post> comments = commentService.getCommentsByPostId(postId);

            // Quét và thiết lập trạng thái thả tim cho từng bình luận nếu người dùng đã đăng nhập
            if (currentUser != null) {
                likeService.setLikeStatusForPosts(comments, currentUser);
            }

            // Chuyển đổi sang cấu trúc Map sạch để tránh lỗi tuần tự hóa JSON vòng lặp của JPA Entity
            List<Map<String, Object>> responseList = convertPostListToResponse(comments);
            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi khi lấy danh sách bình luận: " + e.getMessage()));
        }
    }

    /**
     * API 2: Lấy danh sách Phản hồi (REPLY) của một bình luận gốc theo cơ chế Phân trang (Xem thêm).
     * URL: GET /api/comments/comment/{commentId}/replies?page=0&size=3
     */
    @GetMapping("/comment/{commentId}/replies")
    public ResponseEntity<?> getRepliesByComment(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            HttpServletRequest request) {

        User currentUser = (User) request.getSession().getAttribute("currentUser");
        try {
            Slice<Post> replySlice = commentService.getRepliesByCommentId(commentId, page, size);
            List<Post> replies = replySlice.getContent();

            // Quét trạng thái thả tim cho danh sách câu trả lời
            if (currentUser != null) {
                likeService.setLikeStatusForPosts(replies, currentUser);
            }

            List<Map<String, Object>> replyResponseList = convertPostListToResponse(replies);

            // Gói thêm thông tin phân trang (liệu còn trang tiếp theo để hiện nút "Xem thêm" hay không)
            Map<String, Object> result = new HashMap<>();
            result.put("replies", replyResponseList);
            result.put("hasNext", replySlice.hasNext()); // Trả về true nếu vẫn còn reply chưa tải hết
            result.put("currentPage", page);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi tải phản hồi: " + e.getMessage()));
        }
    }

    /**
     * API 3: Xử lý tạo mới một Bình luận (COMMENT) hoặc Phản hồi (REPLY).
     * URL: POST /api/comments/create
     */
    @PostMapping("/create")
    public ResponseEntity<?> createCommentOrReply(
            @RequestParam("postId") Long postId,
            @RequestParam(value = "parentId", required = false) Long parentId,
            @RequestParam("content") String content,
            @RequestParam(value = "imageFile", required = false) MultipartFile file,
            HttpServletRequest request) {

        User currentUser = (User) request.getSession().getAttribute("currentUser");

        // Chặn nếu người dùng chưa đăng nhập
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Vui lòng đăng nhập để thực hiện bình luận!"));
        }

        // Chặn nếu nội dung chữ rỗng và không đính kèm ảnh
        if ((content == null || content.trim().isEmpty()) && (file == null || file.isEmpty())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Nội dung bình luận không được để trống!"));
        }

        try {
            Post newInteraction = commentService.createCommentOrReply(currentUser, postId, parentId, content, file);

            // Thiết lập mặc định cho đối tượng mới tạo
            newInteraction.setLikedByCurrentUser(false);

            // Biến đổi bản ghi vừa tạo thành cấu trúc JSON rút gọn trả về cho FE chèn ngay lập tức vào UI
            Map<String, Object> responseData = convertSinglePostToMap(newInteraction);
            responseData.put("success", true);

            return ResponseEntity.ok(responseData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Có lỗi hệ thống xảy ra: " + e.getMessage()));
        }
    }

    // ==================== CÁC HÀM BỔ TRỢ CHUYỂN ĐỔI CẤU TRÚC DỮ LIỆU (MAPPING) ====================

    private List<Map<String, Object>> convertPostListToResponse(List<Post> posts) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Post p : posts) {
            list.add(convertSinglePostToMap(p));
        }
        return list;
    }

    private Map<String, Object> convertSinglePostToMap(Post p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("parentId", p.getParentId());
        map.put("type", p.getType().name());
        map.put("content", p.getContent());
        map.put("likeCount", p.getLikeCount());
        map.put("commentCount", p.getCommentCount()); // Đối với comment gốc, nó có thể dùng để biết có bao nhiêu reply
        map.put("createdAt", p.getCreatedAt().toString());
        map.put("isLiked", p.isLikedByCurrentUser());

        // Mapping thông tin User viết bình luận
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", p.getUser().getId());
        userMap.put("username", p.getUser().getUsername());
        userMap.put("avatarUrl", p.getUser().getAvatarUrl() != null ? p.getUser().getAvatarUrl() : "https://cdn-icons-png.flaticon.com/512/149/149071.png");
        map.put("user", userMap);

        // Mapping danh sách ảnh (Nếu có)
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