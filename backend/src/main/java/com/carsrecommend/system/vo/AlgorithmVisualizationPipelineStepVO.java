package com.carsrecommend.system.vo;

public record AlgorithmVisualizationPipelineStepVO(
        int step,
        String title,
        String description,
        String inputSummary,
        String outputSummary,
        String recordResult,
        String codeModule) {
}
