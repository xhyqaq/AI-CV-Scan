package org.xhy.function_calling.llm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.xhy.function_calling.dto.request.UrlChatCompletionRequest;
import org.xhy.function_calling.dto.response.ChatCompletionResponse;
import org.xhy.function_calling.dto.response.Choice;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实URL聊天完成请求测试
 * 注意：此测试会调用真实的LLM API，可能会产生费用
 */
@SpringBootTest
public class UrlChatCompletionTest {

    @Autowired
    @Qualifier("qwenLLMService") // 明确指定使用通义千问服务
    private LLMService llmService;

    @Test
    public void testRealUrlChatCompletion() {
        // 创建URL请求 - 使用一个公开的图片URL
        List<String> imageUrls = Collections.singletonList(
                "https://raw.githubusercontent.com/spring-projects/spring-framework/main/src/docs/spring-framework-reference/images/spring-logo.svg");
        String prompt = "这是什么图片？请详细描述一下";

        UrlChatCompletionRequest request = UrlChatCompletionRequest.createRequest(imageUrls, prompt);

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
        System.out.println("URL分析响应: " + firstChoice.getMessage().getContent());
    }
}