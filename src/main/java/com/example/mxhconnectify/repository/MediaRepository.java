package com.example.mxhconnectify.repository;

import com.example.mxhconnectify.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaRepository extends JpaRepository<Media,Long> {
    // Lấy toàn bộ media của 1 bài viết
    List<Media> findByPost_Id(Long postId);
}
