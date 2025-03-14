package org.xhy.function_calling.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.xhy.function_calling.tool.ToolDefinition;

import java.util.HashMap;
import java.util.Map;

/**
 * Echo工具定义
 * 一个简单的测试工具，返回输入的参数
 */
@Component
public class EchoToolDefinition implements ToolDefinition {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "echo";
    }

    @Override
    public String getDescription() {
        return "一个简单的回显工具，返回输入的参数";
    }

    @Override
    public Map<String, Object> getParametersDefinition() {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> messageProperty = new HashMap<>();
        messageProperty.put("type", "string");
        messageProperty.put("description", "要回显的消息");

        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of("message", messageProperty));
        schema.put("required", new String[] { "message" });

        return schema;
    }

    @Override
    public Object execute(String parameters) {
        try {
            JsonNode paramsNode = objectMapper.readTree(parameters);
            String message = paramsNode.has("message") ? paramsNode.get("message").asText() : "No message provided";

            Map<String, Object> result = new HashMap<>();
            result.put("message", message);
            result.put("timestamp", System.currentTimeMillis());
            result.put("echo", true);

            return result;
        } catch (Exception e) {
            throw new RuntimeException("执行echo工具时出错: " + e.getMessage(), e);
        }
    }
}