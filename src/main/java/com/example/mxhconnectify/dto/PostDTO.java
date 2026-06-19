package com.example.mxhconnectify.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class PostDTO {
    private String content;
    private List<MultipartFile> files; // Nhận danh sách file từ form
}

