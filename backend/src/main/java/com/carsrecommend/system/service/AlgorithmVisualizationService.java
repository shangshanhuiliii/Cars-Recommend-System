package com.carsrecommend.system.service;

import com.carsrecommend.system.vo.AlgorithmVisualizationVO;

public interface AlgorithmVisualizationService {

    AlgorithmVisualizationVO getVisualization(Long recordId, Long userId);

    AlgorithmVisualizationVO getVisualizationForAdmin(Long recordId);
}
