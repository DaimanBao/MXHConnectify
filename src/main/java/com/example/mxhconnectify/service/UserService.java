package com.example.mxhconnectify.service;

import com.example.mxhconnectify.dto.LoginDTO;
import com.example.mxhconnectify.dto.RegisterDTO;
import com.example.mxhconnectify.dto.SearchUserDTO;
import com.example.mxhconnectify.entity.User;
import com.example.mxhconnectify.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final FollowService followService;

    @Autowired
    public UserService(UserRepository userRepository
            , BCryptPasswordEncoder passwordEncoder
            , EmailService emailService
            , FollowService followService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.followService = followService;
    }

    /**
     * Xử lý đăng ký tài khoản mới: Check trùng, băm mật khẩu, tạo token và bắn mail ngầm
     */

    @Transactional
    public void registerNewUser(RegisterDTO registerDTO) {
        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            throw new RuntimeException("Tên tài khoản này đã tồn tại!");
        }
        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            throw new RuntimeException("Email này đã được sử dụng!");
        }

        String encryptedPassword = passwordEncoder.encode(registerDTO.getPassword());

        String token = UUID.randomUUID().toString();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(15);

        User user = User.builder()
                .username(registerDTO.getUsername())
                .email(registerDTO.getEmail())
                .password(encryptedPassword)
                .fullName(registerDTO.getFullName())
                .isActive(false) // Mặc định khóa, chờ kích hoạt email
                .emailToken(token)
                .emailTokenExpiry(expiryTime)
                .build();

        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), token);
    }
    /**
     * Xử lý nghiệp vụ Đăng nhập: Cho phép nhập cả Username hoặc Email
     */
    @Transactional(readOnly = true)
    public User authenticateUser(LoginDTO loginDTO) {
        User user = userRepository.findByUsernameOrEmail(loginDTO.getUsernameOrEmail(), loginDTO.getUsernameOrEmail())
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
        Optional<User> userOpt = userRepository.findByEmailToken(token);

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

        userRepository.save(user);
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Địa chỉ Email này chưa được đăng ký!"));

        // Sinh mã token đặt lại mật khẩu và cấu hình thời gian hết hạn (15 phút)
        String token = UUID.randomUUID().toString();
        user.setForgotPasswordToken(token);
        user.setForgotPasswordTokenExpiry(LocalDateTime.now().plusMinutes(15));

        userRepository.save(user);

        // Gọi sang EmailService để gửi đường dẫn thay đổi mật khẩu (Hãy chắc chắn bạn đã tạo hàm này trong EmailService)
        emailService.sendResetPasswordEmail(user.getEmail(), user.getFullName(), token);
    }

    /**
     * Bước 2: Kiểm tra xem token khôi phục mật khẩu mà người dùng nhấn vào có hợp lệ hay không
     */
    @Transactional(readOnly = true)
    public boolean validateForgotPasswordToken(String token) {
        Optional<User> userOpt = userRepository.findByForgotPasswordToken(token);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        // Trả về true nếu thời gian hết hạn nằm sau thời gian hiện tại (Token vẫn còn hạn dùng)
        return !user.getForgotPasswordTokenExpiry().isBefore(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    /**
     * Bước 3: Người dùng nhập mật khẩu mới hợp lệ, tiến hành băm mật khẩu và lưu cập nhật vào DB
     */
    @Transactional
    public void updatePassword(String token, String newPassword) {
        User user = userRepository.findByForgotPasswordToken(token)
                .orElseThrow(() -> new RuntimeException("Mã xác thực không hợp lệ hoặc đã bị thay đổi!"));

        if (user.getForgotPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã xác thực đổi mật khẩu đã hết hạn!");
        }

        // Băm mật khẩu mới bằng BCrypt trước khi ghi đè vào DB
        user.setPassword(passwordEncoder.encode(newPassword));

        // Dọn dẹp sạch sẽ dữ liệu Token cũ để tránh việc đường link bị tái sử dụng bừa bãi
        user.setForgotPasswordToken(null);
        user.setForgotPasswordTokenExpiry(null);

        userRepository.save(user);
    }

    @Transactional
    public void updateProfile(User user) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        existingUser.setFullName(user.getFullName());
        existingUser.setHeadline(user.getHeadline());
        existingUser.setDescription(user.getDescription());
        existingUser.setCommunityLinks(user.getCommunityLinks());

        userRepository.save(existingUser);
    }

    public List<SearchUserDTO> searchUsersByKeyword(String keyword, Long currentId, Pageable pageable) {
        String searchKeyword = "%" + keyword + "%";
        return userRepository.searchByKeyword(searchKeyword, currentId, pageable)
                .stream()
                .map(user -> {
                    boolean isFollowing = followService.isFollowing(currentId, user.getId());

                    return SearchUserDTO.builder()
                            .username(user.getUsername())
                            .fullName(user.getFullName())
                            .avatarUrl(user.getAvatarUrl())
                            .isFollowing(followService.isFollowing(currentId, user.getId()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<SearchUserDTO> getRandomUsers(Long currentUserId, Pageable pageable) {
        return userRepository.findRandomUsers(currentUserId, pageable)
                .stream()
                .map(user -> SearchUserDTO.builder()
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .avatarUrl(user.getAvatarUrl())
                        .isFollowing(false) // Luôn là false vì SQL đã lọc bỏ người đã follow
                        .build())
                .collect(Collectors.toList());
    }

    public void updateAvatar(User user, MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new RuntimeException("Vui lòng chọn ảnh!");

        // 1. Nếu user đã có ảnh cũ, xóa nó đi
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            // Giả sử avatarUrl lưu dạng "/uploads/abc.png"
            // Bạn cần tách tên file ra và xóa file vật lý tương ứng
            String oldFileName = user.getAvatarUrl().replace("/avatar_uploads/", "");
            Path oldPath = Paths.get("E:/MXHConnectify/avatar_uploads/" + oldFileName);
            Files.deleteIfExists(oldPath);
        }

        // 2. Lưu file mới như bình thường
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path path = Paths.get("E:/MXHConnectify/avatar_uploads/" + fileName);
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        // 3. Cập nhật DB
        user.setAvatarUrl("/avatar_uploads/" + fileName);
        userRepository.save(user);
    }
}