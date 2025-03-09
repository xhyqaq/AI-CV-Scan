# 通义千问 LLM 服务集成

本文档说明如何在系统中配置和使用阿里云通义千问 LLM 服务。

## 功能特点

- 支持通义千问各种模型（qwen-turbo, qwen-plus, qwen-max等）
- 支持 Function Calling（工具调用）功能
- 使用 OpenAI 兼容接口，简化集成过程

## 配置步骤

1. 注册阿里云账号并开通[大模型服务平台百炼](https://www.aliyun.com/product/bailian)服务
2. 获取通义千问 API 密钥（API Key）
3. 在 `application.properties` 中引入通义千问配置文件: 
   ```properties
   spring.profiles.include=qwen
   ```
4. 更新 `application-qwen.properties` 文件中的配置:
   ```properties
   # 替换为您的实际 API 密钥
   qwen.api.token=your_qwen_api_key_here
   
   # 可选：修改使用的模型
   qwen.api.model=qwen-plus
   ```

## 使用方法

服务将自动注册到Spring容器中。您可以通过依赖注入使用该服务:

```java
@Autowired
private LLMService llmService; // 自动注入配置的LLM服务

// 或者明确指定通义千问服务
@Autowired
@Qualifier("qwenLLMService")
private LLMService qwenService;
```

调用示例:

```java
String userQuery = "帮我查询今天的天气";
ChatCompletionResponse response = llmService.getChatCompletion(userQuery);

// 处理响应...
```

## Function Calling 功能

通义千问支持 Function Calling 功能，允许模型调用预定义的工具。系统会自动从 `ToolRegistry` 中注册的工具定义生成工具描述，并在请求中提供给模型。

如需使用 Function Calling 功能，请确保:

1. 正确实现并注册了 `ToolDefinition` 接口的工具类
2. 确保使用支持 Function Calling 的模型（推荐 qwen-plus 或 qwen-max）

## 更多资源

- [通义千问API文档](https://help.aliyun.com/zh/model-studio/developer-reference/use-qwen-by-calling-api)
- [Function Calling 开发指南](https://help.aliyun.com/zh/model-studio/user-guide/qwen-function-calling) 