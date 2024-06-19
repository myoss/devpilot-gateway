package com.zhongan.devpilot.gateway.config;

import java.util.Map;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app common properties
 *
 * @author Jerry.Chen
 */
@Data
@ConfigurationProperties(prefix = "app-common")
public class AppCommonProperties {

    /**
     * OpenAi模型 代码补全配置信息
     */
    private CodeCompletionConfig openAiCodeCompletion;

    @Data
    public static class CodeCompletionConfig {
        /**
         * Claude3模型 代码补全请求头
         */
        private Map<String, String> requestHeaders;

        /**
         * 模型 代码补全请求url
         */
        private String requestUrl;

        /**
         * 模型 代码补全模板
         */
        private String template;

        /**
         * 代码补全结果最大行数
         */
        private Integer resultMaxLines;
    }
}
