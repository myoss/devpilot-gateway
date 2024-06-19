package com.zhongan.devpilot.gateway.completions.providers.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * OpenAi 消息接口返回体
 * <p>
 * <a href="https://github.com/Azure/azure-rest-api-specs/blob/main/specification/cognitiveservices/data-plane/AzureOpenAI/inference/stable/2023-05-15/inference.json">AzureOpenAI inference</a>
 *
 * @author Jerry.Chen
 */
@Data
public class OpenAiMessageResponse {
    private String id;
    private String object;
    private Long created;
    private String model;

    private List<Choice> choices;
    private MessageUsage usage;

    @Data
    public static class Choice {
        private MessageContent message;

        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    public static class MessageContent {
        private String role;
        private String content;
    }

    @Data
    public static class MessageUsage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }
}
