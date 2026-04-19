package com.vocabmaster.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vocabmaster.user.entity.User;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {

    private String uuid;
    private String nickname;
    private String email;
    private String phoneMasked;
    private String avatarUrl;
    private String timezone;
    private String locale;
    private List<String> boundProviders;

    public static UserDto from(User user) {
        return UserDto.builder()
                .uuid(user.getUuid())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .timezone(user.getTimezone())
                .locale(user.getLocale())
                .build();
    }

    /** 手机号脱敏：138****0000 */
    public static String maskPhone(String plainPhone) {
        if (plainPhone == null || plainPhone.length() < 7) return plainPhone;
        return plainPhone.substring(0, 3) + "****" + plainPhone.substring(plainPhone.length() - 4);
    }
}
