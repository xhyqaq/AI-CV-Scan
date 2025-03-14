package org.xhy.function_calling.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GitHub 分析结果（简化版）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubAnalysisResult {

    /**
     * 用户列表
     */
    private List<UserInfo> users;

    /**
     * 原创项目列表
     */
    private List<RepoInfo> originalRepositories;

    /**
     * Fork 的项目列表
     */
    private List<RepoInfo> forkedRepositories;

    /**
     * 分析结果消息
     */
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        /**
         * 用户名
         */
        private String username;

        /**
         * 用户描述
         */
        private String bio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepoInfo {
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
         * 是否有 README
         */
        private boolean hasReadme;

        /**
         * 提交次数
         */
        private int commitCount;

        /**
         * 是否是真实项目
         */
        private boolean isRealProject;

        /**
         * 是否是 fork 的仓库
         */
        private boolean isFork;

        /**
         * 对于 fork 的仓库，是否有自己的贡献
         * 如果 ahead > 0，表示有自己的贡献
         */
        private boolean hasOwnContributions;

        /**
         * 领先原仓库的提交数
         */
        private int aheadCount;

        /**
         * 落后原仓库的提交数
         */
        private int behindCount;
    }
}