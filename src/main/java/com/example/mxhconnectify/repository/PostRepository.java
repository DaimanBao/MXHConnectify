package com.example.mxhconnectify.repository;

import com.example.mxhconnectify.entity.Post;
import com.example.mxhconnectify.enums.PostStatus;
import com.example.mxhconnectify.enums.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post,Long> {
    long countByUser_IdAndParentIdIsNullAndStatus(Long userId, PostStatus status);

    // Lấy danh sách bài viết/comment theo parentId (dùng cho feed hoặc load comment)
    Page<Post> findByParentIdAndStatus(Long parentId, PostStatus status, Pageable pageable);

    // Tìm bài viết gốc của user (parentId là null)
    Page<Post> findByUser_IdAndParentIdIsNullAndStatus(Long userId, PostStatus status, Pageable pageable);

    // 1. Cho Home Feed: Lấy post của chính mình HOẶC của những người mình theo dõi
    @Query("SELECT p FROM Post p WHERE p.status = 'ENABLE' AND p.parentId IS NULL " +
            "AND (p.user.id = :currentUserId OR p.user.id IN " +
            "(SELECT f.following.id FROM Follow f WHERE f.follower.id = :currentUserId)) " +
            "ORDER BY p.createdAt DESC")
    Page<Post> findHomeFeed(@Param("currentUserId") Long currentUserId, Pageable pageable);

    // 2. Cho Explore Feed: Lấy post của những người mình KHÔNG theo dõi
    @Query("SELECT p FROM Post p WHERE p.status = 'ENABLE' AND p.parentId IS NULL " +
            "AND p.user.id != :currentUserId AND p.user.id NOT IN " +
            "(SELECT f.following.id FROM Follow f WHERE f.follower.id = :currentUserId) " +
            "ORDER BY p.createdAt DESC")
    Page<Post> findExploreFeed(@Param("currentUserId") Long currentUserId, Pageable pageable);

    List<Post> findByUser_IdAndParentIdIsNullAndStatusOrderByCreatedAtDesc(Long userId, PostStatus status);


    /**
     * 1. Lấy TẤT CẢ các bình luận cấp 1 (COMMENT) thuộc về một bài viết gốc (POST).
     * Sắp xếp theo thời gian tạo: Bình luận cũ ở trên, bình luận mới ở dưới (hoặc ngược lại tùy bạn, thông thường là cũ trước mới sau).
     */
    List<Post> findByParentIdAndTypeAndStatusOrderByCreatedAtAsc(Long parentId, PostType type, PostStatus status);

    /**
     * 2. Lấy danh sách các câu phản hồi (REPLY) thuộc về một bình luận gốc (COMMENT) dưới dạng PHÂN TRANG (Slice).
     * Mỗi lần bấm "Xem thêm", Frontend sẽ truyền số trang lên để lấy cụm tiếp theo.
     */
    Slice<Post> findByParentIdAndTypeAndStatusOrderByCreatedAtAsc(Long parentId, PostType type, PostStatus status, Pageable pageable);
}
