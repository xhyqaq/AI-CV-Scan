package org.xhy.function_calling.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.xhy.function_calling.dto.response.ChatCompletionResponse;
import org.xhy.function_calling.llm.LLMService;
import org.xhy.function_calling.service.TaskExecutor;
import org.xhy.function_calling.tool.ToolCallProcessor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于LLM和工具调用的任务执行器实现
 * 负责执行子任务，判断是否需要调用工具，并追踪执行进度
 */
@Service
public class LLMTaskExecutor implements TaskExecutor {

    private static final Logger logger = LoggerFactory.getLogger(LLMTaskExecutor.class);

    private final LLMService llmService;
    private final ToolCallProcessor toolCallProcessor;

    // 进度追踪
    private final AtomicInteger totalTasks = new AtomicInteger(0);
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private String currentTask = "";
    private final Map<String, Object> taskResults = new HashMap<>();

    public LLMTaskExecutor(@Qualifier("qwenLLMService") LLMService llmService, ToolCallProcessor toolCallProcessor) {
        this.llmService = llmService;
        this.toolCallProcessor = toolCallProcessor;
    }

    @Override
    public Map<String, Object> executeTask(String subtask, Map<String, Object> context) {
        // 更新当前任务信息
        this.currentTask = subtask;

        // 使用上下文信息构建提示词
        StringBuilder contextInfo = new StringBuilder();
        if (context != null && !context.isEmpty()) {
            contextInfo.append("以下是之前任务的执行结果：\n");
            context.forEach((key, value) -> contextInfo.append(key).append(": ").append(value).append("\n"));
            contextInfo.append("\n");
        }

        String prompt = String.format(
                "%s请执行以下任务：%s",
                contextInfo.toString(),
                subtask);

        logger.info("执行子任务: {}", subtask);

        try {
            // 调用LLM处理任务
            ChatCompletionResponse response = llmService.getChatCompletion(prompt);

            // 处理LLM响应和可能的工具调用
            Map<String, Object> result = toolCallProcessor.processResponse(response, subtask);

            // 将结果添加到任务结果集
            String resultKey = "任务" + completedTasks.incrementAndGet();
            taskResults.put(resultKey, result);

            // 返回单个任务的执行结果
            return result;
        } catch (Exception e) {
            logger.error("执行子任务时出错", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "执行任务过程中出现错误: " + e.getMessage());
            errorResult.put("status", "failed");

            // 增加已完成任务计数（即使失败）
            completedTasks.incrementAndGet();

            return errorResult;
        }
    }

    @Override
    public Map<String, Object> getProgressInfo() {
        Map<String, Object> progressInfo = new HashMap<>();
        progressInfo.put("total_tasks", totalTasks.get());
        progressInfo.put("completed_tasks", completedTasks.get());
        progressInfo.put("current_task", currentTask);
        progressInfo.put("progress_percentage", totalTasks.get() > 0
                ? (double) completedTasks.get() / totalTasks.get() * 100
                : 0);
        progressInfo.put("task_results", taskResults);

        return progressInfo;
    }

    /**
     * 设置总任务数
     * 
     * @param count 总任务数
     */
    public void setTotalTasks(int count) {
        totalTasks.set(count);
        completedTasks.set(0);
        taskResults.clear();
    }

    /**
     * 重置执行状态
     */
    public void reset() {
        totalTasks.set(0);
        completedTasks.set(0);
        currentTask = "";
        taskResults.clear();
    }
}