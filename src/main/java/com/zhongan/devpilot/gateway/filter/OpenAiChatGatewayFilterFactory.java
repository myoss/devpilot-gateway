package com.zhongan.devpilot.gateway.filter;

import java.util.LinkedHashMap;

import com.zhongan.devpilot.gateway.utils.JacksonMapper;
import reactor.core.publisher.Flux;

import org.springframework.ai.autoconfigure.openai.OpenAiChatProperties;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import static com.zhongan.devpilot.gateway.constant.Constants.REQUEST_BODY;

/**
 * OpenAi Chat Gateway Filter Factory
 *
 * @author Jerry.Chen
 */
@Component
public class OpenAiChatGatewayFilterFactory extends AbstractGatewayFilterFactory<OpenAiChatGatewayFilterFactory.Config> {
    private final WebClient webClient;
    private final JacksonMapper jacksonMapper;
    private final OpenAiChatProperties openAiChatProperties;

    public OpenAiChatGatewayFilterFactory(WebClient openAiWebClient, OpenAiChatProperties openAiChatProperties) {
        super(Config.class);
        this.webClient = openAiWebClient;
        this.jacksonMapper = JacksonMapper.nonDefaultMapper();
        this.openAiChatProperties = openAiChatProperties;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpResponse response = exchange.getResponse();
            String requestBody = exchange.getAttribute(REQUEST_BODY);
            LinkedHashMap<String, Object> chatCompletionRequest = jacksonMapper.fromJson(requestBody, JacksonMapper.LINKED_HASH_MAP_S2O_TYPE_REFERENCE);
            OpenAiChatOptions aiChatOptions = openAiChatProperties.getOptions();
            chatCompletionRequest.put("model", aiChatOptions.getModel());
            if (aiChatOptions.getMaxTokens() != null) {
                chatCompletionRequest.put("max_tokens", aiChatOptions.getMaxTokens());
            }
            if (aiChatOptions.getTemperature() != null) {
                chatCompletionRequest.put("temperature", aiChatOptions.getTemperature());
            }
            if (aiChatOptions.getTopP() != null) {
                chatCompletionRequest.put("top_p", aiChatOptions.getTopP());
            }
            String requestJson = jacksonMapper.toJson(chatCompletionRequest);

            // 转发请求到OpenAI并处理响应
            Flux<DataBuffer> dataBufferFlux = webClient.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(requestJson)
                    .retrieve()
                    .bodyToFlux(DataBuffer.class);

            Boolean stream = (Boolean) chatCompletionRequest.get("stream");
            if (stream != null && stream) {
                // 设置响应的Content-Type
                response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE);
            }
            // 直接使用writeWith方法将Flux<DataBuffer>写入响应
            return response.writeWith(dataBufferFlux);
        };
    }

    public static class Config {
        // 这里可以添加你的自定义配置参数
    }
}
