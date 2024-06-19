package com.zhongan.devpilot.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.zhongan.devpilot.gateway.completions.context.DocumentContext;
import com.zhongan.devpilot.gateway.completions.providers.Provider;
import com.zhongan.devpilot.gateway.utils.JacksonMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import static com.zhongan.devpilot.gateway.constant.Constants.REQUEST_BODY;

/**
 * 代码补全过滤器
 *
 * @author Jerry.Chen
 */
@Slf4j
@Component
public class CodeCompletionGatewayFilterFactory extends AbstractGatewayFilterFactory<CodeCompletionGatewayFilterFactory.Config> implements Ordered {
    private final JacksonMapper jacksonMapper;
    private final Map<String, Provider> providerMap;

    public CodeCompletionGatewayFilterFactory(Map<String, Provider> providerMap) {
        super(CodeCompletionGatewayFilterFactory.Config.class);
        this.jacksonMapper = JacksonMapper.nonDefaultMapper();
        this.providerMap = providerMap;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            HttpHeaders headers = request.getHeaders();
            MediaType contentType = headers.getContentType();

            // 检查请求的Content-Type是否为JSON类型
            if (contentType != null && MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
                // 获取请求的body
                String requestBody = exchange.getAttribute(REQUEST_BODY);
                log.info("requestBody: {}", requestBody);
                LinkedHashMap<String, Object> jsonObject = jacksonMapper.fromJson(requestBody, JacksonMapper.LINKED_HASH_MAP_S2O_TYPE_REFERENCE);
                String document = (String) jsonObject.get("document");
                Integer position = Integer.parseInt((String) jsonObject.get("position"));
                String filePath = (String) jsonObject.get("filePath");
                String completionType = (String) jsonObject.get("completionType");
                Provider provider = providerMap.get(config.getDefaultProviderName());
                DocumentContext documentContext = DocumentContext.build(document, filePath, position, completionType);
                Mono<String> completions = provider.generateCompletions(documentContext);
                ServerHttpResponse response = exchange.getResponse();
                // 将 completions 写入到 response 中
                return completions.flatMap(completion -> {
                    log.info("filterCompletion: {}", completion);
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    DataBufferFactory bufferFactory = response.bufferFactory();
                    DataBuffer dataBuffer = bufferFactory.wrap(completion.getBytes(StandardCharsets.UTF_8));
                    return response.writeWith(Flux.just(dataBuffer));
                });
            }

            // 如果Content-Type不是JSON类型，则直接继续处理请求
            return chain.filter(exchange);
        };
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Data
    public static class Config {
        /**
         * 默认的代码补全提供者名称
         */
        private String defaultProviderName;
    }
}
