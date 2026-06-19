package com.example.mxhconnectify.service;

import com.example.mxhconnectify.entity.Media;
import com.example.mxhconnectify.entity.Post;
import com.example.mxhconnectify.entity.User;
import com.example.mxhconnectify.enums.PostStatus;
import com.example.mxhconnectify.enums.PostType;
import com.example.mxhconnectify.repository.MediaRepository;
import com.example.mxhconnectify.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class CommentService {

    private final PostRepository postRepository;
    private final MediaRepository mediaRepository;

    @Autowired
    public CommentService(PostRepository postRepository, MediaRepository mediaRepository) {
        this.postRepository = postRepository;
        this.mediaRepository = mediaRepository;
    }

    /**
     * 1. Lấy tất cả các bình luận cấp 1 (COMMENT) của một bài viết chính.
     */
    @Transactional(readOnly = true)
    public List<Post> getCommentsByPostId(Long postId) {
        return postRepository.findByParentIdAndTypeAndStatusOrderByCreatedAtAsc(
                postId, PostType.COMMENT, PostStatus.ENABLE
        );
    }

    /**
     * 2. Lấy danh sách phản hồi (REPLY) của một bình luận gốc theo cơ chế phân trang (Slice).
     */
    @Transactional(readOnly = true)
    public Slice<Post> getRepliesByCommentId(Long commentId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findByParentIdAndTypeAndStatusOrderByCreatedAtAsc(
                commentId, PostType.REPLY, PostStatus.ENABLE, pageable
        );
    }

    /**
     * 3. Xử lý tạo mới một Bình luận (COMMENT) hoặc một Phản hồi (REPLY).
     */
    @Transactional
    public Post createCommentOrReply(User currentUser, Long postId, Long parentId, String content, MultipartFile file) throws IOException {
        // Kiểm tra xem bài viết chính (POST) có thực sự tồn tại hay không
        Post rootPost = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Bài viết gốc không tồn tại hoặc đã bị xóa"));

        // Khởi tạo đối tượng Post mới đóng vai trò là Comment hoặc Reply
        Post interaction = new Post();
        interaction.setUser(currentUser);
        interaction.setContent(content);
        interaction.setStatus(PostStatus.ENABLE);
        interaction.setLikeCount(0);
        interaction.setCommentCount(0);

        if (parentId == null) {
            // Trường hợp A: Đây là bình luận cấp 1 trực tiếp vào bài viết
            interaction.setType(PostType.COMMENT);
            interaction.setParentId(rootPost.getId()); // trỏ về ID bài viết gốc

            // LƯU Ý SỬA: ĐỒNG BỘ CHỈ SỐ: Tăng tổng số lượng commentCount của bài viết chính (POST) lên 1
            int currentCommentCount = rootPost.getCommentCount() != null ? rootPost.getCommentCount() : 0;
            rootPost.setCommentCount(currentCommentCount + 1);
            postRepository.save(rootPost);
        } else {
            // Trường hợp B: Đây là câu trả lời (Reply)
            // Kiểm tra xem bình luận cha có tồn tại không
            Post parentComment = postRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Bình luận gốc cần phản hồi không tồn tại"));

            // ĐÃ SỬA: Ép kiểu dữ liệu là REPLY để khớp với câu Query phân trang
            interaction.setType(PostType.REPLY);
            // Theo logic UI thẳng hàng đã chốt: parentId luôn trỏ về ID của COMMENT gốc (bậc 1) quản lý cụm đó
            interaction.setParentId(parentComment.getId());

            // ĐÃ SỬA CHÍNH XÁC: Tăng tổng số lượng reply (commentCount) của chính BÌNH LUẬN CHA lên 1
            int parentCommentCount = parentComment.getCommentCount() != null ? parentComment.getCommentCount() : 0;
            parentComment.setCommentCount(parentCommentCount + 1);
            postRepository.save(parentComment); // Lưu lại bình luận cha để cập nhật số lượng câu trả lời
        }

        // Lưu bản ghi Comment/Reply vào Database
        Post savedInteraction = postRepository.save(interaction);

        // Xử lý đính kèm đa phương tiện (Giới hạn tối đa 1 file ảnh IMAGE)
        if (file != null && !file.isEmpty()) {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image")) {
                throw new IllegalArgumentException("Chỉ cho phép đính kèm tệp tin hình ảnh khi bình luận!");
            }

            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path uploadDirectory = Paths.get("E:/MXHConnectify/post_uploads/");

            if (!Files.exists(uploadDirectory)) {
                Files.createDirectories(uploadDirectory);
            }

            Path copyLocation = uploadDirectory.resolve(fileName);
            Files.copy(file.getInputStream(), copyLocation, StandardCopyOption.REPLACE_EXISTING);

            Media media = new Media();
            media.setPost(savedInteraction);
            media.setUrl("/post_uploads/" + fileName);
            media.setType("IMAGE");
            mediaRepository.save(media);

            savedInteraction.getMediaList().add(media);
        }

        return savedInteraction;
    }
}