package org.xhy.function_calling.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.xhy.function_calling.dto.response.ChatCompletionResponse;
import org.xhy.function_calling.llm.LLMService;
import org.xhy.function_calling.service.TaskDecomposer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
                "你是一个专业的任务规划专家，请根据用户的需求，将复杂任务分解为合理的子任务序列。" +
                        "在分解任务时，请考虑以下几点：" +
                        "\n1. 充分理解用户的真实需求和背景，挖掘潜在的子任务" +
                        "\n2. 子任务应该覆盖问题解决的整个过程，确保完整性" +
                        "\n3. 根据任务的复杂度，决定合适的子任务粒度和数量" +
                        "\n4. 子任务应按照合理的顺序排列，确保执行的流畅性" +
                        "\n5. 子任务描述应面向用户，清晰易懂，避免技术术语" +
                        "\n6. 创造性地考虑用户可能忽略的方面，提供全面的规划" +
                        "\n\n以下是用户的需求：%s" +
                        "\n\n请分解为合理的子任务序列，直接以数字编号的形式列出，无需额外解释。",
                userQuery);

        try {
            // 调用LLM来分解任务
            ChatCompletionResponse response = llmService.getChatCompletion(decompositionPrompt);

            // 从响应中获取内容
            String decompositionResult = "";
            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                decompositionResult = response.getChoices().get(0).getMessage().getContent();
                logger.debug("LLM返回的任务分解原始结果: {}", decompositionResult);
            }

            if (decompositionResult == null || decompositionResult.isEmpty()) {
                logger.error("任务分解失败: LLM返回空结果");
                return Collections.singletonList(userQuery);
            }

            // 更健壮的子任务提取逻辑
            List<String> subtasks = new ArrayList<>();

            // 尝试从格式化的数字列表中提取
            String[] lines = decompositionResult.split("\\r?\\n");
            for (String line : lines) {
                line = line.trim();

                // 跳过空行和非任务行
                if (line.isEmpty() || line.startsWith("以下是") || line.startsWith("这是") ||
                        line.startsWith("我会") || line.startsWith("接下来")) {
                    continue;
                }

                // 匹配常见的任务格式: 数字、序号或点号开头
                // 例如: "1. 任务", "1) 任务", "• 任务", "- 任务"等
                String cleanedLine = line.replaceAll("^\\d+\\.\\s*|^\\d+\\)\\s*|^[•\\-]\\s*", "").trim();

                if (!cleanedLine.isEmpty()) {
                    subtasks.add(cleanedLine);
                }
            }

            logger.debug("解析后的子任务列表({} 个): {}", subtasks.size(), subtasks);

            if (subtasks.isEmpty()) {
                logger.warn("未能提取有效子任务，将使用原始查询作为单一任务");
                return Collections.singletonList(userQuery);
            }

            logger.info("成功将任务分解为 {} 个子任务", subtasks.size());
            return subtasks;
        } catch (Exception e) {
            logger.error("任务分解过程中发生异常", e);
            return Collections.singletonList(userQuery);
        }
    }
}