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

import java.util.List;

@Controller
@RequestMapping("/search")
public class SearchController {
    @Autowired
    private UserService userService;

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
        Long currentId = (currentUser != null) ? currentUser.getId() : -1L;
        Pageable pageable = PageRequest.of(page, 20);

        if (keyword != null && !keyword.trim().isEmpty()) {
            // Truyền thêm currentId vào đây
            return userService.searchUsersByKeyword(keyword, currentId, pageable);
        } else {
            return userService.getRandomUsers(currentId, pageable);
        }
    }
}
