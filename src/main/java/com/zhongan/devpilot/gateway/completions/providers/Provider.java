package com.zhongan.devpilot.gateway.completions.providers;

import com.zhongan.devpilot.gateway.completions.context.DocumentContext;
import reactor.core.publisher.Mono;

/**
 * 代码补全提供者
 *
 * @author Jerry.Chen
 */
public interface Provider {
    /**
     * 生成补全文本
     *
     * @param documentContext 文档上下文
     * @return 补全文本
     */
    Mono<String> generateCompletions(DocumentContext documentContext);
}
