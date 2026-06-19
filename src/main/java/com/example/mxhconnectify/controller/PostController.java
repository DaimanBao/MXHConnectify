package com.example.mxhconnectify.controller;

import com.example.mxhconnectify.dto.PostDTO;
import com.example.mxhconnectify.entity.Post;
import com.example.mxhconnectify.entity.User;
import com.example.mxhconnectify.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Optional;

@Controller
@RequestMapping("/post")
public class PostController {

    private final PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public String createPostPage(){
        return "create-post";
    }

    @PostMapping("/create")
    public String handleCreatePost(@ModelAttribute PostDTO postDTO,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        // 1. Lấy session từ request
        HttpSession session = request.getSession(false); // false để không tạo session mới nếu chưa có

        // 2. Kiểm tra session và user
        if (session == null || session.getAttribute("currentUser") == null) {
            return "redirect:/login";
        }

        User currentUser = (User) session.getAttribute("currentUser");

        try {
            // 3. Gọi service để lưu bài viết
            postService.createPost(currentUser, postDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng bài viết thành công!");
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi tải file!");
        }

        return "redirect:/home";
    }
}
