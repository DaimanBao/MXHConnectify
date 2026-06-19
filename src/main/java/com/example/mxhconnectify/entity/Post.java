package com.example.mxhconnectify.entity;

import com.example.mxhconnectify.enums.PostStatus;
import com.example.mxhconnectify.enums.PostType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@Getter
@Setter
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Long parentId; // Nếu là bài post gốc thì null

    @Enumerated(EnumType.STRING)
    private PostType type;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Integer likeCount = 0;
    private Integer commentCount = 0;

    @Enumerated(EnumType.STRING)
    private PostStatus status = PostStatus.ENABLE;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Quan hệ 1-N với Media
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Media> mediaList = new ArrayList<>();
}
