package com.example.mxhconnectify.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchUserDTO {
    private String username;
    private String fullName;
    private String avatarUrl;
    private boolean isFollowing;
}
