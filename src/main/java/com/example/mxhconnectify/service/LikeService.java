package com.example.mxhconnectify.service;

import com.example.mxhconnectify.entity.Like;
import com.example.mxhconnectify.entity.Post;
import com.example.mxhconnectify.entity.User;
import com.example.mxhconnectify.repository.LikeRepository;
import com.example.mxhconnectify.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LikeService {
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;

    @Autowired
    public LikeService(LikeRepository likeRepository, PostRepository postRepository) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public Map<String, Object> toggleLike(Long postId, User currentUser) {
        // 1. Tìm bài viết mục tiêu
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Bài viết không tồn tại"));

        // 2. Sử dụng findByUser_IdAndPost_Id từ Repository của bạn để kiểm tra trạng thái
        Optional<Like> existingLike = likeRepository.findByUser_IdAndPost_Id(currentUser.getId(), post.getId());

        boolean liked;
        int currentLikeCount = post.getLikeCount() != null ? post.getLikeCount() : 0;

        if (existingLike.isPresent()) {
            // Đã thích -> Sử dụng hàm xóa trực tiếp của bạn
            likeRepository.deleteByUser_IdAndPost_Id(currentUser.getId(), post.getId());
            currentLikeCount = Math.max(0, currentLikeCount - 1);
            liked = false;
        } else {
            // Chưa thích -> Tiến hành tạo thực thể Like mới và lưu
            Like newLike = new Like();
            newLike.setUser(currentUser);
            newLike.setPost(post);
            likeRepository.save(newLike);

            currentLikeCount += 1;
            liked = true;
        }

        // 3. Cập nhật và đồng bộ lại bộ đếm vào bảng bài viết posts
        post.setLikeCount(currentLikeCount);
        postRepository.save(post);

        // 4. Trả về Map thông tin để Controller xuất JSON về Client
        return Map.of(
                "success", true,
                "liked", liked,
                "likeCount", currentLikeCount
        );
    }

    public void setLikeStatusForPosts(List<Post> posts, User currentUser) {
        if (posts == null || currentUser == null) return;

        posts.forEach(post -> {
            // Gọi repo tại đây (Đúng nơi, đúng chỗ)
            boolean isLiked = likeRepository.findByUser_IdAndPost_Id(currentUser.getId(), post.getId()).isPresent();
            post.setLikedByCurrentUser(isLiked);
        });
    }
}
