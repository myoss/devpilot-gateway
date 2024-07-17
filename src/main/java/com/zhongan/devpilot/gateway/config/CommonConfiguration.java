package com.zhongan.devpilot.gateway.config;

import org.springframework.ai.autoconfigure.openai.OpenAiConnectionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * App Common Configuration
 *
 * @author Jerry.Chen
 */
@EnableConfigurationProperties({AppCommonProperties.class, LlmPromptProperties.class})
@Configuration
public class CommonConfiguration {
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    /**
     * Creates and configures a WebClient for interacting with the OpenAI API.
     * This function sets the base URL and authentication headers using the provided properties.
     *
     * @param webClientBuilder           The WebClient.Builder used to create the WebClient instance.
     * @param openAiConnectionProperties The properties containing the base URL and API key for OpenAI.
     * @return A configured instance of WebClient for OpenAI API calls.
     */
    @Bean
    public WebClient openAiWebClient(WebClient.Builder webClientBuilder, OpenAiConnectionProperties openAiConnectionProperties) {
        String baseUrl = openAiConnectionProperties.getBaseUrl();
        String apiKey = openAiConnectionProperties.getApiKey();
        return webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> {
                    headers.setBearerAuth(apiKey);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                })
                .build();
    }
}
