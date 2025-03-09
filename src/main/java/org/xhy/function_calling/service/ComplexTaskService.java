package org.xhy.function_calling.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.function_calling.service.impl.LLMTaskExecutor;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 复杂任务处理服务
 * 负责管理复杂任务的整个生命周期，包括任务分解、执行和结果整合
 */
@Service
public class ComplexTaskService {

    private static final Logger logger = LoggerFactory.getLogger(ComplexTaskService.class);

    private final TaskDecomposer taskDecomposer;
    private final LLMTaskExecutor taskExecutor;
    private final ResultPolisher resultPolisher;

    // 存储正在执行的任务
    private final Map<String, CompletableFuture<Map<String, Object>>> runningTasks = new HashMap<>();

    // 存储任务进度信息，包括子任务列表
    private final Map<String, Map<String, Object>> taskProgressMap = new ConcurrentHashMap<>();

    public ComplexTaskService(TaskDecomposer taskDecomposer, LLMTaskExecutor taskExecutor,
            ResultPolisher resultPolisher) {
        this.taskDecomposer = taskDecomposer;
        this.taskExecutor = taskExecutor;
        this.resultPolisher = resultPolisher;
    }

    /**
     * 处理复杂任务请求
     * 
     * @param userQuery 用户查询/任务描述
     * @return 任务ID，用于后续查询进度和结果
     */
    public String processComplexTask(String userQuery) {
        // 生成唯一任务ID
        String taskId = UUID.randomUUID().toString();

        // 初始化任务进度信息
        Map<String, Object> initialProgress = new HashMap<>();
        initialProgress.put("task_id", taskId);
        initialProgress.put("original_query", userQuery);
        initialProgress.put("status", "initializing");
        initialProgress.put("progress_percentage", 0.0);
        taskProgressMap.put(taskId, initialProgress);

        // 启动异步任务处理
        CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync(() -> {
            try {
                // 1. 任务分解
                List<String> subtasks = taskDecomposer.decomposeTask(userQuery);
                logger.info("任务[{}]已分解为{}个子任务", taskId, subtasks.size());

                // 更新任务进度信息，立即添加子任务列表以便前端可以显示
                Map<String, Object> progressAfterDecomposition = taskProgressMap.get(taskId);
                progressAfterDecomposition.put("subtasks", subtasks);
                progressAfterDecomposition.put("status", "in_progress");
                progressAfterDecomposition.put("current_task", subtasks.isEmpty() ? "" : subtasks.get(0));
                progressAfterDecomposition.put("completed_tasks", 0);
                progressAfterDecomposition.put("total_tasks", subtasks.size());
                taskProgressMap.put(taskId, progressAfterDecomposition);

                // 设置任务总数
                taskExecutor.setTotalTasks(subtasks.size());

                // 2. 逐一执行子任务，最后一个子任务(结果润色)除外
                Map<String, Object> context = new HashMap<>();
                context.put("原始任务", userQuery);

                List<Map<String, Object>> results = new ArrayList<>();

                // 执行除最后一个子任务(结果润色)外的所有子任务
                for (int i = 0; i < subtasks.size() - 1; i++) {
                    String subtask = subtasks.get(i);

                    // 更新当前执行的子任务
                    Map<String, Object> currentProgress = taskProgressMap.get(taskId);
                    currentProgress.put("current_task", subtask);
                    taskProgressMap.put(taskId, currentProgress);

                    // 执行子任务
                    Map<String, Object> subtaskResult = taskExecutor.executeTask(subtask, context);
                    results.add(subtaskResult);

                    // 更新上下文
                    context.put("子任务" + results.size() + "结果", subtaskResult);

                    // 更新任务进度
                    Map<String, Object> updatedProgress = taskProgressMap.get(taskId);
                    updatedProgress.put("completed_tasks", i + 1);
                    // 计算进度时保留最后10%给结果润色阶段
                    double progressPercentage = (double) (i + 1) / subtasks.size() * 90;
                    updatedProgress.put("progress_percentage", progressPercentage);
                    taskProgressMap.put(taskId, updatedProgress);
                }

                // 3. 执行最后的结果润色子任务
                if (!subtasks.isEmpty()) {
                    String polishingTask = subtasks.get(subtasks.size() - 1);

                    // 更新当前执行的子任务为润色任务
                    Map<String, Object> currentProgress = taskProgressMap.get(taskId);
                    currentProgress.put("current_task", polishingTask);
                    taskProgressMap.put(taskId, currentProgress);

                    // 润色结果，生成用户友好的输出
                    String polishedResult = resultPolisher.polishResult(userQuery, subtasks, results);

                    // 更新任务进度为100%
                    Map<String, Object> finalProgress = taskProgressMap.get(taskId);
                    finalProgress.put("completed_tasks", subtasks.size());
                    finalProgress.put("progress_percentage", 100.0);
                    taskProgressMap.put(taskId, finalProgress);

                    // 4. 整合最终结果
                    Map<String, Object> finalResult = new HashMap<>();
                    finalResult.put("task_id", taskId);
                    finalResult.put("original_query", userQuery);
                    finalResult.put("subtasks", subtasks);
                    finalResult.put("results", results);
                    finalResult.put("polished_result", polishedResult);
                    finalResult.put("status", "completed");
                    finalResult.put("progress_percentage", 100.0);
                    finalResult.put("completed_tasks", subtasks.size());
                    finalResult.put("total_tasks", subtasks.size());

                    // 更新进度信息
                    taskProgressMap.put(taskId, finalResult);

                    logger.info("任务[{}]已完成", taskId);

                    return finalResult;
                } else {
                    // 没有子任务的情况，直接返回结果
                    Map<String, Object> emptyResult = new HashMap<>();
                    emptyResult.put("task_id", taskId);
                    emptyResult.put("original_query", userQuery);
                    emptyResult.put("status", "completed");
                    emptyResult.put("message", "无法将任务分解为子任务");

                    taskProgressMap.put(taskId, emptyResult);

                    return emptyResult;
                }
            } catch (Exception e) {
                logger.error("处理任务[{}]时出错", taskId, e);

                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("task_id", taskId);
                errorResult.put("original_query", userQuery);
                errorResult.put("status", "failed");
                errorResult.put("error", e.getMessage());

                // 更新进度信息
                taskProgressMap.put(taskId, errorResult);

                return errorResult;
            }
        });

        // 存储运行中的任务
        runningTasks.put(taskId, future);

        return taskId;
    }

