package com.carsrecommend.system.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AlgorithmVisualizationVO(
        Long recordId,
        Long demandId,
        Long userId,
        String algorithmVersion,
        BigDecimal alpha,
        String recommendStatus,
        String fallbackMessage,
        String profileText,
        AlgorithmVisualizationDemandVO demand,
        List<AlgorithmVisualizationConstraintVO> constraints,
        List<AlgorithmVisualizationDimensionVO> dimensions,
        AlgorithmVisualizationWeightVO weights,
        List<AlgorithmVisualizationStageStatVO> stageStats,
        List<AlgorithmVisualizationPipelineStepVO> pipeline,
        List<AlgorithmVisualizationMatrixRowVO> matrixRows,
        List<AlgorithmVisualizationItemVO> items,
        List<AlgorithmVisualizationFeatureScoreRuleVO> featureScoreRules,
        String snapshotNote,
        String compatibilityNote,
        LocalDateTime createTime) {
}
