package com.zhongan.devpilot.gateway.config;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "llm-prompt")
public class LlmPromptProperties {

    private Map<String, PromptConfig> explainCommand;


    @Getter
    @Setter
    public static class PromptConfig {

        private String model;

        private String promptTemplate;

    }

}
