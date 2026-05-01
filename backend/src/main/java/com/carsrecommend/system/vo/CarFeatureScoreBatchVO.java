package com.carsrecommend.system.vo;

import java.util.List;

public class CarFeatureScoreBatchVO {

    private int recalculatedCount;
    private List<CarFeatureScoreVO> records;

    public CarFeatureScoreBatchVO() {
    }

    public CarFeatureScoreBatchVO(int recalculatedCount, List<CarFeatureScoreVO> records) {
        this.recalculatedCount = recalculatedCount;
        this.records = records;
    }

    public int getRecalculatedCount() {
        return recalculatedCount;
    }

    public void setRecalculatedCount(int recalculatedCount) {
        this.recalculatedCount = recalculatedCount;
    }

    public List<CarFeatureScoreVO> getRecords() {
        return records;
    }

    public void setRecords(List<CarFeatureScoreVO> records) {
        this.records = records;
    }
}
