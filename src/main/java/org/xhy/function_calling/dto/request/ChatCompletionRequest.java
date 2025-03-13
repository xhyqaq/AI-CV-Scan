package org.xhy.function_calling.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 聊天完成请求对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest {
    private String model;
    private List<Message> messages;
    private Boolean stream;
    private Integer max_tokens;
    private Object stop;
    private Double temperature;
    private Double top_p;
    private Integer top_k;
    private Double frequency_penalty;
    private Integer n;
    private ResponseFormat response_format;
    private List<Tool> tools;

    /**
     * 文件URL，用于处理文件类型的请求
     */
    private String file_url;
}