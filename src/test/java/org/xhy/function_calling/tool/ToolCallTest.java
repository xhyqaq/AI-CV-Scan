package org.xhy.function_calling.tool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.xhy.function_calling.dto.GitHubAnalysisResult;
import org.xhy.function_calling.dto.request.ChatCompletionRequest;
import org.xhy.function_calling.dto.request.Message;
import org.xhy.function_calling.dto.request.Tool;
import org.xhy.function_calling.dto.response.ChatCompletionResponse;
import org.xhy.function_calling.dto.response.Choice;
import org.xhy.function_calling.dto.response.ResponseMessage;
import org.xhy.function_calling.dto.response.ToolCall;
import org.xhy.function_calling.llm.LLMService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实工具调用测试
 * 注意：此测试会调用真实的LLM API，可能会产生费用
 */
@SpringBootTest
public class ToolCallTest {

    @Qualifier("siliconFlowLLMService")
    @Autowired
    private LLMService llmService;

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private ToolCallProcessor toolCallProcessor;

    @Test
    public void testRealEchoToolCall() {
        // 创建请求
        ChatCompletionRequest request = new ChatCompletionRequest();
        Message userMessage = new Message();
        userMessage.setRole("user");
        userMessage.setContent("请使用echo工具回显这条消息：'这是一个真实的工具调用测试'");
        request.setMessages(Collections.singletonList(userMessage));
        request.setModel("THUDM/glm-4-9b-chat");

        // 添加工具定义
        List<Tool> tools = toolRegistry.getToolsForLLM();
        request.setTools(tools);

        // 调用真实的LLM服务
        ChatCompletionResponse response = llmService.getChatCompletion(request);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getId());
        assertFalse(response.getChoices().isEmpty());

        Choice firstChoice = response.getChoices().get(0);
        ResponseMessage responseMessage = firstChoice.getMessage();

        // 打印响应内容，便于查看
        System.out.println("LLM响应类型: " + firstChoice.getFinish_reason());
        if (responseMessage.getContent() != null) {
            System.out.println("LLM响应内容: " + responseMessage.getContent());
        }

        // 如果是工具调用，则处理工具调用
        if ("tool_calls".equals(firstChoice.getFinish_reason()) && responseMessage.getTool_calls() != null) {
            System.out.println("检测到工具调用，共 " + responseMessage.getTool_calls().size() + " 个");

            for (ToolCall toolCall : responseMessage.getTool_calls()) {
                System.out.println("工具调用ID: " + toolCall.getId());
                System.out.println("工具名称: " + toolCall.getFunction().getName());
                System.out.println("工具参数: " + toolCall.getFunction().getArguments());
            }

            // 处理工具调用
            Map<String, Object> processResult = toolCallProcessor.processResponse(response, userMessage.getContent());
            assertNotNull(processResult);

            // 验证工具调用结果
            if (processResult.containsKey("toolResults")) {
                System.out.println("工具调用结果: " + processResult.get("toolResults"));
            } else if (processResult.containsKey("error")) {
                System.out.println("工具调用错误: " + processResult.get("error"));
                fail("工具调用出错: " + processResult.get("error"));
            }
        }
    }

    @Test
    public void testDirectToolExecution() {
        // 直接执行echo工具
        String testMessage = "这是一个直接执行的工具测试";
        String parameters = "{\"message\":\"" + testMessage + "\"}";

        Object result = toolRegistry.executeTool("echo", parameters);
        assertNotNull(result);

        System.out.println("直接执行echo工具结果: " + result);
    }

    @Test
    public void testGitHubAnalyzerTool() {
        // 1. 测试直接执行 GitHub 分析工具

        // 2. 测试通过 LLM 调用 GitHub 分析工具
        ChatCompletionRequest request = new ChatCompletionRequest();
        Message userMessage = new Message();
        userMessage.setRole("user");
        userMessage.setContent("github：https://github.com/LiusCraft");
        request.setMessages(Collections.singletonList(userMessage));
        request.setModel("THUDM/glm-4-9b-chat");

        // 添加工具定义
        List<Tool> tools = toolRegistry.getToolsForLLM();
        request.setTools(tools);

        // 调用真实的LLM服务
        ChatCompletionResponse response = llmService.getChatCompletion(request);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getId());
        assertFalse(response.getChoices().isEmpty());

        Choice firstChoice = response.getChoices().get(0);
        ResponseMessage responseMessage = firstChoice.getMessage();

        // 打印响应内容，便于查看
        System.out.println("LLM响应类型: " + firstChoice.getFinish_reason());
        if (responseMessage.getContent() != null) {
            System.out.println("LLM响应内容: " + responseMessage.getContent());
        }

        // 如果是工具调用，则处理工具调用
        if ("tool_calls".equals(firstChoice.getFinish_reason()) && responseMessage.getTool_calls() != null) {
            System.out.println("检测到工具调用，共 " + responseMessage.getTool_calls().size() + " 个");

            for (ToolCall toolCall : responseMessage.getTool_calls()) {
                System.out.println("工具调用ID: " + toolCall.getId());
                System.out.println("工具名称: " + toolCall.getFunction().getName());
                System.out.println("工具参数: " + toolCall.getFunction().getArguments());

                // 验证是否调用了 GitHub 分析工具
                if ("analyze_github_info".equals(toolCall.getFunction().getName())) {
                    System.out.println("成功调用 GitHub 分析工具");
                }
            }

            // 处理工具调用
            Map<String, Object> processResult = toolCallProcessor.processResponse(response, userMessage.getContent());
            assertNotNull(processResult);

            // 验证工具调用结果
            if (processResult.containsKey("toolResults")) {
                System.out.println("工具调用结果: " + processResult.get("toolResults"));
            } else if (processResult.containsKey("error")) {
                System.out.println("工具调用错误: " + processResult.get("error"));
                fail("工具调用出错: " + processResult.get("error"));
            }
        }
    }
}