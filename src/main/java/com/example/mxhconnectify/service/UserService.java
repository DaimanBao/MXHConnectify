package com.example.mxhconnectify.service;

import com.example.mxhconnectify.dao.UserDAO;
import com.example.mxhconnectify.dto.LoginDTO;
import com.example.mxhconnectify.dto.RegisterDTO;
import com.example.mxhconnectify.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserDAO userDAO;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Autowired // Inject đầy đủ các bean cần thiết để xử lý nghiệp vụ
    public UserService(UserDAO userDAO, BCryptPasswordEncoder passwordEncoder, EmailService emailService) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Xử lý đăng ký tài khoản mới: Check trùng, băm mật khẩu, tạo token và bắn mail ngầm
     */
    @Transactional
    public void registerNewUser(RegisterDTO registerDTO) {
        if (userDAO.existsByUsername(registerDTO.getUsername())) {
            throw new RuntimeException("Tên tài khoản này đã tồn tại!");
        }
        if (userDAO.existsByEmail(registerDTO.getEmail())) {
            throw new RuntimeException("Email này đã được sử dụng!");
        }

        String encryptedPassword = passwordEncoder.encode(registerDTO.getPassword());

        String token = UUID.randomUUID().toString();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(15);

        User user = User.builder()
                .username(registerDTO.getUsername())
                .email(registerDTO.getEmail())
                .password(encryptedPassword)
                .bio(registerDTO.getBio())
                .isActive(false) // Mặc định khóa, chờ kích hoạt email
                .emailToken(token)
                .emailTokenExpiry(expiryTime)
                .build();

        userDAO.save(user);

        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), token);
    }

    /**
     * Xử lý nghiệp vụ Đăng nhập: Cho phép nhập cả Username hoặc Email
     */
    @Transactional(readOnly = true)
    public User authenticateUser(LoginDTO loginDTO) {
        User user = userDAO.findByUsernameOrEmail(loginDTO.getUsernameOrEmail(), loginDTO.getUsernameOrEmail())
                .orElseThrow(() -> new RuntimeException("Tên tài khoản hoặc Email không tồn tại!"));

        if (!user.isActive()) {
            throw new RuntimeException("Tài khoản của bạn chưa được kích hoạt. Vui lòng kiểm tra Email!");
        }

        boolean isPasswordMatch = passwordEncoder.matches(loginDTO.getPassword(), user.getPassword());

        if (!isPasswordMatch) {
            throw new RuntimeException("Mật khẩu không chính xác!");
        }

        return user;
    }

    /**
     * Xử lý xác thực Token khi người dùng click vào link từ Email gửi về
     */
    @Transactional
    public boolean verifyEmailToken(String token) {
        Optional<User> userOpt = userDAO.findByEmailToken(token);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        if (user.getEmailTokenExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        user.setActive(true);
        user.setEmailToken(null);
        user.setEmailTokenExpiry(null);

        userDAO.save(user);
        return true;
    }

    // ===================================================================
    // THÊM MỚI: CÁC TÍNH NĂNG PHỤC VỤ LUỒNG QUÊN MẬT KHẨU (FORGOT PASSWORD)
    // ===================================================================

    /**
     * Bước 1: Tiếp nhận yêu cầu quên mật khẩu, sinh token và gửi mail đặt lại mật khẩu ngầm
     */
    @Transactional
    public void sendForgotPasswordEmail(String email) {
        // Tìm user theo email thông qua UserDAO
        User user = userDAO.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Địa chỉ Email này chưa được đăng ký!"));

        // Sinh mã token đặt lại mật khẩu và cấu hình thời gian hết hạn (15 phút)
        String token = UUID.randomUUID().toString();
        user.setForgotPasswordToken(token);
        user.setForgotPasswordTokenExpiry(LocalDateTime.now().plusMinutes(15));

        userDAO.save(user);

        // Gọi sang EmailService để gửi đường dẫn thay đổi mật khẩu (Hãy chắc chắn bạn đã tạo hàm này trong EmailService)
        emailService.sendResetPasswordEmail(user.getEmail(), user.getUsername(), token);
    }

    /**
     * Bước 2: Kiểm tra xem token khôi phục mật khẩu mà người dùng nhấn vào có hợp lệ hay không
     */
    @Transactional(readOnly = true)
    public boolean validateForgotPasswordToken(String token) {
        Optional<User> userOpt = userDAO.findByForgotPasswordToken(token);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        // Trả về true nếu thời gian hết hạn nằm sau thời gian hiện tại (Token vẫn còn hạn dùng)
        return !user.getForgotPasswordTokenExpiry().isBefore(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userDAO.findByUsername(username);
    }
    /**
     * Bước 3: Người dùng nhập mật khẩu mới hợp lệ, tiến hành băm mật khẩu và lưu cập nhật vào DB
     */
    @Transactional
    public void updatePassword(String token, String newPassword) {
        User user = userDAO.findByForgotPasswordToken(token)
                .orElseThrow(() -> new RuntimeException("Mã xác thực không hợp lệ hoặc đã bị thay đổi!"));

        if (user.getForgotPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã xác thực đổi mật khẩu đã hết hạn!");
        }

        // Băm mật khẩu mới bằng BCrypt trước khi ghi đè vào DB
        user.setPassword(passwordEncoder.encode(newPassword));

        // Dọn dẹp sạch sẽ dữ liệu Token cũ để tránh việc đường link bị tái sử dụng bừa bãi
        user.setForgotPasswordToken(null);
        user.setForgotPasswordTokenExpiry(null);

        userDAO.save(user);
    }
}