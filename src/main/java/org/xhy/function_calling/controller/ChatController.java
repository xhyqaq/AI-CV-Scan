package org.xhy.function_calling.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xhy.function_calling.dto.response.ChatCompletionResponse;
import org.xhy.function_calling.llm.LLMService;
import org.xhy.function_calling.tool.ToolCallProcessor;

import java.util.Map;

/**
 * 提供统一的聊天接口，支持工具调用
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final LLMService llmService;
    private final ToolCallProcessor toolCallProcessor;

    public ChatController(
            @Qualifier("qwenLLMService") LLMService llmService,
            ToolCallProcessor toolCallProcessor) {
        this.llmService = llmService;
        this.toolCallProcessor = toolCallProcessor;
    }

    /**
     * 聊天接口
     *
     * @param query 用户查询
     * @return 聊天响应，包含AI回复和可能的工具调用结果
     */
    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> chat(@RequestParam String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "查询内容不能为空"));
        }

        logger.info("收到聊天请求: {}", query);

        // 调用LLM服务
        ChatCompletionResponse response = llmService.getChatCompletion(query);

        // 处理响应和工具调用
        Map<String, Object> result = toolCallProcessor.processResponse(response, query);

        // 添加LLM提供商信息
        result.put("provider", llmService.getProviderName());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/")
    public String index() {
        return "index"; // 确保这是正确的模板名称，不需要添加.html后缀
    }
}