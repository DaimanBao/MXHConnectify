package com.example.mxhconnectify.controller;

import com.example.mxhconnectify.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
}
