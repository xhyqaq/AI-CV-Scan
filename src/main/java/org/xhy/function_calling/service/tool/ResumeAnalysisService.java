package org.xhy.function_calling.service.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xhy.function_calling.dto.request.ChatCompletionRequest;
import org.xhy.function_calling.dto.request.Message;
import org.xhy.function_calling.dto.request.UrlChatCompletionRequest;
import org.xhy.function_calling.dto.response.ChatCompletionResponse;
import org.xhy.function_calling.dto.response.ResumeAnalysisResult;
import org.xhy.function_calling.llm.LLMService;
import org.xhy.function_calling.util.JsonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历分析服务
 * 处理简历URL，使用大模型分析简历排版、提取内容并进行评价
 */
@Service
public class ResumeAnalysisService {

        private static final Logger logger = LoggerFactory.getLogger(ResumeAnalysisService.class);

        private final LLMService llmService;

        @Autowired
        public ResumeAnalysisService(@Qualifier("siliconFlowLLMService") LLMService llmService) {
                this.llmService = llmService;
        }

        /**
         * 分析简历
         * 
         * @param resumeUrl 函数参数 JSON 字符串
         * @return 简历分析结果
         */
        public ResumeAnalysisResult analyzeResume(String resumeUrl) {
                logger.info("收到简历分析请求: {}", resumeUrl);
                try {
                        // 1.分析排版
                        String layoutResult = analyzeLayoutAndExtractContent(resumeUrl);

                        // 2.解析简历内容
                        String contentJson = parseResumeContent(resumeUrl);

                        // 3. 评价简历
                        String evaluation = evaluateResume(contentJson);
                        logger.info("简历评价完成");

                        // 4. 生成总结
                        String summary = generateSummary(layoutResult, evaluation);
                        logger.info("简历分析总结完成");

                        // 构建并返回结果
                        return ResumeAnalysisResult.builder()
                                        .resumeUrl(resumeUrl)
                                        .layoutAnalysis(layoutResult)
                                        .evaluation(evaluation)
                                        .summary(summary)
                                        .build();

                } catch (Exception e) {
                        logger.error("处理简历分析请求失败", e);
                        return null;
                }
        }

        /**
         * 发送带图片URL的多模态请求
         * 
         * @param imageUrl  图片URL
         * @param prompt    提示词
         * @return 模型响应内容
         */
        private String sendMultimodalRequest(String imageUrl, String prompt) {

                UrlChatCompletionRequest request = UrlChatCompletionRequest.createRequest(Arrays.asList(imageUrl), prompt);
                request.setModel("Qwen/Qwen2-VL-72B-Instruct");
                ChatCompletionResponse response = llmService.getChatCompletion(request);
                return response.getChoices().get(0).getMessage().getContent();
        }

        /**
         * 分析简历排版
         */
        private String analyzeLayoutAndExtractContent(String resumeUrl) {
                logger.info("开始分析简历排版: {}", resumeUrl);
                String prompt = "你是一个专业的简历排版分析专家。请分析简历的排版格式，如果没有建议，则输出排版优秀。";
                String result = sendMultimodalRequest(resumeUrl, prompt);
                logger.info("简历排版分析完成: {}", result);
                return result;
        }

        /**
         * 解析简历内容
         * 
         * @return 简历内容文本
         */
        private String parseResumeContent(String resumeUrl) {
                logger.info("开始解析简历内容: {}", resumeUrl);
                String prompt = "提取文件中的所有内容，按照markdown的形式返回";
                String result = sendMultimodalRequest(resumeUrl, prompt);
                logger.info("简历内容解析完成，内容长度: {}", result.length());
                return result;
        }

        /**
         * 评价简历
         * 使用大模型根据提取的内容评价简历质量
         */
        private String evaluateResume(String extractedContent) {
                List<Message> messages = new ArrayList<>();

                // 系统消息
                messages.add(Message.builder()
                                .role("system")
                                .content("你是一个专业的简历评估专家。请根据提供的简历内容结合候选者的背景思考后进行全面评估")
                                .build());

                // 用户消息
                messages.add(Message.builder()
                                .role("user")
                                .content("简历内容：\n" + extractedContent)
                                .build());

                // 构建请求
                ChatCompletionRequest request = ChatCompletionRequest.builder()
                                .model("deepseek-ai/DeepSeek-R1")
                                .messages(messages)
                                .build();

                // 发送请求
                ChatCompletionResponse response = llmService.getChatCompletion(request);

                return response != null && response.getChoices() != null && !response.getChoices().isEmpty()
                                ? response.getChoices().get(0).getMessage().getContent()
                                : "无法评价简历";
        }

        /**
         * 生成总结
         * 综合前面的分析结果，生成最终总结
         */
        private String generateSummary(String layoutAnalysis, String extractedContent) {
                List<Message> messages = new ArrayList<>();

                // 系统消息
                messages.add(Message.builder()
                                .role("system")
                                .content("你是一个专业的简历顾问。请根据提供的分析结果，生成一份简洁的总结报告。" +
                                                "总结应该包含：\n" +
                                                "1. 简历的整体质量\n" +
                                                "2. 候选人的主要优势\n" +
                                                "3. 存在的主要问题\n" +
                                                "4. 是否建议进一步考虑该候选人\n" +
                                                "请确保总结不超过300字。")
                                .build());

                // 用户消息
                messages.add(Message.builder()
                                .role("user")
                                .content(String.format(
                                                "请根据以下信息生成总结：\n" +
                                                                "排版分析：%s\n\n" +
                                                                "简历内容：%s",
                                                layoutAnalysis,
                                                JsonUtils.toJson(extractedContent)))
                                .build());

                // 构建请求
                ChatCompletionRequest request = ChatCompletionRequest.builder()
                                .model("deepseek-ai/DeepSeek-R1")
                                .messages(messages)
                                .max_tokens(1024)
                                .temperature(0.7)
                                .build();

                // 发送请求
                ChatCompletionResponse response = llmService.getChatCompletion(request);

                return response != null && response.getChoices() != null && !response.getChoices().isEmpty()
                                ? response.getChoices().get(0).getMessage().getContent()
                                : "无法生成简历总结";
        }
}