package com.example.mxhconnectify.controller;

import com.example.mxhconnectify.dto.ProfileUpdateDTO;
import com.example.mxhconnectify.entity.Post;
import com.example.mxhconnectify.entity.User;
import com.example.mxhconnectify.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("profile")
public class ProfileController {

    private final UserService userService;
    private final FollowService followService;
    private final PostService postService;
    private final SavedPostService savedPostService;
    private final LikeService likeService;

    @Autowired
    public ProfileController(UserService userService, FollowService followService, PostService postService, SavedPostService savedPostService, LikeService likeService) {
        this.userService = userService;
        this.followService = followService;
        this.postService = postService;
        this.savedPostService = savedPostService;
        this.likeService = likeService;
    }

    @GetMapping("/{username}")
    public String profilePage(@PathVariable String username, Model model, HttpServletRequest request) {
        // 1. Lấy thông tin user dựa theo username trên URL để hiển thị
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // 2. Lấy thông tin tài khoản đang đăng nhập từ Session
        User currentUser = (User) request.getSession().getAttribute("currentUser");

        // 3. Logic kiểm tra chủ sở hữu để ẩn/hiện nút chỉnh sửa hoặc theo dõi
        boolean isOwner = false;
        boolean isFollowing = false; // Mặc định chưa follow nếu chưa đăng nhập

        if (currentUser != null) {
            if (currentUser.getId().equals(user.getId())) {
                isOwner = true;
            } else {
                // Giao tiếp qua tầng Service kiểm tra trạng thái follow
                isFollowing = followService.isFollowing(currentUser.getId(), user.getId());
            }
        }

        // 4. Lấy danh sách bài viết tự đăng của trang cá nhân đang xem
        List<Post> userPosts = postService.getUserPosts(user.getId());

        // Chỉ thiết lập trạng thái Like/Save khi có người dùng đang đăng nhập hệ thống
        if (currentUser != null) {
            likeService.setLikeStatusForPosts(userPosts, currentUser);
            savedPostService.setSaveStatusForPosts(userPosts, currentUser);
        }
        model.addAttribute("posts", userPosts);

        // 5. Nếu là chính chủ (isOwner = true), lấy thêm danh sách bài viết đã lưu để đổ vào Tab Đã lưu
        if (isOwner && currentUser != null) {
            List<Post> savedPosts = savedPostService.getSavedPostsByUserId(currentUser.getId());
            likeService.setLikeStatusForPosts(savedPosts, currentUser); // Quét trạng thái like cho bài viết đã lưu
            savedPostService.setSaveStatusForPosts(savedPosts, currentUser); // Quét trạng thái lưu cho bài viết đã lưu
            model.addAttribute("savedPosts", savedPosts);
        }

        // 6. Lấy các thông số đếm số lượng thống kê
        long followerCount = followService.getFollowerCount(user.getId());
        long followingCount = followService.getFollowingCount(user.getId());
        long postCount = postService.getPostCountByUserId(user.getId());

        // 7. Đẩy dữ liệu xuống Thymeleaf hiển thị lên giao diện HTML
        model.addAttribute("user", user);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isFollowing", isFollowing);
        model.addAttribute("followerCount", followerCount);
        model.addAttribute("followingCount", followingCount);
        model.addAttribute("postCount", postCount);

        return "profile";
    }

    @PostMapping("/{username}/follow")
    @ResponseBody
    public ResponseEntity<?> toggleFollow(@PathVariable String username, HttpServletRequest request) {
        User currentUser = (User) request.getSession().getAttribute("currentUser");

        // Kiểm tra xem người dùng đã đăng nhập chưa
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
        }

        try {
            // Đẩy toàn bộ logic phức tạp xuống cho Service gánh vác xử lý dữ liệu DB
            Map<String, Object> result = followService.toggleFollow(currentUser, username);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Có lỗi hệ thống xảy ra"));
        }
    }

    @PostMapping("/edit")
    public String updateProfile(@ModelAttribute ProfileUpdateDTO profileDTO,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        User currentUser = (User) request.getSession().getAttribute("currentUser");

        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            // Cập nhật thông tin mới vào đối tượng user lấy từ session
            currentUser.setFullName(profileDTO.getFullName());
            currentUser.setHeadline(profileDTO.getHeadline());
            currentUser.setDescription(profileDTO.getDescription());
            currentUser.setCommunityLinks(profileDTO.getCommunityLinks());

            // Gọi tầng Service cập nhật thông tin xuống Database
            userService.updateProfile(currentUser);

            // Làm mới lại dữ liệu trong Session để đồng bộ hiển thị lên toàn bộ hệ thống ngay lập tức
            request.getSession().setAttribute("currentUser", currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thành công!");
            return "redirect:/profile/" + currentUser.getUsername();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/profile/" + currentUser.getUsername();
        }
    }

    @PostMapping("/edit/avatar")
    public String updateAvatar(@RequestParam("avatarFile") MultipartFile file,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            // Xử lý upload và cập nhật ảnh đại diện mới qua Service
            userService.updateAvatar(currentUser, file);
            request.getSession().setAttribute("currentUser", currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật ảnh đại diện!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/profile/" + currentUser.getUsername();
    }
}