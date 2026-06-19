package com.example.mxhconnectify.controller;

import com.example.mxhconnectify.entity.Post;
import com.example.mxhconnectify.entity.User;
import com.example.mxhconnectify.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/home")
public class HomeController {

    private final PostService postService;

    @Autowired
    public HomeController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public String home(HttpServletRequest request,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        // 1. Lấy session từ request hiện tại
        HttpSession session = request.getSession(false);

        // 2. Kiểm tra nếu chưa đăng nhập thì bắt quay về trang login
        if (session == null || session.getAttribute("currentUser") == null) {
            return "redirect:/login";
        }

        // 3. Lấy thông tin đối tượng User đang đăng nhập từ Session
        User currentUser = (User) session.getAttribute("currentUser");

        // 4. Gọi Service lấy danh sách bài viết thuộc Home Feed (gồm bài của mình và người mình follow)
        // Thiết lập kích thước mỗi trang là 5 bài để tối ưu hiệu năng hiển thị bảng tin
        int pageSize = 5;
        Page<Post> postPage = postService.getHomeFeed(currentUser, page, pageSize);

        // 5. Đẩy dữ liệu danh sách bài viết qua Model sang view Thymeleaf
        // postPage.getContent() sẽ trả về List<Post> giúp th:each="post : ${posts}" duyệt lặp mượt mà
        model.addAttribute("posts", postPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", postPage.getTotalPages());

        // Trả về file giao diện home.html
        return "home";
    }
}