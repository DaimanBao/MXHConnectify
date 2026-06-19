package com.example.mxhconnectify.service;


import com.example.mxhconnectify.dto.PostDTO;
import com.example.mxhconnectify.entity.Media;
import com.example.mxhconnectify.entity.Post;
import com.example.mxhconnectify.entity.User;
import com.example.mxhconnectify.enums.PostStatus;
import com.example.mxhconnectify.enums.PostType;
import com.example.mxhconnectify.repository.MediaRepository;
import com.example.mxhconnectify.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final MediaRepository mediaRepository;

    @Autowired
    public PostService(PostRepository postRepository, MediaRepository mediaRepository) {
        this.postRepository = postRepository;
        this.mediaRepository = mediaRepository;
    }

    private final String POST_UPLOAD_DIR = "E:/MXHConnectify/post_uploads/";


    @Transactional
    public void createPost(User user, PostDTO postDTO) throws IOException {
        // Kiểm tra và tạo thư mục nếu chưa tồn tại
        Path uploadPath = Paths.get(POST_UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 1. Tạo và lưu Post
        Post post = new Post();
        post.setUser(user);
        post.setContent(postDTO.getContent());
        post.setType(PostType.POST);
        post.setStatus(PostStatus.ENABLE);

        post = postRepository.save(post);

        // 2. Xử lý lưu Media
        if (postDTO.getFiles() != null && !postDTO.getFiles().isEmpty()) {
            for (MultipartFile file : postDTO.getFiles()) {
                if (!file.isEmpty()) {
                    String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                    Path copyLocation = uploadPath.resolve(fileName); // Dùng resolve an toàn hơn
                    Files.copy(file.getInputStream(), copyLocation, StandardCopyOption.REPLACE_EXISTING);

                    // 3. Lưu thông tin Media vào DB
                    Media media = new Media();
                    media.setPost(post);
                    media.setUrl("/post_uploads/" + fileName); // Đường dẫn tương ứng mới
                    media.setType(file.getContentType().startsWith("image") ? "IMAGE" : "VIDEO");
                    mediaRepository.save(media);
                }
            }
        }
    }


    public Page<Post> getHomeFeed(User currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> homeFeed = postRepository.findHomeFeed(currentUser.getId(), pageable);

        // VỚI MỖI BÀI VIẾT TRÊN NEWFEED: Quét tìm bình luận tiêu biểu có nhiều like nhất
        attachFeaturedComments(homeFeed);

        return homeFeed;
    }

    /**
     * Lấy danh sách bài viết cho trang KHÁM PHÁ: chỉ lấy post của những người
     * mà currentUser CHƯA theo dõi (và không phải chính mình).
     */
    public Page<Post> getExploreFeed(User currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> exploreFeed = postRepository.findExploreFeed(currentUser.getId(), pageable);

        // Tương tự Home Feed: gắn bình luận tiêu biểu (nhiều like nhất) cho mỗi bài viết
        attachFeaturedComments(exploreFeed);

        return exploreFeed;
    }

    /**
     * Hàm bổ trợ: quét và gắn bình luận có nhiều like nhất cho từng bài viết trong 1 trang kết quả.
     */
    private void attachFeaturedComments(Page<Post> postPage) {
        if (postPage != null && postPage.hasContent()) {
            postPage.getContent().forEach(post -> {
                postRepository.findTopFeaturedCommentByPostId(post.getId())
                        .ifPresent(comment -> post.setFeaturedComment(comment));
            });
        }
    }

    public Optional<Post> findById(Long id) {
        return postRepository.findById(id);
    }

    public long getPostCountByUserId(Long userId) {
        if (userId == null) return 0;

        return postRepository.countByUser_IdAndParentIdIsNullAndStatus(userId, PostStatus.ENABLE);
    }

    public List<Post> getUserPosts(Long userId) {
        if (userId == null) {
            return List.of(); // Trả về danh sách rỗng nếu userId truyền vào bị rỗng
        }

        // Gọi xuống Repository để lấy danh sách bài viết đang hoạt động (ENABLE)
        return postRepository.findByUser_IdAndParentIdIsNullAndStatusOrderByCreatedAtDesc(userId, PostStatus.ENABLE);
    }
}