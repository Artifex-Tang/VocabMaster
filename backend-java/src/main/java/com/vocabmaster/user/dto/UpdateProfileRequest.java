package com.vocabmaster.user.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String nickname;
    private String avatarUrl;
    private String timezone;
    private String locale;
}
