package com.zhongan.devpilot.gateway.filter;


import com.zhongan.devpilot.gateway.completions.providers.dto.DevPilotChatCompletionRequest;
import com.zhongan.devpilot.gateway.completions.providers.dto.DevPilotMessage;
import com.zhongan.devpilot.gateway.config.LlmPromptProperties;
import com.zhongan.devpilot.gateway.enums.AnswerLanguage;
import com.zhongan.devpilot.gateway.utils.JacksonMapper;
import com.zhongan.devpilot.gateway.utils.PromptTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

import reactor.core.publisher.Flux;

import static com.zhongan.devpilot.gateway.constant.Constants.REQUEST_BODY;


/**
 * 执行命令过滤器，替换提示词，请求大模型接口
 * <p>
 * 1. 获取请求的body
 * 2. 解析body中的type和version
 *
 * type:
 *    COMMENT_METHOD
 *    CHECK_PERFORMANCE
 *    GENERATE_COMMENTS
 *    GENERATE_TESTS
 *    FIX_CODE
 *    REVIEW_CODE
 *    EXPLAIN_CODE
 *    GENERATE_COMMIT
 *    PURE_CHAT （用户输入聊天类型消息）
 * version: 默认V1
 *
 * 3. 根据type和version获取对应的命令提示符
 * 4. 替换命令提示符中的占位符
 * 5. 请求大模型接口
 *
 * 提示词配置样例如下：
 llm-prompt:
    explain-command:
     # 生成方法注释
      COMMENT_METHOD_V1:
      model: "gpt-3.5-turbo"
      promptTemplate: |+
         "Write a function comment for the specified function in the appropriate style for the programming language being used.\n" +
         "The comment should start with a brief summary of what the function does. This should be followed by a detailed description, if necessary.\n" +
         "Then, document each parameter, explaining the purpose of each one.\n" +
         "If the function returns a value, describe what the function returns.\n" +
         "If the function throws exceptions, document each exception and under what conditions it is thrown.\n" +
         "Make sure the comment is clear, concise, and free of spelling and grammar errors.\n" +
         "The comment should help other developers understand what the function does, how it works, and how to use it.\n" +
         "Please note that the function definition is not included in this task, only the function comment is required.\n\n" +
         "The comment is being written for the following code: <code> \n" +
         "{{selectedCode}} </code>"

     PERFORMANCE_CHECK_V1:
     model: "gpt-3.5-turbo"
     promptTemplate: |+
         "Perform a performance check on the specified code. Only identify and report on the aspects where actual performance issues are found. Do not list out aspects that do not have issues.\n" +
         "The check may focus on, but is not limited to, the following aspects:\n" +
         "1.Algorithmic Efficiency: Check for inefficient algorithms that may slow down the program.\n" +
         "2.Data Structures: Evaluate the use of data structures for potential inefficiencies.\n" +
         "3.Loops: Inspect loops for unnecessary computations or operations that could be moved outside.\n" +
         "4.Method Calls: Look for frequent method calls.\n" +
         "5.Object Creation: Check for unnecessary object creation.\n" +
         "6.Use of Libraries: Review the use of library methods for potential inefficiencies.\n" +
         "7.Concurrency: If multithreading is used, ensure efficient operation and absence of bottlenecks or deadlocks.\n" +
         "8.I/O Operations: Look for inefficient use of I/O operations.\n" +
         "9.Database Queries: If the code interacts with a database, check for inefficient or excessive queries.\n" +
         "10.Network Calls: If the code makes network calls, consider their efficiency and potential impact on performance.\n" +

     GENERATE_COMMENTS_V1:
     model: "gpt-3.5-turbo"
     promptTemplate: |+
       "Write inline comments for the key parts of the specified function.\n" +
       "The comments should explain what each part of the function does in a clear and concise manner.\n" +
       "Avoid commenting on every single line of code, as this can make the code harder to read and maintain. Instead, focus on the parts of the function that are complex, important, or not immediately obvious.\n" +
       "Remember, the goal of inline comments is to help other developers understand the code, not to explain every single detail.\n\n" +
       "The comment is being written for the following code: <code>\n" +
       "{{selectedCode}} </code>"

     GENERATE_TESTS_V1:
     model: "gpt-3.5-turbo"
     promptTemplate: |+
       "{{selectedCode}}\n
        Giving the {{language:unknown}} code above, "
       "please help to generate {{testFramework:suitable}} test cases for it, "
       "mocking test data with {{mockFramework:suitable mock framework}} if necessary, "
       "{{additionalMockPrompt:}}"
       "be aware that if the code is untestable, "
       "please state it and give suggestions instead."

     FIX_CODE_V1:
     model: "gpt-3.5-turbo"
     promptTemplate: |+
       "Perform a code fix on the specified code. Only identify and make changes in the aspects where actual issues are found. Do not list out aspects that do not have issues. \n" +
       "The fix may focus on, but is not limited to, the following aspects:\n" +
       "1. Bug Fixes: Identify and correct any errors or bugs in the code. Ensure the fix doesn't introduce new bugs.\n" +
       "2. Performance Improvements: Look for opportunities to optimize the code for better performance. This could involve changing algorithms, reducing memory usage, or other optimizations.\n" +
       "3. Code Clarity: Make the code easier to read and understand. This could involve renaming variables for clarity, breaking up complex functions into smaller ones.\n" +
       "4. Code Structure: Improve the organization of the code. This could involve refactoring the code to improve its structure, or rearranging code for better readability.\n" +
       "5. Coding Standards: Ensure the code follows the agreed-upon coding standards. This includes naming conventions, comment style, indentation, and other formatting rules.\n" +
       "6. Error Handling: Improve error handling in the code. The code should fail gracefully and not expose any sensitive information when an error occurs.\n" +
 */
@Slf4j
@Component
public class DevPilotExplainCommandGatewayFilterFactory extends AbstractGatewayFilterFactory<DevPilotExplainCommandGatewayFilterFactory.Config> implements Ordered {

    private final LlmPromptProperties llmPromptProperties;

    @Autowired
    private JacksonMapper jacksonMapper;

    public DevPilotExplainCommandGatewayFilterFactory(LlmPromptProperties llmPromptProperties) {
        super(Config.class);
        this.llmPromptProperties = llmPromptProperties;
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
