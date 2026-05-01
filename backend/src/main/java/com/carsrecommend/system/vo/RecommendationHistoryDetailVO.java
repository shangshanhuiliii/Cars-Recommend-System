package com.carsrecommend.system.vo;

public class RecommendationHistoryDetailVO extends RecommendationResponseVO {

    private DemandWeightsVO weights;
    private UserDemandVO demand;

    public DemandWeightsVO getWeights() {
        return weights;
    }

    public void setWeights(DemandWeightsVO weights) {
        this.weights = weights;
    }

    public UserDemandVO getDemand() {
        return demand;
    }

    public void setDemand(UserDemandVO demand) {
        this.demand = demand;
    }
}
