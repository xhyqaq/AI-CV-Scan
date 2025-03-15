package org.xhy.function_calling.tool.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xhy.function_calling.dto.GitHubAnalysisResult;
import org.xhy.function_calling.tool.ToolDefinition;
import org.xhy.function_calling.util.JsonUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class GitHubRepoAnalyzerTool implements ToolDefinition {

    private static final Logger logger = LoggerFactory.getLogger(GitHubRepoAnalyzerTool.class);
    private static final Pattern GITHUB_REPO_URL_PATTERN = Pattern.compile("github\\.com/([^/]+)/([^/]+)");
    private static final Pattern GITHUB_USER_URL_PATTERN = Pattern.compile("github\\.com/([^/]+)/?$");

    // 限制常量
    private static final int MAX_REPOS_PER_USER = 20; // 每个用户最多分析的仓库数
    private static final int REPO_ANALYSIS_TIMEOUT_SECONDS = 10; // 增加超时时间，从5秒改为10秒
    private static final int MAX_THREADS = 3; // 减少最大线程数，避免API限制
    private static final int MAX_COMMITS_TO_CHECK = 30; // 最多检查的提交数量
    private static final int API_RETRY_DELAY_MS = 100; // API请求之间的延迟，避免触发限制

    // GitHub API 客户端
    @Autowired
    private GitHub github;

    @Override
    public String getName() {
        return "analyze_github_info";
    }

    @Override
    public String getDescription() {
        return "分析多个GitHub用户或仓库信息，支持用户主页URL（如 https://github.com/username）和仓库URL（如 https://github.com/username/repository）";
    }

    @Override
    public Map<String, Object> getParametersDefinition() {
        Map<String, Object> parameters = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();

        // urls参数定义 - 接受URL数组
        Map<String, Object> urlsParam = new HashMap<>();
        urlsParam.put("type", "array");
        urlsParam.put("items", Map.of("type", "string"));
        urlsParam.put("description", "GitHub用户或仓库的URL列表，支持多个链接进行批量分析");
        properties.put("urls", urlsParam);

        // 构建参数schema
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", new String[] { "urls" });

        return parameters;
    }

    @Override
    public Object execute(String arguments) {
        try {
            // 解析参数
            Map<String, Object> params = arguments != null && !arguments.isEmpty()
                    ? JsonUtils.fromJson(arguments, Map.class)
                    : new HashMap<>();

            // 获取URL列表
            List<String> urls = (List<String>) params.get("urls");
            if (urls == null || urls.isEmpty()) {
                return GitHubAnalysisResult.builder()
                        .message("未提供GitHub URL")
                        .build();
            }

            Set<GitHubAnalysisResult.UserInfo> users = Collections.synchronizedSet(new HashSet<>());
            Set<GitHubAnalysisResult.RepoInfo> originalRepos = Collections.synchronizedSet(new HashSet<>());
            Set<GitHubAnalysisResult.RepoInfo> forkedRepos = Collections.synchronizedSet(new HashSet<>());
            List<String> errors = Collections.synchronizedList(new ArrayList<>());

            // 记录认证状态
            boolean isAuthenticated = github.isCredentialValid();
            logger.info("GitHub API 认证状态: {}", isAuthenticated ? "已认证" : "匿名");

            // 获取 API 限制信息
            GHRateLimit rateLimit = github.getRateLimit();
            logger.info("GitHub API 限制: {}/{} 请求/小时, 重置时间: {}",
                    rateLimit.getRemaining(), rateLimit.getLimit(), rateLimit.getResetDate());

            // 创建线程池用于并行处理URL
            ExecutorService urlExecutor = Executors.newFixedThreadPool(Math.min(urls.size(), MAX_THREADS));
            List<Future<?>> urlFutures = new ArrayList<>();

            // 并行处理每个URL
            for (String urlItem : urls) {
                urlFutures.add(urlExecutor.submit(() -> {
                    try {
                        // 尝试匹配仓库 URL
                        Matcher repoMatcher = GITHUB_REPO_URL_PATTERN.matcher(urlItem);
                        if (repoMatcher.find()) {
                            String owner = repoMatcher.group(1);
                            String repoName = repoMatcher.group(2);
                            try {
                                analyzeRepositoryWithTimeout(github, owner, repoName, originalRepos, forkedRepos, true);
                            } catch (TimeoutException e) {
                                logger.warn("分析仓库 {}/{} 超时", owner, repoName);
                                errors.add("分析仓库 " + owner + "/" + repoName + " 超时");
                            } catch (Exception e) {
                                logger.warn("分析仓库 {}/{} 出错: {}", owner, repoName, e.getMessage());
                                errors.add("分析仓库 " + owner + "/" + repoName + " 出错: " + e.getMessage());
                            }
                            return;
                        }

                        // 尝试匹配用户 URL
                        Matcher userMatcher = GITHUB_USER_URL_PATTERN.matcher(urlItem);
                        if (userMatcher.find()) {
                            String username = userMatcher.group(1);
                            try {
                                analyzeUser(github, username, users, originalRepos, forkedRepos, true);
                            } catch (Exception e) {
                                logger.warn("分析用户 {} 出错: {}", username, e.getMessage());
                                errors.add("分析用户 " + username + " 出错: " + e.getMessage());
                            }
                            return;
                        }

                        // URL 格式不匹配
                        errors.add("无效的 GitHub URL 格式: " + urlItem);
                    } catch (Exception e) {
                        logger.warn("处理 URL {} 出错: {}", urlItem, e.getMessage());
                        errors.add("处理 URL " + urlItem + " 出错: " + e.getMessage());
                    }
                }));
            }

            // 等待所有URL处理完成
            for (Future<?> future : urlFutures) {
                try {
                    future.get();
                } catch (Exception e) {
                    logger.error("等待URL处理任务完成时出错", e);
                }
            }

            // 关闭线程池
            urlExecutor.shutdown();
            try {
                if (!urlExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    urlExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                urlExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            // 再次获取 API 限制信息，查看剩余请求数
            try {
                GHRateLimit updatedRateLimit = github.getRateLimit();
                logger.info("分析完成后 GitHub API 限制: {}/{} 请求/小时",
                        updatedRateLimit.getRemaining(), updatedRateLimit.getLimit());
            } catch (Exception e) {
                logger.warn("无法获取更新后的 API 限制信息: {}", e.getMessage());
            }

            // 构建结果消息
            StringBuilder messageBuilder = new StringBuilder();
            if (errors.isEmpty()) {
                messageBuilder.append("分析成功完成");
                messageBuilder.append(isAuthenticated ? "（已认证）" : "（匿名）");
            } else {
                messageBuilder.append("分析完成，但有部分错误: ");
                messageBuilder.append(String.join("; ", errors));
            }

            // 添加仓库统计信息
            int originalCount = originalRepos.size();
            int forkedCount = forkedRepos.size();
            messageBuilder.append("。发现 ").append(originalCount).append(" 个原创仓库");
            if (true) { // includeForked always true now
                messageBuilder.append(" 和 ").append(forkedCount).append(" 个fork仓库");

                // 统计有自己贡献的 fork 仓库数量
                long contributedForks = forkedRepos.stream()
                        .filter(GitHubAnalysisResult.RepoInfo::isHasOwnContributions)
                        .count();

                if (contributedForks > 0) {
                    messageBuilder.append("（其中 ").append(contributedForks)
                            .append(" 个fork仓库有所有者的贡献）");
                }
            }

            return GitHubAnalysisResult.builder()
                    .users(new ArrayList<>(users))
                    .originalRepositories(new ArrayList<>(originalRepos))
                    .forkedRepositories(new ArrayList<>(forkedRepos))
                    .message(messageBuilder.toString())
                    .build();

        } catch (Exception e) {
            logger.error("分析 GitHub 信息出错", e);
            return GitHubAnalysisResult.builder()
                    .message("分析 GitHub 信息出错: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 分析用户信息
     */
    private void analyzeUser(GitHub github, String username,
            Set<GitHubAnalysisResult.UserInfo> users,
            Set<GitHubAnalysisResult.RepoInfo> originalRepos,
            Set<GitHubAnalysisResult.RepoInfo> forkedRepos,
            boolean includeForked) throws IOException {
        GHUser user = github.getUser(username);

        // 添加用户信息
        users.add(GitHubAnalysisResult.UserInfo.builder()
                .username(user.getLogin())
                .bio(user.getBio())
                .build());

        // 分析用户的仓库，限制数量并按 star 数排序
        List<GHRepository> allRepos = new ArrayList<>();
        int count = 0;

        // 使用分页迭代器，每页获取100个仓库
        PagedIterator<GHRepository> iterator = user.listRepositories().iterator();
        while (iterator.hasNext() && count < MAX_REPOS_PER_USER) {
            GHRepository repo = iterator.next();
            // 如果不包含 fork 的仓库，则跳过
            if (!includeForked && repo.isFork()) {
                continue;
            }
            allRepos.add(repo);
            count++;
        }

        // 按 star 数排序
        allRepos.sort(Comparator.comparingInt(GHRepository::getStargazersCount).reversed());

        // 创建线程池用于并行分析仓库
        ExecutorService repoExecutor = Executors.newFixedThreadPool(Math.min(allRepos.size(), MAX_THREADS));
        List<Future<?>> repoFutures = new ArrayList<>();

        // 并行分析所有仓库
        for (GHRepository repo : allRepos) {
            repoFutures.add(repoExecutor.submit(() -> {
                try {
                    // 使用超时控制分析每个仓库
                    analyzeRepositoryWithTimeout(repo, originalRepos, forkedRepos, includeForked);
                } catch (TimeoutException e) {
                    logger.warn("分析用户 {} 的仓库 {} 超时",
                            username, repo.getName());
                    // 继续处理下一个仓库
                } catch (Exception e) {
                    logger.warn("分析用户 {} 的仓库 {} 出错: {}",
                            username, repo.getName(), e.getMessage());
                    // 继续处理下一个仓库
                }
            }));
        }

        // 等待所有仓库分析完成
        for (Future<?> future : repoFutures) {
            try {
                future.get();
            } catch (Exception e) {
                logger.error("等待仓库分析任务完成时出错", e);
            }
        }

        // 关闭线程池
        repoExecutor.shutdown();
        try {
            if (!repoExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                repoExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            repoExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 使用超时控制分析仓库（通过所有者和仓库名）
     */
    private void analyzeRepositoryWithTimeout(GitHub github, String owner, String repoName,
            Set<GitHubAnalysisResult.RepoInfo> originalRepos,
            Set<GitHubAnalysisResult.RepoInfo> forkedRepos,
            boolean includeForked) throws IOException, TimeoutException {
        GHRepository repository = github.getRepository(owner + "/" + repoName);

        // 如果不包含 fork 的仓库，且当前仓库是 fork 的，则跳过
        if (!includeForked && repository.isFork()) {
            logger.info("跳过fork仓库 {}/{}", owner, repoName);
            return;
        }

        analyzeRepositoryWithTimeout(repository, originalRepos, forkedRepos, includeForked);
    }

    /**
     * 使用超时控制分析仓库
     */
    private void analyzeRepositoryWithTimeout(GHRepository repository,
            Set<GitHubAnalysisResult.RepoInfo> originalRepos,
            Set<GitHubAnalysisResult.RepoInfo> forkedRepos,
            boolean includeForked) throws TimeoutException {
        // 如果不包含 fork 的仓库，且当前仓库是 fork 的，则跳过
        if (!includeForked && repository.isFork()) {
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Void> future = executor.submit(() -> {
            try {
                analyzeRepository(repository, originalRepos, forkedRepos);
            } catch (Exception e) {
                logger.warn("仓库分析任务出错: {}", e.getMessage());
                throw new RuntimeException(e);
            }
            return null;
        });

        try {
            future.get(REPO_ANALYSIS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.warn("仓库 {} 分析超时 - 可能是大型仓库", repository.getFullName());
            throw e;
        } catch (Exception e) {
            future.cancel(true);
            throw new RuntimeException("仓库分析失败", e);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 分析仓库信息（通过仓库对象）
     */
    private void analyzeRepository(GHRepository repository,
            Set<GitHubAnalysisResult.RepoInfo> originalRepos,
            Set<GitHubAnalysisResult.RepoInfo> forkedRepos) throws IOException {
        // 检查是否有 README - 使用简单的 API 调用
        boolean hasReadme = false;
        try {
            // 尝试获取 README 的 URL，如果存在则表示有 README
            String readmeUrl = repository.getHtmlUrl() + "/blob/master/README.md";
            hasReadme = readmeUrl != null;
        } catch (Exception e) {
            logger.debug("仓库没有 README 文件");
        }

        // 判断是否是真实项目 - 使用基本信息判断
        boolean isRealProject = isRealProjectEfficient(repository);

        // 检查是否是 fork 的仓库
        boolean isFork = repository.isFork();

        // 默认值
        boolean hasOwnContributions = false;
        int contributionCount = 0;
        String owner = repository.getOwnerName();

        try {
            // 为所有仓库统计提交数，不仅限于fork仓库
            // 获取有限数量的提交记录，添加错误重试机制
            PagedIterable<GHCommit> commits = null;
            int retryCount = 0;
            boolean success = false;

            while (!success && retryCount < 3) {
                try {
                    // 在请求之间添加短暂延迟以避免触发API限制
                    if (retryCount > 0) {
                        Thread.sleep(API_RETRY_DELAY_MS * retryCount);
                    }

                    commits = repository.listCommits().withPageSize(MAX_COMMITS_TO_CHECK);
                    success = true;
                } catch (Exception e) {
                    retryCount++;
                    logger.warn("获取仓库 {} 提交记录失败 (尝试 {}/3): {}",
                            repository.getFullName(), retryCount, e.getMessage());
                    if (retryCount >= 3) {
                        throw e;
                    }
                }
            }

            if (commits != null) {
                // 限制实际获取的提交记录数量，避免分页过多
                List<GHCommit> commitList = new ArrayList<>();
                int count = 0;
                for (GHCommit commit : commits) {
                    commitList.add(commit);
                    count++;
                    if (count >= MAX_COMMITS_TO_CHECK)
                        break;
                }

                // 统计所有提交数
                contributionCount = commitList.size();

                // 对于fork仓库，还需要检查所有者的贡献
                if (isFork) {
                    int ownerCommits = 0;
                    for (GHCommit commit : commitList) {
                        try {
                            GHUser author = commit.getAuthor();
                            // 检查作者是否为仓库所有者
                            if (author != null && owner.equals(author.getLogin())) {
                                ownerCommits++;
                            }
                        } catch (Exception e) {
                            // 忽略无法解析作者的提交
                            logger.debug("无法获取提交作者信息: {}", e.getMessage());
                        }
                    }

                    hasOwnContributions = ownerCommits > 0;

                    if (hasOwnContributions) {
                        logger.info("仓库 {} 中发现所有者 {} 的 {} 次提交贡献",
                                repository.getFullName(), owner, ownerCommits);
                    } else {
                        logger.info("仓库 {} 中未找到所有者 {} 的提交贡献",
                                repository.getFullName(), owner);
                    }
                } else {
                    // 非fork仓库，所有提交都算作贡献
                    logger.info("原创仓库 {} 共有 {} 次提交",
                            repository.getFullName(), contributionCount);
                }
            }
        } catch (Exception e) {
            logger.warn("检查仓库提交记录出错: {}", e.getMessage());
        }

        // 创建仓库信息对象
        GitHubAnalysisResult.RepoInfo repoInfo = GitHubAnalysisResult.RepoInfo.builder()
                .name(repository.getName())
                .owner(repository.getOwnerName())
                .description(repository.getDescription())
                .stars(repository.getStargazersCount())
                .forks(repository.getForks())
                .hasReadme(hasReadme)
                .commitCount(contributionCount) // 使用统计的提交数
                .isRealProject(isRealProject)
                .isFork(isFork)
                .hasOwnContributions(hasOwnContributions)
                .aheadCount(isFork ? contributionCount : 0) // 只为fork仓库设置ahead计数
                .behindCount(0) // 不再计算落后提交数
                .build();

        // 根据是否是 fork 的仓库，添加到不同的集合中
        if (isFork) {
            forkedRepos.add(repoInfo);
        } else {
            originalRepos.add(repoInfo);
        }
    }

    /**
     * 高效版的真实项目判断
     * 只使用仓库对象中已有的信息，不进行额外 API 调用
     */
    private boolean isRealProjectEfficient(GHRepository repository) {
        // 所有这些数据都已经在repository对象中，不需要额外API调用
        int score = 0;

        // 1. 项目描述
        if (repository.getDescription() != null && !repository.getDescription().trim().isEmpty()) {
            score++;
        }

        // 2. Star数量
        if (repository.getStargazersCount() > 0) {
            score++;
            if (repository.getStargazersCount() >= 5) {
                score++; // 5个以上star加分
            }
        }

        // 3. Fork数量
        if (repository.getForks() > 0) {
            score++;
            if (repository.getForks() >= 3) {
                score++; // 3个以上fork加分
            }
        }

        // 4. 项目已存在一段时间
        try {
            Date createdAt = repository.getCreatedAt();
            Date updatedAt = repository.getUpdatedAt();
            if (createdAt != null && updatedAt != null) {
                long daysExisted = TimeUnit.DAYS.convert(
                        System.currentTimeMillis() - createdAt.getTime(), TimeUnit.MILLISECONDS);

                // 项目创建时间超过30天
                if (daysExisted > 30) {
                    score++;
                }

                // 近期有更新（90天内）
                long daysSinceLastUpdate = TimeUnit.DAYS.convert(
                        System.currentTimeMillis() - updatedAt.getTime(), TimeUnit.MILLISECONDS);
                if (daysSinceLastUpdate < 90) {
                    score++;
                }
            }
        } catch (Exception e) {
            logger.debug("无法计算仓库日期: {}", e.getMessage());
        }

        // 5. 观察者数量（Watchers）
        if (repository.getWatchers() > 1) {
            score++;
        }

        // 6. 是否启用Issues功能
        if (repository.hasIssues()) {
            score++;
        }

        // 7. 检查编程语言（防止空仓库）
        if (repository.getLanguage() != null) {
            score++;
        }

        // 8. 不是fork的仓库加分
        if (!repository.isFork()) {
            score += 2;
        }

        // 9. 仓库大小（KB）- 太小的仓库可能是空的或者示例项目
        try {
            if (repository.getSize() > 1000) { // 大于1MB
                score++;
            }
        } catch (Exception e) {
            // 忽略
        }

        // 设置一个合理阈值 - 根据实际情况调整
        logger.debug("仓库 {} 真实性评分: {}", repository.getFullName(), score);
        return score >= 5; // 达到5分以上认为是真实项目
    }
}