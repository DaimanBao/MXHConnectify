package com.example.mxhconnectify.service;

import com.example.mxhconnectify.entity.Post;
import com.example.mxhconnectify.entity.SavedPost;
import com.example.mxhconnectify.entity.User;
import com.example.mxhconnectify.repository.PostRepository;
import com.example.mxhconnectify.repository.SavedPostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SavedPostService {

    private final SavedPostRepository savedPostRepository;
    private final PostRepository postRepository;

    @Autowired
    public SavedPostService(SavedPostRepository savedPostRepository, PostRepository postRepository) {
        this.savedPostRepository = savedPostRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public Map<String, Object> toggleSavePost(Long postId, User currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Bài viết không tồn tại"));

        // Quy tắc: Không cho phép tự lưu bài viết của chính mình
        if (post.getUser().getId().equals(currentUser.getId())) {
            return Map.of("success", false, "message", "Bạn không thể lưu bài viết của chính mình!");
        }

        Optional<SavedPost> existingSave = savedPostRepository.findByUser_IdAndPost_Id(currentUser.getId(), post.getId());
        boolean saved;

        if (existingSave.isPresent()) {
            savedPostRepository.deleteByUser_IdAndPost_Id(currentUser.getId(), post.getId());
            saved = false;
        } else {
            SavedPost savedPost = new SavedPost();
            savedPost.setUser(currentUser);
            savedPost.setPost(post);
            savedPostRepository.save(savedPost);
            saved = true;
        }

        return Map.of(
                "success", true,
                "saved", saved
        );
    }

    // Hàm quét danh sách bài viết ngoài trang chủ để gán trạng thái hiển thị icon
    @Transactional(readOnly = true)
    public void setSaveStatusForPosts(List<Post> posts, User currentUser) {
        if (posts == null || currentUser == null) return;

        posts.forEach(post -> {
            boolean isSaved = savedPostRepository.findByUser_IdAndPost_Id(currentUser.getId(), post.getId()).isPresent();
            post.setSavedByCurrentUser(isSaved);
        });
    }
}
