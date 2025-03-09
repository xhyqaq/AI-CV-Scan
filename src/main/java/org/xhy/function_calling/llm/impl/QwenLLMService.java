package org.xhy.function_calling.llm.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xhy.function_calling.dto.request.*;
import org.xhy.function_calling.dto.response.ChatCompletionResponse;
import org.xhy.function_calling.llm.LLMService;
import org.xhy.function_calling.tool.ToolRegistry;
import org.xhy.function_calling.util.JsonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 通义千问 LLM 服务实现
 * 阿里云通义千问API服务 - 支持Function Calling功能
 */
@Service
public class QwenLLMService implements LLMService {

        private static final Logger logger = LoggerFactory.getLogger(QwenLLMService.class);

        private final RestTemplate restTemplate;
        private final ToolRegistry toolRegistry;

        @Value("${qwen.api.url:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}")
        private String apiUrl;

        @Value("${qwen.api.token}")
        private String apiToken;

        @Value("${qwen.api.model:qwen-plus}")
        private String model;

        public QwenLLMService(RestTemplate restTemplate, ToolRegistry toolRegistry) {
                this.restTemplate = restTemplate;
                this.toolRegistry = toolRegistry;
        }

        @Override
        public ChatCompletionResponse getChatCompletion(String userQuery) {
                // 创建请求头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + apiToken);

                // 创建消息列表
                List<Message> messages = new ArrayList<>();

                // 添加系统消息
                Message systemMessage = Message.builder()
                                .role("system")
                                .content("你是一个智能旅游助手，可以使用工具来帮助用户完成旅游规划。\n\n" +
                                                "当用户需要旅游规划或相关信息时，请使用google_search工具进行全面搜索，包括：\n" +
                                                "1. 搜索'[城市名] 近期天气预报'获取天气信息\n" +
                                                "2. 搜索'[城市名] 热门旅游景点'获取景点信息\n" +
                                                "3. 搜索'[城市名] 推荐餐厅'或'[城市名] 美食'获取餐饮信息\n" +
                                                "4. 搜索'[城市名] 住宿推荐'或'[城市名] 酒店'获取住宿信息\n" +
                                                "5. 搜索'[城市名] 交通指南'获取当地交通信息\n" +
                                                "6. 搜索'[城市名] 旅游预算'或'[城市名] 旅游花费'获取费用参考\n" +
                                                "7. 搜索'[城市名] [天数]天 旅游攻略'获取整体行程建议\n\n" +
                                                "请根据用户的具体需求有策略地确定搜索关键词，执行多次搜索以获得足够的信息。" +
                                                "然后根据搜索结果，为用户提供全面、详细的旅游规划，包括：\n" +
                                                "- 行程总览（几天几晚，住宿地点，主要景点）\n" +
                                                "- 每日详细行程安排（上午/下午/晚上的活动安排）\n" +
                                                "- 天气和着装建议\n" +
                                                "- 餐饮推荐（当地特色美食和餐厅）\n" +
                                                "- 住宿建议（位置、价格范围、酒店类型）\n" +
                                                "- 交通方式推荐（市内交通、景点间交通）\n" +
                                                "- 预算参考（总体花费估算，各项具体费用）\n" +
                                                "- 旅行贴士（注意事项、必备物品等）\n\n" +
                                                "在回答中结构化展示信息，使用标题、分点和表格使内容清晰易读。" +
                                                "google_search工具能够搜索任何城市和目的地的信息，不受地域限制。")
                                .build();

                // 添加用户消息
                Message userMessage = Message.builder()
                                .role("user")
                                .content(userQuery)
                                .build();

                messages.add(systemMessage);
                messages.add(userMessage);

                // 创建请求体
                ChatCompletionRequest requestBody = ChatCompletionRequest.builder()
                                .model(model)
                                .messages(messages)
                                .stream(false)
                                .max_tokens(2048)
                                .stop(null)
                                .temperature(0.7)
                                .top_p(0.7)
                                .frequency_penalty(0.0)
                                .n(1)
                                .response_format(new ResponseFormat("text"))
                                .tools(toolRegistry.getToolsForLLM())
                                .build();

                // 创建 HTTP 请求实体
                HttpEntity<ChatCompletionRequest> requestEntity = new HttpEntity<>(requestBody, headers);

                // 发送请求并获取响应
                logger.debug("发送请求到 通义千问 API: {}", JsonUtils.toJson(requestBody));
                ChatCompletionResponse response = restTemplate.postForObject(apiUrl, requestEntity,
                                ChatCompletionResponse.class);

                // 记录响应内容，帮助调试可能的截断问题
                if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                        String content = response.getChoices().get(0).getMessage().getContent();
                        logger.debug("收到通义千问响应，内容长度: {}", content != null ? content.length() : 0);
                        if (content != null && content.length() > 500) {
                                logger.debug("响应内容前500字符: {}", content.substring(0, 500));
                                logger.debug("响应内容后500字符: {}", content.substring(Math.max(0, content.length() - 500)));
                        }
                } else {
                        logger.warn("收到空响应或无选择项");
                }

                return response;
        }

        @Override
        public String getProviderName() {
                return "Qwen";
        }
}