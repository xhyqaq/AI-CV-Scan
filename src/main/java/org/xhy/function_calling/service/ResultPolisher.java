package org.xhy.function_calling.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.xhy.function_calling.dto.response.ChatCompletionResponse;
import org.xhy.function_calling.llm.LLMService;

import java.util.List;
import java.util.Map;

/**
 * 结果润色服务
 * 负责将原始任务执行结果处理成更友好的格式
 */
@Service
public class ResultPolisher {

    private static final Logger logger = LoggerFactory.getLogger(ResultPolisher.class);

    private final LLMService llmService;

    public ResultPolisher(@Qualifier("qwenLLMService") LLMService llmService) {
        this.llmService = llmService;
    }

    /**
     * 润色任务执行结果
     * 
     * @param originalQuery 用户原始查询
     * @param subtasks      子任务列表
     * @param results       执行结果列表
     * @return 润色后的结果
     */
    public String polishResult(String originalQuery, List<String> subtasks, List<Map<String, Object>> results) {
        try {
            // 构建提示词，包含原始查询、子任务和结果
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("用户的原始任务是: ").append(originalQuery).append("\n\n");
            promptBuilder.append("系统将任务分解为以下子任务:\n");

            for (int i = 0; i < subtasks.size(); i++) {
                promptBuilder.append(i + 1).append(". ").append(subtasks.get(i)).append("\n");
            }

            promptBuilder.append("\n执行结果如下:\n");
            for (Map<String, Object> result : results) {
                promptBuilder.append(result).append("\n");
            }

            promptBuilder.append("\n请根据以上信息，生成一个结构化、美观、易于理解的最终结果。");
            promptBuilder.append("结果应当包含所有重要信息，但要去除技术细节和冗余数据。");
            promptBuilder.append("使用清晰的标题、分段和列表使内容易于阅读。");
            promptBuilder.append("如果是旅行计划，应包含每天的具体行程安排、景点介绍、用餐建议和交通指南等内容。");

            // 调用LLM进行润色
            ChatCompletionResponse response = llmService.getChatCompletion(promptBuilder.toString());

            // 提取润色后的内容
            String polishedResult = "";
            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                polishedResult = response.getChoices().get(0).getMessage().getContent();
            }

            if (polishedResult.isEmpty()) {
                logger.warn("结果润色失败，返回原始结果");
                return "无法生成美化结果，请查看原始数据。";
            }

            return polishedResult;

        } catch (Exception e) {
            logger.error("结果润色过程中出错", e);
            return "结果润色失败: " + e.getMessage();
        }
    }
}