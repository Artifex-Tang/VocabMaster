package com.vocabmaster.admin.dto;

import com.vocabmaster.user.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserDto {

    private Long id;
    private String uuid;
    private String nickname;
    private String email;
    private String role;
    private int status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    public static AdminUserDto from(User u) {
        return AdminUserDto.builder()
                .id(u.getId())
                .uuid(u.getUuid())
                .nickname(u.getNickname())
                .email(u.getEmail())
                .role(u.getRole())
                .status(u.getStatus())
                .lastLoginAt(u.getLastLoginAt())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
