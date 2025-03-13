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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 通义千问 LLM 服务实现
 * 阿里云通义千问API服务 - 支持Function Calling功能
 */
@Service
public class QwenLLMService implements LLMService {

        private static final Logger logger = LoggerFactory.getLogger(QwenLLMService.class);

        private final RestTemplate restTemplate;
        private final ToolRegistry toolRegistry;

        @Value("${qwen.api.url:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}")
        private String apiUrl;

        @Value("${qwen.api.token}")
        private String apiToken;

        @Value("${qwen.api.model:qwen-plus}")
        private String model;

        public QwenLLMService(RestTemplate restTemplate, ToolRegistry toolRegistry) {
                this.restTemplate = restTemplate;
                this.toolRegistry = toolRegistry;
        }

        @Override
        @Deprecated
        public ChatCompletionResponse getChatCompletion(String userQuery) {
                // 创建请求头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + apiToken);

                // 创建消息列表
                List<Message> messages = new ArrayList<>();

                // 添加系统消息
                Message systemMessage = Message.builder()
                                .role("system")
                                .content("你是一个智能助手，可以使用工具来帮助用户完成任务。")
                                .build();

                // 添加用户消息
                Message userMessage = Message.builder()
                                .role("user")
                                .content(userQuery)
                                .build();

                messages.add(systemMessage);
                messages.add(userMessage);

                // 创建请求体
                ChatCompletionRequest requestBody = ChatCompletionRequest.builder()
                                .model(model)
                                .messages(messages)
                                .stream(false)
                                .max_tokens(2048)
                                .stop(null)
                                .temperature(0.7)
                                .top_p(0.7)
                                .frequency_penalty(0.0)
                                .n(1)
                                .response_format(new ResponseFormat("text"))
                                .tools(toolRegistry.getToolsForLLM())
                                .build();

                return sendRequest(requestBody, headers);
        }

        @Override
        public ChatCompletionResponse getChatCompletion(ChatCompletionRequest request) {
                // 创建请求头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + apiToken);

                // 如果请求中没有指定模型，使用默认模型
                if (request.getModel() == null || request.getModel().trim().isEmpty()) {
                        request.setModel(model);
                }

                // 如果请求中没有指定工具，添加默认工具
                if (request.getTools() == null || request.getTools().isEmpty()) {
                        request.setTools(toolRegistry.getToolsForLLM());
                }

                return sendRequest(request, headers);
        }

        @Override
        public ChatCompletionResponse getChatCompletion(UrlChatCompletionRequest request) {
                return null;
        }

        private ChatCompletionResponse sendRequest(ChatCompletionRequest requestBody, HttpHeaders headers) {
                // 创建 HTTP 请求实体
                HttpEntity<ChatCompletionRequest> requestEntity = new HttpEntity<>(requestBody, headers);

                // 发送请求并获取响应
                logger.debug("发送请求到 通义千问 API: {}", JsonUtils.toJson(requestBody));
                ChatCompletionResponse response = restTemplate.postForObject(apiUrl, requestEntity,
                                ChatCompletionResponse.class);

                // 记录响应内容，帮助调试可能的截断问题
                if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                        String content = response.getChoices().get(0).getMessage().getContent();
                        logger.debug("收到通义千问响应，内容长度: {}", content != null ? content.length() : 0);
                        if (content != null && content.length() > 500) {
                                logger.debug("响应内容前500字符: {}", content.substring(0, 500));
                                logger.debug("响应内容后500字符: {}", content.substring(Math.max(0, content.length() - 500)));
                        }
                } else {
                        logger.warn("收到空响应或无选择项");
                }

                return response;
        }

        @Override
        public String getProviderName() {
                return "Qwen";
        }
}