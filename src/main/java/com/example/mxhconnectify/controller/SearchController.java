package com.example.mxhconnectify.controller;

import com.example.mxhconnectify.dto.SearchUserDTO;
import com.example.mxhconnectify.entity.User;
import com.example.mxhconnectify.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/search")
public class SearchController {

    private UserService userService;

    @Autowired
    public SearchController(UserService userService) {
        this.userService = userService;
    }

    // 1. Trả về file search.html
    @GetMapping
    public String searchPage(){
        return "search";
    }

    @GetMapping("/users")
    @ResponseBody
    public List<SearchUserDTO> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            HttpServletRequest request) {

        User currentUser = (User) request.getSession().getAttribute("currentUser");

        // Nếu chưa đăng nhập, không trả về dữ liệu tìm kiếm
        if (currentUser == null) {
            return Collections.emptyList();
        }

        Pageable pageable = PageRequest.of(page, 20);

        if (keyword != null && !keyword.trim().isEmpty()) {
            // Khi có keyword: Tìm kiếm map động trạng thái Follow/Unfollow
            return userService.searchUsersByKeyword(keyword.trim(), currentUser.getId(), pageable);
        } else {
            // Khi không nhập gì: Trả về danh sách ngẫu nhiên những người CHƯA theo dõi
            return userService.getRandomUsers(currentUser.getId(), pageable);
        }
    }
}
