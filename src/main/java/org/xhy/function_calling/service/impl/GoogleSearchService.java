package org.xhy.function_calling.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xhy.function_calling.service.SearchService;
import org.xhy.function_calling.util.JsonUtils;

import java.util.*;

/**
 * Google搜索服务实现
 * 提供模拟的Google搜索结果数据
 */
@Service
public class GoogleSearchService implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSearchService.class);

    @Value("${google.api.key:dummy-api-key}")
    private String apiKey;

    @Value("${google.cse.id:dummy-cse-id}")
    private String searchEngineId;

    @Override
    public Map<String, Object> search(String query, int limit) {
        logger.info("执行Google搜索模拟, 查询: {}, 限制: {}", query, limit);

        // 直接返回模拟数据
        return getSimulatedResults(query, limit);
    }

    @Override
    public String getProviderName() {
        return "Google";
    }

    /**
     * 生成模拟搜索结果（用于测试）
     */
    private Map<String, Object> getSimulatedResults(String query, int num) {
        List<Map<String, Object>> results = new ArrayList<>();

        // 从查询中提取城市名称
        String city = extractCityFromQuery(query);

        // 创建模拟景点数据
        if (query.contains("景点") || query.contains("旅游") || query.contains("游玩")) {
            for (int i = 0; i < num; i++) {
                Map<String, Object> result = new HashMap<>();
                result.put("title", city + "热门景点 #" + (i + 1) + " - 旅游指南");
                result.put("link", "https://example.com/travel/" + city + "/attraction" + (i + 1));
                result.put("snippet",
                        city + "必游景点推荐。这个城市拥有丰富的历史文化和自然风景，是旅游度假的理想目的地。游客可以参观历史古迹、博物馆、公园和特色街区等。景点分布在城市各区域，建议合理安排行程。");
                results.add(result);
            }
        }
        // 创建模拟餐厅数据
        else if (query.contains("餐厅") || query.contains("美食") || query.contains("食物") || query.contains("吃")) {
            for (int i = 0; i < num; i++) {
                Map<String, Object> result = new HashMap<>();
                result.put("title", city + "最佳餐厅 #" + (i + 1) + " - 美食指南");
                result.put("link", "https://example.com/food/" + city + "/restaurant" + (i + 1));
                result.put("snippet",
                        city + "美食推荐。这个城市的美食文化丰富多样，有各种特色餐厅和当地小吃。游客可以品尝传统美食、时尚餐厅和街头小吃等。人均消费从30元到300元不等，覆盖各种预算需求。");
                results.add(result);
            }
        }
        // 创建模拟行程数据
        else if (query.contains("行程") || query.contains("攻略") || query.contains("计划")) {
            for (int i = 0; i < num; i++) {
                Map<String, Object> result = new HashMap<>();
                result.put("title", city + (i + 1) + "日游完美行程 - 旅游攻略");
                result.put("link", "https://example.com/trip/" + city + "/itinerary" + (i + 1));
                result.put("snippet", city
                        + "旅游攻略。这个城市适合短期或长期旅行，有各种景点和活动。建议游客合理安排时间，了解当地交通和天气状况，准备充分的旅行预算。一般3日游预算在2000-5000元左右，包含住宿、餐饮和门票费用。");
                results.add(result);
            }
        }
        // 创建模拟天气数据
        else if (query.contains("天气") || query.contains("气温") || query.contains("气候")) {
            for (int i = 0; i < num; i++) {
                Map<String, Object> result = new HashMap<>();
                result.put("title", city + "未来" + (i + 1) + "天天气预报 - 气象信息");
                result.put("link", "https://example.com/weather/" + city + "/forecast" + (i + 1));
                result.put("snippet",
                        city + "近期天气预报。未来几天气温为15-25摄氏度，多云转晴，适宜出行。建议携带轻薄外套，注意防晒。早晚温差较大，请注意增减衣物。降水概率较低，整体天气状况适合户外活动。");
                results.add(result);
            }
        }
        // 创建模拟住宿数据
        else if (query.contains("住宿") || query.contains("酒店") || query.contains("宾馆") || query.contains("民宿")) {
            for (int i = 0; i < num; i++) {
                Map<String, Object> result = new HashMap<>();
                result.put("title", city + "最佳住宿选择 #" + (i + 1) + " - 酒店指南");
                result.put("link", "https://example.com/hotel/" + city + "/recommendation" + (i + 1));
                result.put("snippet", city
                        + "住宿推荐。这个城市有各种住宿选择，从经济型到豪华酒店，从市中心到景区附近。价格区间从200元到2000元每晚。建议提前预订，特别是在旅游旺季。许多酒店提供免费早餐和接送服务，位置便利交通方便。");
                results.add(result);
            }
        }
        // 创建模拟交通数据
        else if (query.contains("交通") || query.contains("出行") || query.contains("公交") || query.contains("地铁")) {
            for (int i = 0; i < num; i++) {
                Map<String, Object> result = new HashMap<>();
                result.put("title", city + "交通出行指南 #" + (i + 1) + " - 出行建议");
                result.put("link", "https://example.com/transport/" + city + "/guide" + (i + 1));
                result.put("snippet", city
                        + "交通指南。城市内部交通便利，拥有完善的公共交通网络，包括地铁、公交、出租车等。建议使用公共交通前往主要景点，避开早晚高峰。也可以考虑租自行车探索市区。从机场到市区有多种交通选择，包括机场快线、机场巴士和出租车。");
                results.add(result);
            }
        }
        // 创建模拟预算数据
        else if (query.contains("预算") || query.contains("花费") || query.contains("消费") || query.contains("价格")) {
            for (int i = 0; i < num; i++) {
                Map<String, Object> result = new HashMap<>();
                result.put("title", city + "旅游预算指南 #" + (i + 1) + " - 消费参考");
                result.put("link", "https://example.com/budget/" + city + "/guide" + (i + 1));
                result.put("snippet", city
                        + "旅游预算参考。3天游玩总预算约2000-5000元/人。住宿：经济型200-500元/晚，中档500-1000元/晚，豪华1000元以上/晚。餐饮：早餐20-50元，午晚餐50-200元/人。交通：公共交通2-10元/次，出租车起步价一般10-20元。景点门票：主要景点50-200元/处。购物和其他费用视个人情况而定。");
                results.add(result);
            }
        }
        // 默认搜索结果
        else {
            for (int i = 0; i < num; i++) {
                Map<String, Object> result = new HashMap<>();
                result.put("title", "关于 " + query + " 的信息 #" + (i + 1));
                result.put("link", "https://example.com/search?q=" + query + "&page=" + (i + 1));
                result.put("snippet", "这是关于 " + query
                        + " 的模拟搜索结果。由于使用的是模拟数据，实际内容可能与查询不完全相关。在实际应用中，这里会显示真实的搜索结果摘要。包含相关的景点、餐饮、住宿、交通等综合信息。");
                results.add(result);
            }
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("status", "success");
        resultMap.put("query", query);
        resultMap.put("results", results);
        resultMap.put("note", "这是模拟数据，仅用于测试。");

        return resultMap;
    }

    /**
     * 从查询中提取城市名称
     * 简单实现，仅用于模拟数据
     */
    private String extractCityFromQuery(String query) {
        // 常见城市列表
        List<String> commonCities = Arrays.asList(
                "北京", "上海", "广州", "深圳", "杭州", "成都", "重庆", "西安", "南京", "武汉",
                "厦门", "青岛", "大连", "苏州", "三亚", "丽江", "拉萨", "哈尔滨", "长沙", "昆明");

        // 查找匹配的城市名称
        for (String city : commonCities) {
            if (query.contains(city)) {
                return city;
            }
        }

        // 默认返回北京
        return "北京";
    }
}