package com.zhongan.devpilot.gateway.filter;

import java.nio.charset.StandardCharsets;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;

import static com.zhongan.devpilot.gateway.constant.Constants.REQUEST_BODY;

/**
 * Records the request event details and extracts the request body for non-GET, DELETE, OPTIONS, and HEAD requests.
 *
 * @author Jerry.Chen
 */
@Component
@Slf4j
public class RequestEventRecordGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest httpRequest = exchange.getRequest();
        HttpMethod method = httpRequest.getMethod();
        log.info("requestURI: {}, method: {}", httpRequest.getURI(), method);
        if (method == HttpMethod.GET || method == HttpMethod.DELETE || method == HttpMethod.OPTIONS || method == HttpMethod.HEAD) {
            return chain.filter(exchange);
        }

        ServerWebExchangeDecorator exchangeDecorator = new ServerWebExchangeDecorator(exchange) {
            private final ServerHttpRequest request = exchange.getRequest();

            @Override
            public ServerHttpRequest getRequest() {
                return request;
            }
        };

        return DataBufferUtils.join(exchangeDecorator.getRequest().getBody()).flatMap(dataBuffer -> {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            String body = new String(bytes, StandardCharsets.UTF_8);
            exchangeDecorator.getAttributes().put(REQUEST_BODY, body);
            return chain.filter(exchangeDecorator.mutate().request(exchangeDecorator.getRequest()).build());
        });
    }
}