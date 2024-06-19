package com.zhongan.devpilot.gateway.completions.providers.impl;

import java.util.List;
import java.util.Map;

import com.zhongan.devpilot.gateway.completions.context.DocumentContext;
import com.zhongan.devpilot.gateway.completions.providers.Provider;
import com.zhongan.devpilot.gateway.completions.providers.dto.CodeMessage;
import com.zhongan.devpilot.gateway.completions.providers.dto.CodePrefixComponents;
import com.zhongan.devpilot.gateway.completions.providers.dto.OpenAiMessageResponse;
import com.zhongan.devpilot.gateway.config.AppCommonProperties;
import com.zhongan.devpilot.gateway.utils.JacksonMapper;
import com.zhongan.devpilot.gateway.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import org.springframework.ai.autoconfigure.openai.OpenAiChatProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * A provider implementation that uses the OpenAI API to generate code completions.
 *
 * @author Jerry.Chen
 */
@Slf4j
@Service
public class OpenAiProvider implements Provider {
    private static final String EMPTY_RESPONSE = "{\"id\":\"-1\",\"role\":\"\",\"content\":\"\"}";
    private final WebClient webClient;
    private final JacksonMapper jacksonMapper;
    private final OpenAiChatProperties openAiChatProperties;
    private final AppCommonProperties appCommonProperties;

    public OpenAiProvider(WebClient openAiWebClient, OpenAiChatProperties openAiChatProperties, AppCommonProperties appCommonProperties) {
        this.webClient = openAiWebClient;
        this.jacksonMapper = JacksonMapper.nonDefaultMapper();
        this.openAiChatProperties = openAiChatProperties;
        this.appCommonProperties = appCommonProperties;
    }

    @Override
    public Mono<String> generateCompletions(DocumentContext documentContext) {
        String prefix = documentContext.getPrefix();
        CodePrefixComponents headAndTail = CodePrefixComponents.getHeadAndTail(prefix);
        CodePrefixComponents.CodeTrimmed head = headAndTail.getHead();
        CodePrefixComponents.CodeTrimmed tail = headAndTail.getTail();

        // 填充块: 我们希望模型完成的代码
        String infillBlock = tail.getTrimmed().endsWith("\n") ? StringUtils.stripToEmpty(tail.getTrimmed()) : tail.getTrimmed();
        // code before the cursor, without the code extracted for the infillBlock
        String infillPrefix = head.getRaw();
        // code after the cursor
        String infillSuffix = documentContext.getSuffix();

        String infillPrefixEncode = StringUtil.removeQuote(jacksonMapper.toJson(infillPrefix));
        String infillSuffixEncode = StringUtil.removeQuote(jacksonMapper.toJson(infillSuffix));
        String infillBlockEncode = StringUtil.removeQuote(jacksonMapper.toJson(infillBlock));

        AppCommonProperties.CodeCompletionConfig openAiCodeCompletion = appCommonProperties.getOpenAiCodeCompletion();
        String template = openAiCodeCompletion.getTemplate();
        String replaced = template.replace("{OPENING_CODE_TAG}", "<FILL_CODE_777>");
        replaced = replaced.replace("{CLOSING_CODE_TAG}", "</FILL_CODE_777>");
        replaced = replaced.replace("{infillPrefix}", infillPrefixEncode);
        replaced = replaced.replace("{infillSuffix}", infillSuffixEncode);
        replaced = replaced.replace("{infillBlock}", infillBlockEncode);
        replaced = replaced.replace("{relativeFilePath}", documentContext.getPath());
        log.info("OpenAiProviderRequest: {}", replaced);
        Integer resultMaxLines = openAiCodeCompletion.getResultMaxLines();
        String completionType = documentContext.getCompletionType();
        Mono<String> result = webClient.post()
                .uri(openAiCodeCompletion.getRequestUrl())
                .headers(httpHeaders -> {
                    Map<String, String> claude3CodeCompletionsRequestHeaders = openAiCodeCompletion.getRequestHeaders();
                    if (claude3CodeCompletionsRequestHeaders != null) {
                        claude3CodeCompletionsRequestHeaders.forEach(httpHeaders::add);
                    }
                })
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(replaced)
                .exchangeToMono(response -> {
                    HttpStatusCode httpStatusCode = response.statusCode();
                    if (httpStatusCode.equals(HttpStatus.OK)) {
                        return response.bodyToMono(String.class).map(callResult -> {
                            log.info("OpenAiProviderResult: {}", callResult);
                            OpenAiMessageResponse messageResponse = jacksonMapper.fromJson(callResult, OpenAiMessageResponse.class);
                            List<OpenAiMessageResponse.Choice> choices = messageResponse.getChoices();
                            if (CollectionUtils.isEmpty(choices)) {
                                return EMPTY_RESPONSE;
                            }
                            return convertResponse(choices, messageResponse, resultMaxLines, completionType);
                        });
                    } else {
                        return response.bodyToMono(String.class).map(s -> {
                            log.error("OpenAiProviderError: {}, {}", httpStatusCode, s);
                            return EMPTY_RESPONSE;
                        });
                    }
                });
        return result;
    }

    /**
     * 将模型返回的结果转换为前端需要的格式
     *
     * @param choices         模型生成的结果
     * @param messageResponse 模型返回的结果
     * @param resultMaxLines  代码补全结果最大行数
     * @param completionType  代码补全类型
     * @return 前端需要的格式
     */
    private String convertResponse(List<OpenAiMessageResponse.Choice> choices,
                                   OpenAiMessageResponse messageResponse,
                                   Integer resultMaxLines, String completionType) {
        OpenAiMessageResponse.Choice choice = choices.get(0);
        OpenAiMessageResponse.MessageContent messageContent = choice.getMessage();
        String completionText = messageContent.getContent();
        completionText = StringUtil.processMarkdownCodeBlock(completionText);
        completionText = StringUtil.removePrefix(completionText, "<FILL_CODE_777>");

        CodeMessage codeMessage = new CodeMessage();
        codeMessage.setId(messageResponse.getId());
        codeMessage.setRole(messageContent.getRole());
        if ("comment".equals(completionType)) {
            // 注释补全场景
            codeMessage.setContent(completionText);
        } else if ("inline".equals(completionType) || resultMaxLines == null || (resultMaxLines >= 0 && resultMaxLines <= 1)) {
            // 单行补全场景: 只返回第一行的代码
            String[] split = completionText.split("\n");
            codeMessage.setContent(split.length > 0 ? split[0] : completionText);
        } else if (resultMaxLines == -1) {
            // 其它场景: 返回多行代码
            codeMessage.setContent(completionText);
        } else {
            // 其它场景: 返回指定行数的代码
            String[] split = completionText.split("\n");
            int min = Math.min(resultMaxLines, split.length);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < min; i++) {
                sb.append(split[i]).append("\n");
            }
            codeMessage.setContent(sb.toString());
        }
        return jacksonMapper.toJson(codeMessage);
    }
}
