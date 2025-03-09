package org.xhy.function_calling.service;

import java.util.Map;

/**
 * 搜索服务接口
 * 定义搜索功能的抽象，允许多种实现（如Google搜索、百度搜索等）
 */
public interface SearchService {

    /**
     * 执行搜索操作
     * 
     * @param query 搜索查询
     * @param limit 返回结果数量限制
     * @return 搜索结果
     */
    Map<String, Object> search(String query, int limit);

    /**
     * 获取搜索服务提供商名称
     * 
     * @return 提供商名称
     */
    String getProviderName();
}