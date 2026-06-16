package com.example.mxhconnectify.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Gửi email xác thực kích hoạt tài khoản mới (Chạy ngầm bất đồng bộ)
     */
    @Async
    public void sendVerificationEmail(String toEmail, String username, String token) {
        String confirmationUrl = baseUrl + "/verify-email?token=" + token;

        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[Instagram Mini] Kích hoạt tài khoản của bạn");

            String htmlContent = "<div style='font-family: sans-serif; max-width: 500px; margin: 0 auto; padding: 20px; border: 1px solid #e1e1e1; border-radius: 4px;'>"
                    + "<h2 style='color: #262626; text-align: center; font-family: serif; font-size: 28px;'>Instagram</h2>"
                    + "<hr style='border: 0; border-top: 1px solid #efefef; margin: 20px 0;'>"
                    + "<p>Chào <strong>" + username + "</strong>,</p>"
                    + "<p>Cảm ơn bạn đã đăng ký tham gia mạng xã hội Instagram Mini. Vui lòng nhấn vào nút bên dưới để xác thực và kích hoạt tài khoản của mình:</p>"
                    + "<div style='text-align: center; margin: 30px 0;'>"
                    + "  <a href='" + confirmationUrl + "' style='background-color: #0095f6; color: white; padding: 10px 24px; text-decoration: none; font-size: 14px; font-weight: 600; border-radius: 4px; display: inline-block;'>Xác thực Email</a>"
                    + "</div>"
                    + "<p style='color: #8e8e8e; font-size: 12px;'>Lưu ý: Đường dẫn này chỉ có hiệu lực trong vòng 15 phút. Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.</p>"
                    + "<hr style='border: 0; border-top: 1px solid #efefef; margin: 20px 0;'>"
                    + "<p style='color: #8e8e8e; font-size: 11px; text-align: center;'>© 2026 Instagram Mini từ Connectify</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            System.err.println("Gặp lỗi khi gửi email xác thực ngầm: " + e.getMessage());
        }
    }

    // ===================================================================
    // THÊM MỚI: LUỒNG GỬI MAIL ĐẶT LẠI MẬT KHẨU (FORGOT PASSWORD)
    // ===================================================================

    /**
     * Gửi email chứa liên kết đặt lại mật khẩu mới (Chạy ngầm bất đồng bộ)
     */
    @Async
    public void sendResetPasswordEmail(String toEmail, String username, String token) {
        // Tự động nối chuỗi dựa trên cấu hình app.base-url trong file cấu hình properties
        String resetUrl = baseUrl + "/reset-password?token=" + token;

        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[Instagram Mini] Đặt lại mật khẩu của bạn");

            String htmlContent = "<div style='font-family: sans-serif; max-width: 500px; margin: 0 auto; padding: 20px; border: 1px solid #e1e1e1; border-radius: 4px;'>"
                    + "<h2 style='color: #262626; text-align: center; font-family: serif; font-size: 28px;'>Instagram</h2>"
                    + "<hr style='border: 0; border-top: 1px solid #efefef; margin: 20px 0;'>"
                    + "<p>Chào <strong>" + username + "</strong>,</p>"
                    + "<p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. Vui lòng nhấn vào nút bên dưới để tiến hành đổi mật khẩu mới:</p>"
                    + "<div style='text-align: center; margin: 30px 0;'>"
                    + "  <a href='" + resetUrl + "' style='background-color: #0095f6; color: white; padding: 10px 24px; text-decoration: none; font-size: 14px; font-weight: 600; border-radius: 4px; display: inline-block;'>Đặt lại mật khẩu</a>"
                    + "</div>"
                    + "<p style='color: #8e8e8e; font-size: 12px;'>Lưu ý: Đường dẫn này chỉ có hiệu lực trong vòng 15 phút. Nếu bạn không đưa ra yêu cầu này, vui lòng bỏ qua email này một cách an toàn.</p>"
                    + "<hr style='border: 0; border-top: 1px solid #efefef; margin: 20px 0;'>"
                    + "<p style='color: #8e8e8e; font-size: 11px; text-align: center;'>© 2026 Instagram Mini từ Connectify</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            System.err.println("Gặp lỗi khi gửi email reset password ngầm: " + e.getMessage());
        }
    }
}