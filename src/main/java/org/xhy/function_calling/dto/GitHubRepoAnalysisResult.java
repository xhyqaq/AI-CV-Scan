package org.xhy.function_calling.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GitHub 仓库分析结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubRepoAnalysisResult {

    /**
     * 仓库名称
     */
    private String name;

    /**
     * 仓库所有者
     */
    private String owner;

    /**
     * 仓库描述
     */
    private String description;

    /**
     * Star 数量
     */
    private int stars;

    /**
     * Fork 数量
     */
    private int forks;

    /**
     * 是否有 README 文件
     */
    private boolean hasReadme;

    /**
     * 是否是真实项目
     * 基于多个因素判断：是否有代码文件、是否有提交历史、是否有描述等
     */
    private boolean isRealProject;

    /**
     * 仓库创建时间
     */
    private String createdAt;

    /**
     * 最后更新时间
     */
    private String updatedAt;

    /**
     * 仓库语言
     */
    private String language;

    /**
     * 仓库 URL
     */
    private String url;

    /**
     * 分析结果消息
     */
    private String message;
}