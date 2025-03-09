package org.xhy.function_calling.tool.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.xhy.function_calling.service.SearchService;
import org.xhy.function_calling.tool.ToolDefinition;
import org.xhy.function_calling.util.JsonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用搜索工具
 * 通过依赖注入的SearchService实现搜索功能，支持各种搜索引擎
 */
@Service
public class SearchTool implements ToolDefinition {

    private static final Logger logger = LoggerFactory.getLogger(SearchTool.class);

    private final SearchService searchService;

    public SearchTool(@Qualifier("googleSearchService") SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public String getName() {
        return "google_search";
    }

    @Override
    public String getDescription() {
        return "通过" + searchService.getProviderName() + "搜索引擎搜索信息，用于查询旅游、天气、交通、住宿等信息";
    }

    @Override
    public Map<String, Object> getParametersDefinition() {
        Map<String, Object> parametersDefinition = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();

        // 定义查询参数
        Map<String, Object> queryParam = new HashMap<>();
        queryParam.put("type", "string");
        queryParam.put("description", "搜索查询内容");
        properties.put("query", queryParam);

        // 定义结果数量参数
        Map<String, Object> numParam = new HashMap<>();
        numParam.put("type", "integer");
        numParam.put("description", "返回的结果数量，默认为3");
        numParam.put("default", 3);
        properties.put("num", numParam);

        parametersDefinition.put("type", "object");
        parametersDefinition.put("properties", properties);
        parametersDefinition.put("required", new String[] { "query" });

        return parametersDefinition;
    }

    @Override
    public Object execute(String arguments) {
        try {
            // 使用JsonUtils解析参数
            Map<String, Object> params = JsonUtils.fromJson(arguments, Map.class);
            String query = (String) params.get("query");
            int num = params.containsKey("num") ? ((Number) params.get("num")).intValue() : 3;

            logger.info("执行搜索工具, 查询: {}, 数量: {}", query, num);

            // 调用搜索服务执行搜索
            return searchService.search(query, num);

        } catch (Exception e) {
            logger.error("执行搜索工具时发生异常", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "搜索执行异常: " + e.getMessage());
            return errorResult;
        }
    }
}