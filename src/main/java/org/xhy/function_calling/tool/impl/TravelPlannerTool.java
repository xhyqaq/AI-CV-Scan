package org.xhy.function_calling.tool.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.function_calling.tool.ToolDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 旅行规划工具
 * 用于提供旅游景点信息、餐厅推荐、行程安排等功能
 */
@Service
public class TravelPlannerTool implements ToolDefinition {

    private static final Logger logger = LoggerFactory.getLogger(TravelPlannerTool.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "get_travel_planner";
    }

    @Override
    public String getDescription() {
        return "根据用户需求提供旅游规划服务，包括景点推荐、餐厅推荐、行程安排等";
    }

    @Override
    public Map<String, Object> getParametersDefinition() {
        Map<String, Object> parametersDefinition = new HashMap<>();

        Map<String, Object> properties = new HashMap<>();

        // action参数定义
        Map<String, Object> actionParam = new HashMap<>();
        actionParam.put("type", "string");
        actionParam.put("description",
                "要执行的操作：get_attractions(获取景点信息)、get_restaurants(获取餐厅信息)、suggest_itinerary(推荐行程)");
        properties.put("action", actionParam);

        // city参数定义
        Map<String, Object> cityParam = new HashMap<>();
        cityParam.put("type", "string");
        cityParam.put("description", "目标城市名称");
        properties.put("city", cityParam);

        // days参数定义
        Map<String, Object> daysParam = new HashMap<>();
        daysParam.put("type", "integer");
        daysParam.put("description", "行程天数");
        properties.put("days", daysParam);

        // preferences参数定义
        Map<String, Object> preferencesParam = new HashMap<>();
        preferencesParam.put("type", "string");
        preferencesParam.put("description", "用户偏好，如'历史景点'、'美食'、'购物'等");
        properties.put("preferences", preferencesParam);

        parametersDefinition.put("type", "object");
        parametersDefinition.put("properties", properties);
        parametersDefinition.put("required", List.of("action", "city"));

        return parametersDefinition;
    }

    @Override
    public Object execute(String arguments) {
        try {
            // 解析JSON参数
            Map<String, Object> parameters = objectMapper.readValue(arguments, Map.class);

            String action = (String) parameters.get("action");
            String city = (String) parameters.get("city");

            if (action == null || city == null) {
                return Map.of(
                        "status", "error",
                        "message", "缺少必要参数");
            }

            Integer days = parameters.containsKey("days") ? Integer.parseInt(parameters.get("days").toString()) : 1;
            String preferences = (String) parameters.get("preferences");

            logger.info("执行旅行工具: {} - {} - {} 天 - 偏好: {}", action, city, days, preferences);

            switch (action) {
                case "get_attractions":
                    return getAttractions(city, preferences);
                case "get_restaurants":
                    return getRestaurants(city, preferences);
                case "suggest_itinerary":
                    return suggestItinerary(city, days, preferences);
                default:
                    return Map.of(
                            "status", "error",
                            "message", "不支持的操作: " + action);
            }
        } catch (JsonProcessingException e) {
            logger.error("解析参数出错", e);
            return Map.of(
                    "status", "error",
                    "message", "参数解析错误: " + e.getMessage());
        } catch (Exception e) {
            logger.error("执行旅行工具出错", e);
            return Map.of(
                    "status", "error",
                    "message", "执行出错: " + e.getMessage());
        }
    }

    /**
     * 获取城市景点信息
     */
    private Map<String, Object> getAttractions(String city, String preferences) {
        Map<String, Object> result = new HashMap<>();

        // 这里使用模拟数据，实际应用中可连接到旅游API或数据库
        if ("北京".equals(city)) {
            Map<String, String> attractions = new HashMap<>();
            attractions.put("故宫博物院", "世界上规模最大的宫殿建筑群，中国明清两代的皇家宫殿");
            attractions.put("长城", "中国古代伟大的防御工程，世界文化遗产");
            attractions.put("天坛", "明清两代皇帝祭天的场所，中国现存最大的祭天建筑群");
            attractions.put("颐和园", "清代大型皇家园林，保存最完整的皇家行宫御苑");
            attractions.put("南锣鼓巷", "北京最古老的胡同之一，保存了老北京的传统风貌");
            attractions.put("798艺术区", "前身为电子工业的老厂房，现为当代艺术展览区");

            result.put("city", city);
            result.put("attractions", attractions);
            result.put("status", "success");
        } else {
            result.put("status", "error");
            result.put("message", "暂不支持该城市的景点信息: " + city);
        }

        return result;
    }

    /**
     * 获取城市餐厅推荐
     */
    private Map<String, Object> getRestaurants(String city, String preferences) {
        Map<String, Object> result = new HashMap<>();

        // 模拟数据
        if ("北京".equals(city)) {
            Map<String, String> restaurants = new HashMap<>();
            restaurants.put("全聚德", "著名的北京烤鸭餐厅，有百年历史");
            restaurants.put("东来顺", "清真涮羊肉老字号");
            restaurants.put("老舍茶馆", "体验京味文化的茶馆");
            restaurants.put("南门涮肉", "老北京特色涮肉");
            restaurants.put("京兆尹", "创新中餐");

            result.put("city", city);
            result.put("restaurants", restaurants);
            result.put("status", "success");
        } else {
            result.put("status", "error");
            result.put("message", "暂不支持该城市的餐厅信息: " + city);
        }

        return result;
    }

    /**
     * 根据天数推荐行程安排
     */
    private Map<String, Object> suggestItinerary(String city, Integer days, String preferences) {
        Map<String, Object> result = new HashMap<>();

        if ("北京".equals(city)) {
            Map<String, Object> itinerary = new HashMap<>();

            if (days >= 1) {
                Map<String, Object> day1 = new HashMap<>();
                day1.put("上午", "游览天安门广场和故宫博物院");
                day1.put("午餐", "在王府井附近品尝老北京小吃");
                day1.put("下午", "游览景山公园，俯瞰紫禁城全景");
                day1.put("晚餐", "品尝全聚德烤鸭");
                itinerary.put("第1天", day1);
            }

            if (days >= 2) {
                Map<String, Object> day2 = new HashMap<>();
                day2.put("上午", "游览八达岭长城");
                day2.put("午餐", "在长城附近农家乐用餐");
                day2.put("下午", "参观奥林匹克公园，欣赏鸟巢和水立方");
                day2.put("晚餐", "在三里屯享用时尚餐厅美食");
                itinerary.put("第2天", day2);
            }

            if (days >= 3) {
                Map<String, Object> day3 = new HashMap<>();
                day3.put("上午", "游览颐和园，欣赏皇家园林");
                day3.put("午餐", "在颐和园附近用餐");
                day3.put("下午", "游览圆明园遗址公园");
                day3.put("晚餐", "在后海酒吧街品尝美食并感受夜景");
                itinerary.put("第3天", day3);
            }

            result.put("city", city);
            result.put("days", days);
            result.put("itinerary", itinerary);
            result.put("status", "success");
        } else {
            result.put("status", "error");
            result.put("message", "暂不支持该城市的行程规划: " + city);
        }

        return result;
    }
}