    /**
     * 获取任务进度
     * 
     * @param taskId 任务ID
     * @return 任务进度信息
     */
    public Map<String, Object> getTaskProgress(String taskId) {
        // 首先检查进度映射中是否有该任务
        Map<String, Object> progressInfo = taskProgressMap.get(taskId);
        if (progressInfo != null) {
            return new HashMap<>(progressInfo); // 返回一个副本以避免并发修改问题
        }

        CompletableFuture<Map<String, Object>> future = runningTasks.get(taskId);

        if (future == null) {
            Map<String, Object> notFoundResult = new HashMap<>();
            notFoundResult.put("task_id", taskId);
            notFoundResult.put("status", "not_found");
            return notFoundResult;
        }

        if (future.isDone()) {
            try {
                Map<String, Object> result = future.get();

                // 任务已完成，从运行中任务列表中移除
                if ("completed".equals(result.get("status")) || "failed".equals(result.get("status"))) {
                    runningTasks.remove(taskId);
                }

                return result;
            } catch (Exception e) {
                logger.error("获取任务[{}]结果时出错", taskId, e);

                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("task_id", taskId);
                errorResult.put("status", "error");
                errorResult.put("error", "获取任务结果时出错：" + e.getMessage());

                // 出错时也从运行中任务列表移除
                runningTasks.remove(taskId);

                return errorResult;
            }
        } else {
            // 任务仍在进行中但没有进度信息，返回默认进度
            Map<String, Object> defaultProgress = new HashMap<>();
            defaultProgress.put("task_id", taskId);
            defaultProgress.put("status", "in_progress");
            defaultProgress.putAll(taskExecutor.getProgressInfo());

            return defaultProgress;
        }
    }

    /**
     * 取消正在执行的任务
     * 
     * @param taskId 任务ID
     * @return 操作结果
     */
    public Map<String, Object> cancelTask(String taskId) {
        CompletableFuture<Map<String, Object>> future = runningTasks.get(taskId);

        if (future == null) {
            Map<String, Object> notFoundResult = new HashMap<>();
            notFoundResult.put("task_id", taskId);
            notFoundResult.put("status", "not_found");
            return notFoundResult;
        }

        boolean cancelled = future.cancel(true);
        runningTasks.remove(taskId);

        // 更新任务状态
        Map<String, Object> currentProgress = taskProgressMap.get(taskId);
        if (currentProgress != null) {
            currentProgress.put("status", "cancelled");
            taskProgressMap.put(taskId, currentProgress);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("task_id", taskId);
        result.put("status", cancelled ? "cancelled" : "cancel_failed");

        return result;
    }
}