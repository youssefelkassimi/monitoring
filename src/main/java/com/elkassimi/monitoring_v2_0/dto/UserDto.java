package com.elkassimi.monitoring_v2_0.dto;

import lombok.Builder;

@Builder
public record UserDto (
        String  id,
        String fullName,
        String email,
        String password,
        boolean isOnline,
        String role
){}
