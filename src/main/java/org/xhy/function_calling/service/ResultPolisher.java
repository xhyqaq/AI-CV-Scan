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
            for (int i = 0; i < results.size(); i++) {
                promptBuilder.append("任务").append(i + 1).append("结果: ").append(results.get(i)).append("\n\n");
            }

            promptBuilder.append("\n请根据以上信息，生成一个结构化、美观、易于理解的最终结果。");
            promptBuilder.append("结果应当包含所有重要信息，但要去除技术细节和冗余数据。");
            promptBuilder.append("使用清晰的标题、分段和列表使内容易于阅读。");

            // 添加针对旅行规划的特殊指引
            if (originalQuery.contains("旅游") || originalQuery.contains("旅行") ||
                    originalQuery.contains("游玩") || originalQuery.contains("计划") ||
                    originalQuery.contains("行程")) {
                promptBuilder.append("\n\n对于旅行计划，请提供一个全面的旅游指南，应包含以下内容：");
                promptBuilder.append("\n1. 行程概述");
                promptBuilder.append("\n   - 标题（城市名称和天数）");
                promptBuilder.append("\n   - 行程亮点和总体安排");
                promptBuilder.append("\n   - 推荐的住宿区域");

                promptBuilder.append("\n2. 出行准备");
                promptBuilder.append("\n   - 天气情况和着装建议");
                promptBuilder.append("\n   - 必备物品清单");
                promptBuilder.append("\n   - 当地语言和文化简介");

                promptBuilder.append("\n3. 每天的详细行程安排，清晰标明'第X天'，包括：");
                promptBuilder.append("\n   - 上午活动和推荐景点（包含开放时间和预计游玩时长）");
                promptBuilder.append("\n   - 午餐建议（特色餐厅或美食推荐）");
                promptBuilder.append("\n   - 下午活动和推荐景点");
                promptBuilder.append("\n   - 晚餐建议");
                promptBuilder.append("\n   - 晚上活动或休息安排");

                promptBuilder.append("\n4. 交通指南");
                promptBuilder.append("\n   - 市内交通选择和建议");
                promptBuilder.append("\n   - 往返各景点的交通方式");
                promptBuilder.append("\n   - 交通卡或特殊交通工具推荐");

                promptBuilder.append("\n5. 住宿建议");
                promptBuilder.append("\n   - 推荐住宿区域和理由");
                promptBuilder.append("\n   - 不同预算的住宿选择");
                promptBuilder.append("\n   - 预订建议和注意事项");

                promptBuilder.append("\n6. 餐饮指南");
                promptBuilder.append("\n   - 必尝特色美食清单");
                promptBuilder.append("\n   - 各区域推荐餐厅");
                promptBuilder.append("\n   - 不同价位的用餐选择");

                promptBuilder.append("\n7. 预算规划");
                promptBuilder.append("\n   - 总体预算估算");
                promptBuilder.append("\n   - 各项费用明细（住宿、餐饮、交通、门票、购物）");
                promptBuilder.append("\n   - 省钱小贴士");

                promptBuilder.append("\n8. 实用提示");
                promptBuilder.append("\n   - 最佳旅游季节");
                promptBuilder.append("\n   - 安全注意事项");
                promptBuilder.append("\n   - 当地习俗和礼仪");
                promptBuilder.append("\n   - 紧急联系方式");

                // 引导处理搜索结果数据
                promptBuilder.append("\n\n请注意，以上搜索结果中包含各种信息，你需要整合这些信息，提取关键数据，形成一个完整连贯的旅游指南。");
                promptBuilder.append("\n对于缺失的信息，可以基于已有数据合理推断，但要明确标识推断的部分。");
                promptBuilder.append("\n最终的旅游指南应该是一个结构清晰、信息全面、实用性强的文档，可以直接指导用户的旅行活动。");
            }

            // 调用LLM进行润色
            ChatCompletionResponse response = llmService.getChatCompletion(promptBuilder.toString());

            // 提取润色后的内容
            String polishedResult = "";
            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                polishedResult = response.getChoices().get(0).getMessage().getContent();
            }

            if (polishedResult.isEmpty()) {
                logger.warn("结果润色失败: LLM返回空结果");
                return "无法生成美化结果，请查看原始数据。";
            }

            return polishedResult;

        } catch (Exception e) {
            logger.error("结果润色过程中出错", e);
            return "结果润色失败: " + e.getMessage();
        }
    }
}