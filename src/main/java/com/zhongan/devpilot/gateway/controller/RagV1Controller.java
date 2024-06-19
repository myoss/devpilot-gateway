package com.zhongan.devpilot.gateway.controller;

import reactor.core.publisher.Flux;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Retrieves embedding information for a specified Git repository.
 *
 * @author Jerry.Chen
 */
@RequestMapping("/devpilot/v1/rag")
@RestController
public class RagV1Controller {
    /**
     * Retrieves embedding information for a specified Git repository.
     *
     * @param repoName The name of the Git repository for which embedding information is to be retrieved.
     * @return repository embedding status.
     */
    @RequestMapping("/git_repo/embedding_info/{repoName}")
    public Flux<String> getEmbeddingInfo(@PathVariable String repoName) {
        String result = "{\"repoName\": \"%s\", \"embedded\": false}".formatted(repoName);
        return Flux.just(result);
    }
}
