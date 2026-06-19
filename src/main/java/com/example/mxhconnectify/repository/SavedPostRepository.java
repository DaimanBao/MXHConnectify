package com.example.mxhconnectify.repository;

import com.example.mxhconnectify.entity.SavedPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavedPostRepository extends JpaRepository<SavedPost, Long> {

    // Tìm kiếm xem User đã lưu bài viết này chưa
    Optional<SavedPost> findByUser_IdAndPost_Id(Long userId, Long postId);

    // Hủy lưu trực tiếp
    void deleteByUser_IdAndPost_Id(Long userId, Long postId);
}