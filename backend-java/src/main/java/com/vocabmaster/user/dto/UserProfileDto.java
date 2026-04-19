package com.vocabmaster.user.dto;

import com.vocabmaster.user.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserProfileDto {

    private String uuid;
    private String nickname;
    private String avatarUrl;
    private String email;
    private String phoneMasked;
    private String timezone;
    private String locale;
    private List<String> boundProviders;
    private LocalDateTime createdAt;

    public static UserProfileDto from(User u, String phoneMasked, List<String> boundProviders) {
        return UserProfileDto.builder()
                .uuid(u.getUuid())
                .nickname(u.getNickname())
                .avatarUrl(u.getAvatarUrl())
                .email(u.getEmail())
                .phoneMasked(phoneMasked)
                .timezone(u.getTimezone())
                .locale(u.getLocale())
                .boundProviders(boundProviders)
                .createdAt(u.getCreatedAt())
                .build();
    }
}
