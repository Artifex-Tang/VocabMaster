package com.vocabmaster.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardResponse {

    private String date;
    private long totalUsers;
    private long newUsersToday;
    private long dauToday;
    private long totalWords;
    private long totalCheckins;
}
