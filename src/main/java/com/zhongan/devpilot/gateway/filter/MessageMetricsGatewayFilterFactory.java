package com.zhongan.devpilot.gateway.filter;

import java.nio.charset.StandardCharsets;

import reactor.core.publisher.Flux;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import static com.zhongan.devpilot.gateway.constant.Constants.REQUEST_BODY;

/**
 * Message metrics Gateway Filter Factory
 *
 * @author Jerry.Chen
 */
@Component
public class MessageMetricsGatewayFilterFactory extends AbstractGatewayFilterFactory<MessageMetricsGatewayFilterFactory.Config> {

    public MessageMetricsGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpResponse response = exchange.getResponse();
            String requestBody = exchange.getAttribute(REQUEST_BODY);

            // 转发请求到OpenAI并处理响应
            Flux<DataBuffer> dataBufferFlux = Flux.just("{\"success\":true,\"code\":\"200\",\"message\":\"成功\"}").flatMap(s -> {
                byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
                DefaultDataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(bytes);
                return Flux.just(dataBuffer);
            });

            // 直接使用writeWith方法将Flux<DataBuffer>写入响应
            return response.writeWith(dataBufferFlux);
        };
    }

    public static class Config {
        // 这里可以添加你的自定义配置参数
    }
}
