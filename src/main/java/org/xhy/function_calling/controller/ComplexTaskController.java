package org.xhy.function_calling.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xhy.function_calling.service.ComplexTaskService;

import java.util.HashMap;
import java.util.Map;

/**
 * 复杂任务控制器
 * 提供处理复杂任务的API接口
 */
@RestController
@RequestMapping("/api/complex-task")
public class ComplexTaskController {

    private static final Logger logger = LoggerFactory.getLogger(ComplexTaskController.class);

    private final ComplexTaskService complexTaskService;

    public ComplexTaskController(ComplexTaskService complexTaskService) {
        this.complexTaskService = complexTaskService;
    }

    /**
     * 提交复杂任务
     * 
     * @param query 用户查询/任务描述
     * @return 任务ID和相关信息
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitTask(@RequestParam String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "任务描述不能为空"));
        }

        logger.info("收到复杂任务请求: {}", query);

        String taskId = complexTaskService.processComplexTask(query);

        Map<String, Object> response = new HashMap<>();
        response.put("task_id", taskId);
        response.put("status", "processing");
        response.put("message", "任务已提交并开始处理");

        return ResponseEntity.ok(response);
    }

    /**
     * 获取任务进度
     * 
     * @param taskId 任务ID
     * @return 任务进度信息
     */
    @GetMapping("/progress/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskProgress(@PathVariable String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "任务ID不能为空"));
        }

        logger.info("请求任务[{}]的进度", taskId);

        Map<String, Object> progressInfo = complexTaskService.getTaskProgress(taskId);

        return ResponseEntity.ok(progressInfo);
    }

    /**
     * 取消任务
     * 
     * @param taskId 任务ID
     * @return 操作结果
     */
    @DeleteMapping("/cancel/{taskId}")
    public ResponseEntity<Map<String, Object>> cancelTask(@PathVariable String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "任务ID不能为空"));
        }

        logger.info("请求取消任务[{}]", taskId);

        Map<String, Object> result = complexTaskService.cancelTask(taskId);

        return ResponseEntity.ok(result);
    }
}