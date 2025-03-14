package org.xhy.function_calling.llm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.xhy.function_calling.dto.request.ChatCompletionRequest;
import org.xhy.function_calling.dto.request.Message;
import org.xhy.function_calling.dto.response.ChatCompletionResponse;
import org.xhy.function_calling.dto.response.Choice;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实LLM服务测试
 * 注意：此测试会调用真实的LLM API，可能会产生费用
 */
@SpringBootTest
public class LLMServiceTest {

    @Qualifier("siliconFlowLLMService")
    @Autowired
    private LLMService llmService;

    @Test
    public void testRealChatCompletion() {
        // 创建请求
        ChatCompletionRequest request = new ChatCompletionRequest();
        Message userMessage = new Message();
        userMessage.setRole("user");
        userMessage.setContent("你好，请介绍一下自己");
        request.setModel("Qwen/Qwen2-7B-Instruct");
        request.setMessages(Collections.singletonList(userMessage));

        // 调用真实的LLM服务
        ChatCompletionResponse response = llmService.getChatCompletion(request);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getId());
        assertFalse(response.getChoices().isEmpty());

        Choice firstChoice = response.getChoices().get(0);
        assertNotNull(firstChoice.getMessage());
        assertNotNull(firstChoice.getMessage().getContent());
        assertFalse(firstChoice.getMessage().getContent().isEmpty());

        // 打印响应内容，便于查看
        System.out.println("LLM响应: " + firstChoice.getMessage().getContent());
    }
}