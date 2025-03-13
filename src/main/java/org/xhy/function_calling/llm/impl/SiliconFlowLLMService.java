package org.xhy.function_calling.llm.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xhy.function_calling.dto.request.*;
import org.xhy.function_calling.dto.response.ChatCompletionResponse;
import org.xhy.function_calling.llm.LLMService;
import org.xhy.function_calling.tool.ToolRegistry;
import org.xhy.function_calling.util.JsonUtils;

import java.util.Collections;

/**
 * SiliconFlow LLM 服务实现
 */
@Service
public class SiliconFlowLLMService implements LLMService {

    private static final Logger logger = LoggerFactory.getLogger(SiliconFlowLLMService.class);

    private final RestTemplate restTemplate;
    private final ToolRegistry toolRegistry;

    @Value("${siliconflow.api.url}")
    private String apiUrl;

    @Value("${siliconflow.api.token}")
    private String apiToken;

    @Value("${siliconflow.api.model:Qwen/Qwen2.5-72B-Instruct-128K}")
    private String model;

    public SiliconFlowLLMService(RestTemplate restTemplate, ToolRegistry toolRegistry) {
        this.restTemplate = restTemplate;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public ChatCompletionResponse getChatCompletion(String userQuery) {
        // 创建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiToken);

        // 创建消息
        Message userMessage = Message.builder()
                .role("user")
                .content(userQuery)
                .build();

        // 创建请求体
        ChatCompletionRequest requestBody = ChatCompletionRequest.builder()
                .model(model)
                .messages(Collections.singletonList(userMessage))
                .stream(false)
                .max_tokens(512)
                .stop(null)
                .temperature(0.7)
                .top_p(0.7)
                .top_k(50)
                .frequency_penalty(0.5)
                .n(1)
                .response_format(new ResponseFormat("text"))
                .tools(toolRegistry.getToolsForLLM()) // 使用工具注册表获取所有工具
                .build();

        // 创建 HTTP 请求实体
        HttpEntity<ChatCompletionRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        // 发送请求并获取响应
        logger.debug("发送请求到 SiliconFlow API: {}", JsonUtils.toJson(requestBody));
        ChatCompletionResponse response = restTemplate.postForObject(apiUrl, requestEntity,
                ChatCompletionResponse.class);
        logger.debug("收到 SiliconFlow API 响应: {}", JsonUtils.toJson(response));

        return response;
    }

    @Override
    public ChatCompletionResponse getChatCompletion(ChatCompletionRequest request) {
        // 创建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiToken);

        // 创建 HTTP 请求实体
        HttpEntity<ChatCompletionRequest> requestEntity = new HttpEntity<>(request, headers);

        // 发送请求并获取响应
        logger.debug("发送请求到 SiliconFlow API: {}", JsonUtils.toJson(request));
        ChatCompletionResponse response = restTemplate.postForObject(apiUrl, requestEntity,
                ChatCompletionResponse.class);
        logger.debug("收到 SiliconFlow API 响应: {}", JsonUtils.toJson(response));

        return response;
    }

    @Override
    public ChatCompletionResponse getChatCompletion(UrlChatCompletionRequest request) {
        // 创建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiToken);

        // 创建 HTTP 请求实体
        HttpEntity<UrlChatCompletionRequest> requestEntity = new HttpEntity<>(request, headers);

        // 发送请求并获取响应
        logger.debug("发送请求到 SiliconFlow API: {}", JsonUtils.toJson(request));
        ChatCompletionResponse response = restTemplate.postForObject(apiUrl, requestEntity,
                ChatCompletionResponse.class);
        logger.debug("收到 SiliconFlow API 响应: {}", JsonUtils.toJson(response));

        return response;
    }

    @Override
    public String getProviderName() {
        return "SiliconFlow";
    }
}