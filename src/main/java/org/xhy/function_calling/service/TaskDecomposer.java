package org.xhy.function_calling.service;

import java.util.List;

/**
 * 任务分解器接口
 * 负责将复杂任务分解为可执行的子任务列表
 */
public interface TaskDecomposer {

    /**
     * 将用户输入的复杂任务分解为多个子任务
     * 
     * @param userQuery 用户输入的复杂任务描述
     * @return 子任务列表
     */
    List<String> decomposeTask(String userQuery);
}