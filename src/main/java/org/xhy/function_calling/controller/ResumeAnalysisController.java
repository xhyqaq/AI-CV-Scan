package org.xhy.function_calling.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xhy.function_calling.service.tool.ResumeAnalysisService;

import java.util.HashMap;
import java.util.Map;

/**
 * 简历分析控制器
 * 提供简历分析API接口
 */
@RestController
@RequestMapping("/api/resume")
public class ResumeAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(ResumeAnalysisController.class);

    private final ResumeAnalysisService resumeAnalysisService;

    public ResumeAnalysisController(ResumeAnalysisService resumeAnalysisService) {
        this.resumeAnalysisService = resumeAnalysisService;
    }

    /**
     * 提交简历分析任务
     * 
     * @param resumeUrl 简历文件的URL
     * @return 任务ID和相关信息
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeResume(@RequestParam String resumeUrl) {
        if (resumeUrl == null || resumeUrl.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "简历URL不能为空"));
        }

        resumeAnalysisService.analyzeResume(resumeUrl);
        return ResponseEntity.ok(null);
    }
}