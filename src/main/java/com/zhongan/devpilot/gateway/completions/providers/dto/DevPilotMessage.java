package com.zhongan.devpilot.gateway.completions.providers.dto;

import lombok.Data;

@Data
public class DevPilotMessage {
    /**
     * 角色
     */
    private String role;

    /**
     * 内容
     */
    private String content;
}
