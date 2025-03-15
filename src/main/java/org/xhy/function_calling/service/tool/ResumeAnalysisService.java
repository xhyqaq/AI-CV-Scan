package org.xhy.function_calling.service.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.xhy.function_calling.dto.request.ChatCompletionRequest;
import org.xhy.function_calling.dto.request.Message;
import org.xhy.function_calling.dto.request.Tool;
import org.xhy.function_calling.dto.request.UrlChatCompletionRequest;
import org.xhy.function_calling.dto.response.ChatCompletionResponse;
import org.xhy.function_calling.dto.response.Choice;
import org.xhy.function_calling.dto.response.ResumeAnalysisResult;
import org.xhy.function_calling.dto.response.ResponseMessage;
import org.xhy.function_calling.llm.LLMService;
import org.xhy.function_calling.tool.ToolCallProcessor;
import org.xhy.function_calling.tool.ToolRegistry;
import org.xhy.function_calling.util.JsonUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简历分析服务
 * 处理简历URL，使用大模型分析简历排版、提取内容并进行评价
 */
@Service
public class ResumeAnalysisService {

        private static final Logger logger = LoggerFactory.getLogger(ResumeAnalysisService.class);

        private final LLMService llmService;
        private final ToolRegistry toolRegistry;
        private final ToolCallProcessor toolCallProcessor;
        private final ExecutorService executorService;

