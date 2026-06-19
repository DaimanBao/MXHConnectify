package com.example.mxhconnectify.controller;

import com.example.mxhconnectify.dto.SearchUserDTO;
import com.example.mxhconnectify.entity.Post;
import com.example.mxhconnectify.entity.User;
import com.example.mxhconnectify.service.LikeService;
import com.example.mxhconnectify.service.PostService;
import com.example.mxhconnectify.service.SavedPostService;
import com.example.mxhconnectify.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/explore")
public class ExploreController {

    private final PostService postService;
    private final UserService userService;
    private final LikeService likeService;
    private final SavedPostService savedPostService;

    @Autowired
    public ExploreController(PostService postService, UserService userService, LikeService likeService, SavedPostService savedPostService) {
        this.postService = postService;
        this.userService = userService;
        this.likeService = likeService;
        this.savedPostService = savedPostService;
    }

    @GetMapping
    public String explore(HttpServletRequest request,
                          @RequestParam(defaultValue = "0") int page,
                          Model model) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("currentUser") == null) {
            return "redirect:/login";
        }

        User currentUser = (User) session.getAttribute("currentUser");

        // 1. Lấy danh sách bài viết của những người MÌNH CHƯA THEO DÕI
        int pageSize = 20;
        Page<Post> explorePage = postService.getExploreFeed(currentUser, page, pageSize);
        likeService.setLikeStatusForPosts(explorePage.getContent(), currentUser);
        savedPostService.setSaveStatusForPosts(explorePage.getContent(), currentUser);

        model.addAttribute("posts", explorePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", explorePage.getTotalPages());

        // 2. Danh sách 5 người gợi ý ngẫu nhiên (vẫn giữ cho cột dọc bên phải)
        Pageable topFive = PageRequest.of(0, 5);
        List<SearchUserDTO> suggestionList = userService.getRandomUsers(currentUser.getId(), topFive);
        model.addAttribute("suggestions", suggestionList);

        return "explore";
    }
}