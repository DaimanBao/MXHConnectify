package com.example.mxhconnectify.controller;

import com.example.mxhconnectify.dto.LoginDTO;
import com.example.mxhconnectify.dto.RegisterDTO;
import com.example.mxhconnectify.entity.User;
import com.example.mxhconnectify.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.Optional;

@Controller
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // ================= LUỒNG ĐĂNG NHẬP (LOGIN) =================

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request, Model model) {
        // 1. Nếu đã có sẵn Session đăng nhập rồi, cho bay thẳng vào trang cá nhân luôn
        if (request.getSession().getAttribute("currentUser") != null) {
            User user = (User) request.getSession().getAttribute("currentUser");
            return "redirect:/profile/" + user.getUsername();
        }

        // 2. Nếu chưa có Session, đi quét mảng Cookie xem người dùng có bật Remember Me trước đó không
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            // Tìm xem có tồn tại cookie tên là "remember_me_user" hay không
            Optional<Cookie> rememberMeCookie = Arrays.stream(cookies)
                    .filter(c -> "remember_me_user".equals(c.getName()))
                    .findFirst();

            if (rememberMeCookie.isPresent()) {
                String savedUsername = rememberMeCookie.get().getValue();

                // Tìm kiếm thông tin User trong Database dựa trên Username lấy từ Cookie
                Optional<User> userOpt = userService.findByUsername(savedUsername);

                if (userOpt.isPresent() && userOpt.get().isActive()) {
                    // TỰ ĐỘNG ĐĂNG NHẬP: Tái thiết lập Session tự động mà không cần bắt họ nhập form
                    request.getSession().setAttribute("currentUser", userOpt.get());
                    return "redirect:/profile/" + userOpt.get().getUsername();
                }
            }
        }

        // 3. Nếu không tích checkbox (không có cookie) hoặc cookie không hợp lệ, hiển thị trang login bình thường
        model.addAttribute("loginDTO", new LoginDTO());
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@Valid @ModelAttribute("loginDTO") LoginDTO loginDTO,
                              BindingResult bindingResult,
                              @RequestParam(value = "rememberMe", required = false) Boolean rememberMe,
                              HttpServletRequest request,
                              HttpServletResponse response,
                              Model model) {

        if (bindingResult.hasErrors()) {
            return "login";
        }

        try {
            User loggedInUser = userService.authenticateUser(loginDTO);

            // ĐÚNG LUỒNG: Lưu thông tin đăng nhập vào HTTP Session thuần
            request.getSession().setAttribute("currentUser", loggedInUser);

            // XỬ LÝ REMEMBER ME: Nếu người dùng tích chọn "Ghi nhớ đăng nhập"
            if (rememberMe != null && rememberMe) {
                Cookie rememberMeCookie = new Cookie("remember_me_user", loggedInUser.getUsername());
                rememberMeCookie.setMaxAge(7 * 24 * 60 * 60); // Sống trong 7 ngày (tính bằng giây)
                rememberMeCookie.setPath("/"); // Có hiệu lực toàn bộ website
                rememberMeCookie.setHttpOnly(true); // Bảo mật, chống hacker cắp qua JS (XSS)
                response.addCookie(rememberMeCookie);
            }

            return "redirect:/profile/" + loggedInUser.getUsername();

        } catch (RuntimeException e) {
            model.addAttribute("loginError", e.getMessage());
            return "login";
        }
    }

    // ================= LUỒNG ĐĂNG XUẤT (LOGOUT) =================

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {

        // 1. AN TOÀN: Lấy session hiện tại, nếu không có (null) thì bỏ qua, không bị lỗi crash code
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); // Phá hủy session sạch sẽ trên RAM Server
        }

        // 2. XÓA COOKIE 1: Xóa cookie ghi nhớ tùy chỉnh của bạn
        Cookie customCookie = new Cookie("remember_me_user", null);
        customCookie.setMaxAge(0);
        customCookie.setPath("/");
        response.addCookie(customCookie);

        // 3. XÓA COOKIE 2: Xóa nốt Cookie định danh cốt lõi của hệ thống Java Web để tránh lưu rác phiên
        Cookie jsessionCookie = new Cookie("JSESSIONID", null);
        jsessionCookie.setMaxAge(0);
        jsessionCookie.setPath(request.getContextPath() + "/"); // Đảm bảo khớp chính xác Path hệ thống cấp phát
        response.addCookie(jsessionCookie);

        // Chuyển hướng sạch sẽ về trang login kèm tham số báo trạng thái đã logout (để UI nếu muốn có thể hiện thông báo)
        return "redirect:/login"; // Trả về đường dẫn thuần, không bồi thêm đuôi query phía sau
    }

    // ================= LUỒNG ĐĂNG KÝ (REGISTER) =================

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("registerDTO") RegisterDTO registerDTO,
                                 BindingResult bindingResult,
                                 Model model) {

        if (bindingResult.hasErrors()) {
            return "register";
        }

        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.registerDTO", "Mật khẩu xác nhận không trùng khớp");
            return "register";
        }

        try {
            userService.registerNewUser(registerDTO);
            return "redirect:/login?registered=true";
        } catch (RuntimeException e) {
            model.addAttribute("registerError", e.getMessage());
            return "register";
        }
    }

    // ================= LUỒNG XÁC THỰC EMAIL (VERIFY) =================

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam("token") String token, Model model) {
        boolean isVerified = userService.verifyEmailToken(token);
        model.addAttribute("isVerified", isVerified);
        return "verify-result";
    }

    // ================= LUỒNG QUÊN MẬT KHẨU (FORGOT PASSWORD) =================

    // Hiển thị trang nhập Email để yêu cầu cấp lại mật khẩu
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    // Xử lý gửi email chứa link đặt lại mật khẩu
    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email, Model model) {
        try {
            userService.sendForgotPasswordEmail(email);
            model.addAttribute("successMessage", "Hệ thống đã gửi liên kết đặt lại mật khẩu! Vui lòng kiểm tra email.");
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/forgot-password";
    }

    // Tiếp nhận lượt bấm từ email khôi phục mật khẩu gửi về
    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam("token") String token, Model model) {
        boolean isValid = userService.validateForgotPasswordToken(token);

        if (!isValid) {
            // Nếu token hết hạn hoặc không tồn tại, tận dụng trang verify-result để báo lỗi thất bại
            model.addAttribute("isVerified", false);
            return "verify-result";
        }

        // Nếu hợp lệ, truyền token sang trang reset-password qua model để chuẩn bị gửi kèm Form POST
        model.addAttribute("token", token);
        return "reset-password";
    }

    // Xử lý form đặt lại mật khẩu mới gửi lên
    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam("token") String token,
                                      @RequestParam("password") String password,
                                      @RequestParam("confirmPassword") String confirmPassword,
                                      Model model) {
        // Kiểm tra hai ô mật khẩu có khớp nhau không
        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Mật khẩu xác nhận không khớp!");
            model.addAttribute("token", token);
            return "reset-password";
        }

        try {
            userService.updatePassword(token, password);
            // Đổi thành công, đá về login kèm cờ báo hiệu đổi pass thành công hiển thị lên giao diện nếu cần
            return "redirect:/login?passwordChanged=true";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("token", token);
            return "reset-password";
        }
    }
}