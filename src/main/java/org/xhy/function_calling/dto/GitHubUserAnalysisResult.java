package org.xhy.function_calling.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GitHub 用户分析结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubUserAnalysisResult {

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户全名
     */
    private String name;

    /**
     * 用户简介
     */
    private String bio;

    /**
     * 公开仓库数量
     */
    private int publicRepos;

    /**
     * 粉丝数量
     */
    private int followers;

    /**
     * 关注数量
     */
    private int following;

    /**
     * 用户创建时间
     */
    private String createdAt;

    /**
     * 最后更新时间
     */
    private String updatedAt;

    /**
     * 用户主页 URL
     */
    private String url;

    /**
     * 用户所在地
     */
    private String location;

    /**
     * 用户公司
     */
    private String company;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 热门仓库列表（最多显示前5个）
     */
    private List<GitHubRepoAnalysisResult> popularRepositories;

    /**
     * 分析结果消息
     */
    private String message;
}