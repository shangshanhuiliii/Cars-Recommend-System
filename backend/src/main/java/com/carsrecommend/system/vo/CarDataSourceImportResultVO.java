package com.carsrecommend.system.vo;

import java.util.ArrayList;
import java.util.List;

public class CarDataSourceImportResultVO {

    private Integer totalCount = 0;
    private Integer successCount = 0;
    private Integer createdCount = 0;
    private Integer updatedCount = 0;
    private Integer skippedCount = 0;
    private Integer failedCount = 0;
    private String matchingRule = "brand + series + modelName + launchYear";
    private String nextStep = "导入车型可立即用于详情、对比、收藏和管理端维护；进入推荐前请执行全部车型评分重算。";
    private List<CarDataSourceImportIssueVO> issues = new ArrayList<>();

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(Integer createdCount) {
        this.createdCount = createdCount;
    }

    public Integer getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(Integer updatedCount) {
        this.updatedCount = updatedCount;
    }

    public Integer getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(Integer skippedCount) {
        this.skippedCount = skippedCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public String getMatchingRule() {
        return matchingRule;
    }

    public void setMatchingRule(String matchingRule) {
        this.matchingRule = matchingRule;
    }

    public String getNextStep() {
        return nextStep;
    }

    public void setNextStep(String nextStep) {
        this.nextStep = nextStep;
    }

    public List<CarDataSourceImportIssueVO> getIssues() {
        return issues;
    }

    public void setIssues(List<CarDataSourceImportIssueVO> issues) {
        this.issues = issues;
    }
}
