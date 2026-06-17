package com.example.mxhconnectify.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateDTO {
    private String fullName;
    private String headline;
    private String description;
    private String communityLinks; // Nhận chuỗi dạng "link1,link2" từ thẻ input ẩn của Modal
}