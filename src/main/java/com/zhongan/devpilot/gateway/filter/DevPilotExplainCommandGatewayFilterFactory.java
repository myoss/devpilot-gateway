package com.zhongan.devpilot.gateway.filter;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.zhongan.devpilot.gateway.completions.providers.dto.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.gateway.completions.providers.dto.DevPilotMessage;
import com.zhongan.devpilot.gateway.config.LlmPromptProperties;
import com.zhongan.devpilot.gateway.enums.AnswerLanguage;
import com.zhongan.devpilot.gateway.utils.JacksonMapper;
import com.zhongan.devpilot.gateway.utils.PromptTemplate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import static com.zhongan.devpilot.gateway.constant.Constants.REQUEST_BODY;

/**
 * DevPilot prompts replace && request construct filter
 * @author: maozhen
 */
@Slf4j
@Component
public class DevPilotExplainCommandGatewayFilterFactory extends AbstractGatewayFilterFactory<DevPilotExplainCommandGatewayFilterFactory.Config> implements Ordered {

    private final LlmPromptProperties llmPromptProperties;

    private final JacksonMapper jacksonMapper;

    public DevPilotExplainCommandGatewayFilterFactory(LlmPromptProperties llmPromptProperties) {
        super(Config.class);
        this.llmPromptProperties = llmPromptProperties;
        this.jacksonMapper = JacksonMapper.nonDefaultMapper();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            HttpHeaders headers = request.getHeaders();
            MediaType contentType = headers.getContentType();
            if (contentType != null && MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
                String bodyString = exchange.getAttribute(REQUEST_BODY);
                log.debug("request body: {}", bodyString);
                LinkedHashMap<String, Object> jsonObject = jacksonMapper.fromJson(bodyString, JacksonMapper.LINKED_HASH_MAP_S2O_TYPE_REFERENCE);
                boolean stream = (boolean) jsonObject.get("stream");
                String version = (String) jsonObject.get("version");
                List messages = (List) jsonObject.get("messages");
                if (StringUtils.isBlank(version)) {
                    return chain.filter(exchange);
                }
                Map<String, LlmPromptProperties.PromptConfig> explainCommandPrompt = llmPromptProperties.getExplainCommand();
                List<DevPilotMessage> devPilotMessages = new ArrayList<>();
                String model = "gpt-3.5-turbo";
                for (Object msgObj : messages) {
                    Map msg = (Map) msgObj;
                    String commandType = (String) msg.get("commandType");
                    String role = (String) msg.get("role");
                    String chatMsg = (String) msg.get("content");
                    Map<String, Object> replace = (Map) msg.get("promptData");
                    if (replace != null && !replace.isEmpty()) {
                        if (StringUtils.isBlank(commandType) || explainCommandPrompt.get(commandType.concat("_").concat(version)) == null) {
                            log.error("CommandPrompt is null, commandType: {}", commandType);
                            ServerHttpResponse response = exchange.getResponse();
                            response.setStatusCode(HttpStatus.BAD_REQUEST);
                            return response.writeWith(Flux.just(response.bufferFactory().wrap("undefined prompt".getBytes(StandardCharsets.UTF_8))));
                        }
                        LlmPromptProperties.PromptConfig promptConfig = explainCommandPrompt.get(commandType.concat("_").concat(version));
                        String promptTemplate = promptConfig.getPromptTemplate();
                        String commandPrompt;
                        model = promptConfig.getModel();
                        PromptTemplate pTemplate = PromptTemplate.of(promptTemplate);
                        for (Map.Entry entry : replace.entrySet()) {
                            pTemplate.setVariable(entry.getKey().toString(), entry.getValue().toString());
                        }

                        if (replace.containsKey("answerLanguage")) {
                            String languageCode = replace.get("answerLanguage").toString();
                            String answerLangPrompt = AnswerLanguage.getAnswerLangPromptByCode(languageCode);
                            if (StringUtils.isNotBlank(answerLangPrompt)) {
                                pTemplate.appendLast(answerLangPrompt);
                            }
                        }
                        commandPrompt = pTemplate.getPrompt();
                        log.debug("CommandPrompt: {}", commandPrompt);
                        DevPilotMessage devPilotMessage = new DevPilotMessage();
                        devPilotMessage.setContent(commandPrompt);
                        devPilotMessage.setRole("user");
                        devPilotMessages.add(devPilotMessage);
                    } else if ("user".equals(role)) {
                        if (!"PURE_CHAT".equals(commandType)) {
                            DevPilotMessage promptMsg = new DevPilotMessage();
                            LlmPromptProperties.PromptConfig promptConfig = explainCommandPrompt.get(commandType.concat("_").concat(version));
                            promptMsg.setContent(promptConfig.getPromptTemplate());
                            promptMsg.setRole("user");
                            devPilotMessages.add(promptMsg);
                        }
                        DevPilotMessage devPilotMessage = new DevPilotMessage();
                        devPilotMessage.setContent(chatMsg);
                        devPilotMessage.setRole("user");
                        devPilotMessages.add(devPilotMessage);
                    } else {
                        DevPilotMessage devPilotMessage = new DevPilotMessage();
                        devPilotMessage.setContent(chatMsg);
                        devPilotMessage.setRole("assistant");
                        devPilotMessages.add(devPilotMessage);
                    }
                }
                DevPilotChatCompletionRequest devPilotChatCompletionRequest = new DevPilotChatCompletionRequest();
                devPilotChatCompletionRequest.setModel(model);
                devPilotChatCompletionRequest.setStream(stream);
                devPilotChatCompletionRequest.setMessages(devPilotMessages);
                byte[] bytes = jacksonMapper.toJson(devPilotChatCompletionRequest).getBytes(StandardCharsets.UTF_8);
                DataBuffer newDataBuffer = new DefaultDataBufferFactory().wrap(bytes);
                Flux<DataBuffer> flux = Flux.just(newDataBuffer);
                HttpHeaders newHeaders = new HttpHeaders();
                newHeaders.putAll(exchange.getRequest().getHeaders());
                newHeaders.setContentLength(bytes.length);
                ServerHttpRequestDecorator newRequest = new ServerHttpRequestDecorator(request) {
                    @Override
                    public Flux<DataBuffer> getBody() {
                        return flux;
                    }

                    @Override
                    public HttpHeaders getHeaders() {
                        return newHeaders;
                    }
                };
                return chain.filter(exchange.mutate().request(newRequest).build());

            } else {
                return chain.filter(exchange);
            }
        }, getOrder());
    }

    @Override
    public int getOrder() {
        return 2;
    }

    @Data
    public static class Config {

    }

}
