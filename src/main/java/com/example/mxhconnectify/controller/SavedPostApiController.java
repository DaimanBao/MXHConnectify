package com.example.mxhconnectify.controller;

import com.example.mxhconnectify.entity.User;
import com.example.mxhconnectify.service.SavedPostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class SavedPostApiController {

    private final SavedPostService savedPostService;

    @Autowired
    public SavedPostApiController(SavedPostService savedPostService) {
        this.savedPostService = savedPostService;
    }

    @PostMapping("/{postId}/save")
    public ResponseEntity<?> toggleSave(@PathVariable Long postId, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("currentUser") == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Vui lòng đăng nhập trước!"));
        }

        User currentUser = (User) session.getAttribute("currentUser");
        try {
            Map<String, Object> result = savedPostService.toggleSavePost(postId, currentUser);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
