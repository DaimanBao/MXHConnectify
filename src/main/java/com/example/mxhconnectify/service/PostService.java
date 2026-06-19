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
        return postRepository.findHomeFeed(currentUser.getId(), pageable);
    }
}
