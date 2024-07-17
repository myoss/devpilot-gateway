package com.zhongan.devpilot.gateway.completions.providers.dto;

import java.util.List;

import lombok.Data;

@Data
public class DevPilotChatCompletionRequest {
    /**
     * 模型名称
     */
    private String model;

    /**
     * 消息列表
     */
    private List<DevPilotMessage> messages;

    /**
     * 是否是流式的
     */
    private Boolean stream = Boolean.FALSE;
}
