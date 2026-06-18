package com.example.mxhconnectify.controller;


import com.example.mxhconnectify.dto.ProfileUpdateDTO;
import com.example.mxhconnectify.entity.User;
import com.example.mxhconnectify.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("profile")
public class ProfileController {

    private final UserService userService;

    @Autowired
    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{username}")
    public String profilePage(@PathVariable String username, Model model, HttpServletRequest request ) {
        // 1. Lấy thông tin user dựa theo username trên URL để hiển thị
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // 2. Lấy thông tin tài khoản đang đăng nhập từ Session
        User currentUser = (User) request.getSession().getAttribute("currentUser");

        // 3. Logic kiểm tra chủ sở hữu để ẩn/hiện nút chỉnh sửa
        boolean isOwner = false;
        if (currentUser != null && currentUser.getUsername().equals(user.getUsername())) {
            isOwner = true;
        }

        // 4. Đẩy dữ liệu xuống Thymeleaf
        model.addAttribute("user", user);
        model.addAttribute("isOwner", isOwner);
        return "profile";
    }

    @PostMapping("/edit")
    public String updateProfile(@ModelAttribute ProfileUpdateDTO profileDTO
                                                , HttpServletRequest request
                                                , RedirectAttributes redirectAttributes){
        User currentUser = (User) request.getSession().getAttribute("currentUser");

        if (currentUser == null) {
            return "redirect:/login";
        }

        try{
            // Cập nhật thông tin vào đối tượng user lấy từ session
            currentUser.setFullName(profileDTO.getFullName());
            currentUser.setHeadline(profileDTO.getHeadline());
            currentUser.setDescription(profileDTO.getDescription());
            currentUser.setCommunityLinks(profileDTO.getCommunityLinks());

            // Gọi Service
            userService.updateProfile(currentUser);

            // Cập nhật lại Session để các trang khác hiển thị thông tin mới nhất
            request.getSession().setAttribute("currentUser", currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thành công!");
            return "redirect:/profile/" + currentUser.getUsername();
        }catch (Exception e){
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/profile/" + currentUser.getUsername();
        }
    }
    @PostMapping("/edit/avatar")
    public String updateAvatar(@RequestParam("avatarFile") MultipartFile file,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (currentUser == null) return "redirect:/login";

        try {
            userService.updateAvatar(currentUser, file);
            request.getSession().setAttribute("currentUser", currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật ảnh đại diện!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/profile/" + currentUser.getUsername();
    }
}
