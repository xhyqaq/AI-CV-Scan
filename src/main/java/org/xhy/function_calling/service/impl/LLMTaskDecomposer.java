package org.xhy.function_calling.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.xhy.function_calling.dto.response.ChatCompletionResponse;
import org.xhy.function_calling.llm.LLMService;
import org.xhy.function_calling.service.TaskDecomposer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于LLM的任务分解器实现
 * 通过向LLM提供特定的提示词，引导其将复杂任务分解为可执行的子任务
 */
@Service
public class LLMTaskDecomposer implements TaskDecomposer {

    private static final Logger logger = LoggerFactory.getLogger(LLMTaskDecomposer.class);
    private final LLMService llmService;

    public LLMTaskDecomposer(@Qualifier("qwenLLMService") LLMService llmService) {
        this.llmService = llmService;
    }

    @Override
    public List<String> decomposeTask(String userQuery) {
        String decompositionPrompt = String.format(
                "你是一个专业的任务规划助手，请将以下复杂任务分解为一系列有序的具体子任务，以便于执行。" +
                        "子任务描述应当面向用户，清晰易懂，避免使用技术术语或API调用细节。" +
                        "每个子任务应该描述一个完整的、独立的步骤，而不是实现该步骤的技术细节。" +
                        "例如，对于\"帮我安排北京3天游玩计划\"，好的子任务应该是\"获取北京热门景点信息\"而不是\"调用travel_planner获取景点\"。" +
                        "请将结果格式化为数字列表，每行一个子任务，不要添加额外的解释。" +
                        "复杂任务: %s",
                userQuery);

        try {
            // 调用LLM来分解任务
            ChatCompletionResponse response = llmService.getChatCompletion(decompositionPrompt);

            // 从响应中获取内容
            String decompositionResult = "";
            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                decompositionResult = response.getChoices().get(0).getMessage().getContent();
            }

            if (decompositionResult == null || decompositionResult.isEmpty()) {
                logger.error("任务分解失败: LLM返回空结果");
                return Collections.singletonList(userQuery);
            }

            // 解析LLM返回的子任务列表
            List<String> subtasks = Arrays.stream(decompositionResult.split("\n"))
                    .map(line -> line.replaceAll("^\\d+\\.\\s*", "").trim())
                    .filter(task -> !task.isEmpty())
                    .collect(Collectors.toList());

            if (subtasks.isEmpty()) {
                logger.warn("未能提取有效子任务，将使用原始查询作为单一任务");
                return Collections.singletonList(userQuery);
            }

            // 添加最后的润色步骤作为子任务之一
            subtasks.add("整理行程信息，形成最终的游玩计划");

            logger.info("成功将任务分解为{}个子任务", subtasks.size());
            return subtasks;
        } catch (Exception e) {
            logger.error("任务分解过程出错", e);
            // 出错时退回到将整个查询作为单一任务
            return Collections.singletonList(userQuery);
        }
    }
}