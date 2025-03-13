package org.xhy.function_calling.llm;

import org.xhy.function_calling.dto.request.ChatCompletionRequest;
import org.xhy.function_calling.dto.request.UrlChatCompletionRequest;
import org.xhy.function_calling.dto.response.ChatCompletionResponse;

/**
 * LLM服务接口
 * 定义与大语言模型交互的通用接口
 */
public interface LLMService {

    /**
     * 发送聊天请求并获取响应
     * 
     * @param userQuery 用户查询
     * @return 聊天完成响应
     * @deprecated 使用 {@link #getChatCompletion(ChatCompletionRequest)} 替代
     */
    @Deprecated
    ChatCompletionResponse getChatCompletion(String userQuery);

    /**
     * 发送聊天请求并获取响应
     * 
     * @param request 聊天完成请求
     * @return 聊天完成响应
     */
    ChatCompletionResponse getChatCompletion(ChatCompletionRequest request);

    /**
     * 发送url聊天请求
     * @param request
     * @return
     */
    ChatCompletionResponse getChatCompletion(UrlChatCompletionRequest request);

    /**
     * 获取LLM提供商名称
     * 
     * @return 提供商名称
     */
    String getProviderName();
}