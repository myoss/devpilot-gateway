package com.zhongan.devpilot.gateway.config;

import java.util.Map;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 大模型提示词配置
 */
@Data
@ConfigurationProperties(prefix = "llm-prompt")
public class LlmPromptProperties {
    /**
     * 执行命令提示词
     */
    private Map<String, PromptConfig> explainCommand;


    @Data
    public static class PromptConfig {

        private String model;

        private String promptTemplate;
    }

}
