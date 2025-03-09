package org.xhy.function_calling.tool.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.function_calling.service.tool.SearchToolService;
import org.xhy.function_calling.tool.ToolDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 搜索工具
 */
@Component
public class SearchTool implements ToolDefinition {

    private static final Logger logger = LoggerFactory.getLogger(SearchTool.class);

    private final SearchToolService searchService;

    public SearchTool(SearchToolService searchService) {
        this.searchService = searchService;
    }

    @Override
    public String getName() {
        return "get_search";
    }

    @Override
    public String getDescription() {
        return "搜索互联网上的信息";
    }

    @Override
    public Map<String, Object> getParametersDefinition() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        parameters.put("required", List.of("query"));

        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> queryProperty = new HashMap<>();
        queryProperty.put("type", "string");
        queryProperty.put("description", "搜索关键词");

        properties.put("query", queryProperty);
        parameters.put("properties", properties);

        return parameters;
    }

    @Override
    public Object execute(String arguments) {
        logger.info("执行搜索，参数: {}", arguments);
        return searchService.search(arguments);
    }
}