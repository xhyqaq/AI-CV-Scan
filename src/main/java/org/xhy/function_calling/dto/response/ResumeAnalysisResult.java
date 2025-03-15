package org.xhy.function_calling.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 简历分析结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAnalysisResult {
    /**
     * 简历URL
     */
    private String resumeUrl;

    /**
     * 排版分析结果
     */
    private String layoutAnalysis;

    /**
     * 综合评价
     */
    private String evaluation;

    /**
     * GitHub分析结果
     */
    private String githubAnalysis;
}