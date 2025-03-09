package org.xhy.function_calling.service;

import java.util.Map;

/**
 * 任务执行器接口
 * 负责执行子任务并返回执行结果
 */
public interface TaskExecutor {

    /**
     * 执行单个子任务
     * 
     * @param subtask 子任务描述
     * @param context 任务上下文，包含之前子任务的执行结果等信息
     * @return 执行结果
     */
    Map<String, Object> executeTask(String subtask, Map<String, Object> context);

    /**
     * 获取当前任务执行的进度信息
     *
     * @return 进度信息，包含已完成任务数、总任务数、当前执行的任务等
     */
    Map<String, Object> getProgressInfo();
}