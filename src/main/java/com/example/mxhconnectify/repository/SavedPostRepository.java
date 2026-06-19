package com.example.mxhconnectify.repository;

import com.example.mxhconnectify.entity.Post;
import com.example.mxhconnectify.entity.SavedPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedPostRepository extends JpaRepository<SavedPost, Long> {

    // Tìm kiếm xem User đã lưu bài viết này chưa
    Optional<SavedPost> findByUser_IdAndPost_Id(Long userId, Long postId);

    // Hủy lưu trực tiếp
    void deleteByUser_IdAndPost_Id(Long userId, Long postId);

    @Query("SELECT sp.post FROM SavedPost sp WHERE sp.user.id = :userId ORDER BY sp.savedAt DESC")
    List<Post> findSavedPostsByUserId(@Param("userId") Long userId);
}