        @Autowired
        public ResumeAnalysisService(
                        @Qualifier("siliconFlowLLMService") LLMService llmService,
                        ToolRegistry toolRegistry,
                        ToolCallProcessor toolCallProcessor) {
                this.llmService = llmService;
                this.toolRegistry = toolRegistry;
                this.toolCallProcessor = toolCallProcessor;
                this.executorService = Executors.newFixedThreadPool(4);
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
                        // 使用CompletableFuture进行并行处理
                        // 1. 分析排版 - 线程1
                        CompletableFuture<String> layoutFuture = CompletableFuture.supplyAsync(() -> {
                                logger.info("线程1: 开始分析简历排版");
                                return analyzeLayoutAndExtractContent(resumeUrl);
                        }, executorService);

                        // 2. 解析简历内容和GitHub分析 - 线程2
                        CompletableFuture<String> contentFuture = CompletableFuture.supplyAsync(() -> {
                                logger.info("线程2: 开始解析简历内容");
                                return parseResumeContent(resumeUrl);
                        }, executorService);

                        // 3. 提取GitHub链接并分析 - 线程3 (依赖于简历内容)
                        CompletableFuture<String> githubAnalysisFuture = contentFuture.thenApplyAsync(content -> {
                                logger.info("线程3: 开始提取GitHub链接并分析");
                                return analyzeGitHubProfile(content);
                        }, executorService);

                        // 4. 评价简历 - 线程4 (依赖于简历内容和GitHub分析)
                        CompletableFuture<String> evaluationFuture = CompletableFuture.allOf(
                                        contentFuture, githubAnalysisFuture)
                                        .thenApplyAsync(v -> {
                                                logger.info("线程4: 开始评价简历");
                                                String content = contentFuture.join();
                                                String githubAnalysis = githubAnalysisFuture.join();
                                                return evaluateResume(content, githubAnalysis);
                                        }, executorService);

                        // 等待所有结果并构建返回对象
                        String layout = layoutFuture.get();
                        String githubAnalysis = githubAnalysisFuture.get();
                        String evaluation = evaluationFuture.get();

                        // 处理排版结果 - 如果排版优秀，则简化输出
                        String formattedLayout = layout;
                        if (layout.contains("排版优秀") || layout.contains("格式良好") || layout.contains("结构清晰")) {
                                formattedLayout = "排版优秀";
                        }

                        logger.info("简历分析完成");

                        // 构建并返回结果 - 不再生成总结
                        return ResumeAnalysisResult.builder()
                                        .resumeUrl(resumeUrl)
                                        .layoutAnalysis(formattedLayout)
                                        .evaluation(evaluation)
                                        .githubAnalysis(githubAnalysis)
                                        .build();

                } catch (Exception e) {
                        logger.error("处理简历分析请求失败", e);
                        return null;
                }
        }

        /**
         * 从简历内容中提取GitHub链接
         */
        private String extractGitHubUrl(String content) {
                // 使用正则表达式匹配GitHub链接
                Pattern pattern = Pattern.compile("https?://(?:www\\.)?github\\.com/[\\w-]+(?:/[\\w.-]+)*");
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                        return matcher.group();
                }
                return null;
        }

        /**
         * 分析GitHub个人资料
         */
        private String analyzeGitHubProfile(String githubUrl) {
                logger.info("开始分析GitHub个人资料: {}", githubUrl);
                try {
                        // 创建请求
                        ChatCompletionRequest request = new ChatCompletionRequest();
                        Message userMessage = new Message();
                        userMessage.setRole("user");
                        userMessage.setContent("github：" + githubUrl);
                        request.setMessages(Collections.singletonList(userMessage));
                        request.setModel("Qwen/Qwen2.5-7B-Instruct");

                        // 添加工具定义
                        List<Tool> tools = toolRegistry.getToolsForLLM();
                        request.setTools(tools);

                        // 调用LLM服务
                        ChatCompletionResponse response = llmService.getChatCompletion(request);

                        // 处理响应
                        if (response != null && !response.getChoices().isEmpty()) {
                                Choice firstChoice = response.getChoices().get(0);
                                ResponseMessage responseMessage = firstChoice.getMessage();

                                // 如果是工具调用，则处理工具调用
                                if ("tool_calls".equals(firstChoice.getFinish_reason()) &&
                                                responseMessage.getTool_calls() != null) {

                                        logger.info("检测到GitHub分析工具调用");

                                        // 处理工具调用
                                        Map<String, Object> processResult = toolCallProcessor.processResponse(
                                                        response, userMessage.getContent());

                                        return analyzeGitHubToolResults(processResult);

                                } else if (responseMessage.getContent() != null) {
                                        // 如果没有工具调用，直接返回LLM的响应内容
                                        return responseMessage.getContent();
                                }
                        }

                        return "无法分析GitHub个人资料";
                } catch (Exception e) {
                        logger.error("分析GitHub个人资料失败", e);
                        return "GitHub分析过程中发生错误: " + e.getMessage();
                }
        }

        /**
         * 分析GitHub工具调用结果
         */
        private String analyzeGitHubToolResults(Object toolResults) {
                logger.info("开始分析GitHub工具调用结果");
                try {
                        // 将工具结果转换为JSON字符串
                        String toolResultsJson = JsonUtils.toJson(toolResults);

                        // 创建请求，让LLM分析工具结果
                        List<Message> messages = new ArrayList<>();

                        // 用户消息
                        messages.add(Message.builder()
                                        .role("user")
                                        .content("以下是候选人的GitHub分析结果，请对其进行专业评估，重点关注编程活跃度、技术栈多样性、项目质量、贡献度、真实项目，项目价值。输出格式请返回候选者的项目开源情况，项目真实度等对候选者的git评价，简明扼要进行表示\n\n"
                                                        +
                                                        toolResultsJson)
                                        .build());

                        // 构建请求
                        ChatCompletionRequest request = ChatCompletionRequest.builder()
                                        .model("deepseek-ai/DeepSeek-R1")
                                        .messages(messages)
                                        .build();

                        // 发送请求
                        ChatCompletionResponse response = llmService.getChatCompletion(request);

                        if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                                return response.getChoices().get(0).getMessage().getContent();
                        }

                        return "无法分析GitHub工具调用结果";
                } catch (Exception e) {
                        logger.error("分析GitHub工具调用结果失败", e);
                        return "分析GitHub数据时发生错误: " + e.getMessage();
                }
        }

        /**
         * 发送带图片URL的多模态请求
         * 
         * @param imageUrl 图片URL
         * @param prompt   提示词
         * @return 模型响应内容
         */
        private String sendMultimodalRequest(String imageUrl, String prompt) {

                UrlChatCompletionRequest request = UrlChatCompletionRequest.createRequest(Arrays.asList(imageUrl),
                                prompt);
                request.setModel("Qwen/QVQ-72B-Preview");
                ChatCompletionResponse response = llmService.getChatCompletion(request);
                return response.getChoices().get(0).getMessage().getContent();
        }

        /**
         * 分析简历排版
         */
        private String analyzeLayoutAndExtractContent(String resumeUrl) {
                logger.info("开始分析简历排版: {}", resumeUrl);
                String prompt = "你是一个专业的简历排版分析专家。请检查简历中的排版格式问题，如果没有问题则输出排版优秀。如果有问题则简明扼要指出排版的问题";
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
                String prompt = "提取文件中的所有内容并返回";
                String result = sendMultimodalRequest(resumeUrl, prompt);
                logger.info("简历内容解析完成，内容长度: {}", result.length());
                return result;
        }

        /**
         * 评价简历
         * 使用大模型根据提取的内容评价简历质量
         */
        private String evaluateResume(String extractedContent, String githubAnalysis) {
                List<Message> messages = new ArrayList<>();

                int currentYear = java.time.Year.now().getValue();

                // 系统消息
                messages.add(Message.builder()
                                .role("system")
                                .content("你是一个专业的后端开发招聘筛选专家。请根据简历内容和GitHub分析结果进行严格筛选，判断候选人是否适合后端开发岗位，筛选评估过程如下：\n\n" +
                                                "第一步：根据简历中的毕业时间判断候选人是校招生还是社招生\n" +
                                                "- 当前时间是" + currentYear + "年\n" +
                                                "- 如果候选人尚未毕业（毕业时间在" + currentYear + "年或之后），则为校招生/应届生\n" +
                                                "- 如果候选人已经毕业（毕业时间在" + currentYear + "年之前），则为社招生\n\n" +

                                                "第二步：根据候选人类型进行针对性筛选\n" +
                                                "【校招生筛选标准】\n" +
                                                "1. 学校背景：是否来自计算机/软件工程等相关专业的985/211高校，或计算机专业较强的一本院校\n" +
                                                "2. 编程基础：是否掌握数据结构、算法、操作系统、计算机网络等基础知识\n" +
                                                "3. 实习经历：是否有大厂/知名互联网公司后端开发实习经验，负责的模块是否有技术难度\n" +
                                                "4. 项目经验：是否有完整的后端项目开发经验，解决过什么技术难题，是否有亮点\n" +
                                                "4.1 项目经验：项目不可是太简单的项目，例如：没有任何技术解决难题\n" +
                                                "4.2 项目经验：项目描述不可是用技术涵盖整个项目，可以理解为项目的描述用在任何一个项目上都可以描述，这种项目是减分项\n" +
                                                "5. 竞赛经历：是否参加过ACM、蓝桥杯等算法竞赛并取得优异成绩\n" +
                                                "6. 技术栈：是否熟悉至少一种主流后端语言(Java/Go/Python等)和相关框架\n" +
                                                "7. 学习能力：是否有自学能力，能否快速掌握新技术\n" +
                                                "8. 开源经历：GitHub活跃度和贡献\n\n" +

                                                "【社招生筛选标准】\n" +
                                                "1. 工作经历：是否有知名互联网公司或技术导向公司的后端开发经验，工作年限是否充足\n" +
                                                "2. 项目经验：负责过的项目规模和复杂度，是否有架构设计经验，解决过的技术难题\n" +
                                                "3. 技术深度：是否对使用的技术栈有深入理解，能否解决疑难问题\n" +
                                                "4. 技术广度：是否了解分布式系统、高并发、高可用架构等后端核心技术领域\n" +
                                                "5. 系统设计：是否有大型系统设计经验，对系统性能、可扩展性的思考\n" +
                                                "6. 团队协作：是否有团队管理或技术带头人经验，技术影响力如何\n" +
                                                "7. 问题解决：是否能独立解决复杂技术问题，有没有技术攻坚案例\n" +
                                                "8. 开源经历：GitHub活跃度和贡献\n\n" +

                                                "第三步：给出筛选结论\n" +
                                                "请直接给出筛选结果，包括：\n" +
                                                "1. 候选人类型：校招生/社招生\n" +
                                                "2. 技术能力评分：1-100分\n" +
                                                "3. 技术优势：列出3-5条关键优势\n" +
                                                "4. 技术不足：列出1-3条明显不足\n" +
                                                "5. 面试建议：是否推荐进入面试环节，如推荐，建议询问哪些技术点\n\n" +

                                                "筛选报告要简洁有力，直击重点，避免冗长描述。务必保持客观专业，严格筛选，确保只有真正符合后端开发要求的候选人才能通过。")
                                .build());

                // 用户消息
                messages.add(Message.builder()
                                .role("user")
                                .content("简历内容：\n" + extractedContent + "\n\nGitHub分析：\n" + githubAnalysis)
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
}