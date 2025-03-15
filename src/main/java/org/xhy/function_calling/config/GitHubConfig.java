package org.xhy.function_calling.config;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * GitHub API 配置类
 */
@Configuration
public class GitHubConfig {

    private static final Logger logger = LoggerFactory.getLogger(GitHubConfig.class);

    @Value("${github.token:}")
    private String githubToken;

    /**
     * 创建 GitHub API 客户端
     * 如果提供了令牌，则使用认证访问；否则使用匿名访问
     */
    @Bean
    public GitHub gitHub() throws IOException {
        if (githubToken != null && !githubToken.trim().isEmpty()) {
            logger.info("使用认证方式连接 GitHub API");
            return new GitHubBuilder().withOAuthToken(githubToken).build();
        } else {
            logger.info("GitHub token 未提供或为空，使用匿名方式连接 GitHub API");
            return GitHub.connectAnonymously();
        }
    }
}