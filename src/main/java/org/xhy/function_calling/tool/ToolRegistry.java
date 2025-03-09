package org.xhy.function_calling.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.xhy.function_calling.dto.request.Tool;
import org.xhy.function_calling.dto.request.ToolFunction;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册表
 * 负责自动注册和管理所有工具
 */
@Component
public class ToolRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ToolRegistry.class);

    private final Map<String, ToolDefinition> toolDefinitions = new ConcurrentHashMap<>();

    private final ApplicationContext applicationContext;

    @Autowired
    public ToolRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 初始化时自动注册所有工具
     */
    @PostConstruct
    public void init() {
        // 获取所有实现了ToolDefinition接口的Bean
        Map<String, ToolDefinition> toolBeans = applicationContext.getBeansOfType(ToolDefinition.class);

        // 注册所有工具
        toolBeans.forEach((beanName, tool) -> {
            registerTool(tool);
        });

        logger.info("已自动注册 {} 个工具", toolDefinitions.size());
    }

    /**
     * 注册工具
     * 
     * @param tool 工具定义
     */
    public void registerTool(ToolDefinition tool) {
        String toolName = tool.getName();
        toolDefinitions.put(toolName, tool);
        logger.info("注册工具: {}", toolName);
    }

    /**
     * 获取所有工具定义
     * 
     * @return 工具定义列表
     */
    public List<ToolDefinition> getAllTools() {
        return new ArrayList<>(toolDefinitions.values());
    }

    /**
     * 获取指定名称的工具
     * 
     * @param toolName 工具名称
     * @return 工具定义，如果不存在则返回null
     */
    public ToolDefinition getTool(String toolName) {
        return toolDefinitions.get(toolName);
    }

    /**
     * 执行工具
     * 
     * @param toolName  工具名称
     * @param arguments 参数
     * @return 执行结果
     */
    public Object executeTool(String toolName, String arguments) {
        ToolDefinition tool = getTool(toolName);
        if (tool == null) {
            logger.warn("未找到工具: {}", toolName);
            return null;
        }

        logger.info("执行工具: {}, 参数: {}", toolName, arguments);
        try {
            return tool.execute(arguments);
        } catch (Exception e) {
            logger.error("执行工具 {} 时出错: {}", toolName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取所有工具的API定义
     * 用于构建LLM请求
     * 
     * @return 工具API定义列表
     */
    public List<Tool> getToolsForLLM() {
        List<Tool> tools = new ArrayList<>();

        for (ToolDefinition toolDef : getAllTools()) {
            ToolFunction function = ToolFunction.builder()
                    .name(toolDef.getName())
                    .description(toolDef.getDescription())
                    .parameters(toolDef.getParametersDefinition())
                    .strict(false)
                    .build();

            Tool tool = Tool.builder()
                    .type("function")
                    .function(function)
                    .build();

            tools.add(tool);
        }

        return tools;
    }
}