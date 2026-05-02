package com.carsrecommend.system.vo;

import java.math.BigDecimal;
import java.util.List;

public class AdminStatOverviewVO {

    private List<StatItemVO> budgetDistribution;
    private List<StatItemVO> sceneDistribution;
    private List<StatItemVO> focusFactorDistribution;
    private List<StatItemVO> popularCars;
    private List<StatItemVO> recommendStatusDistribution;
    private List<StatItemVO> energyTypeDistribution;
    private List<StatItemVO> bodyTypeDistribution;
    private List<StatItemVO> satisfactionDistribution;
    private List<StatItemVO> feedbackReasonDistribution;
    private Long feedbackCount;
    private BigDecimal averageSatisfaction;

    public List<StatItemVO> getBudgetDistribution() {
        return budgetDistribution;
    }

    public void setBudgetDistribution(List<StatItemVO> budgetDistribution) {
        this.budgetDistribution = budgetDistribution;
    }

    public List<StatItemVO> getSceneDistribution() {
        return sceneDistribution;
    }

    public void setSceneDistribution(List<StatItemVO> sceneDistribution) {
        this.sceneDistribution = sceneDistribution;
    }

    public List<StatItemVO> getFocusFactorDistribution() {
        return focusFactorDistribution;
    }

    public void setFocusFactorDistribution(List<StatItemVO> focusFactorDistribution) {
        this.focusFactorDistribution = focusFactorDistribution;
    }

    public List<StatItemVO> getPopularCars() {
        return popularCars;
    }

    public void setPopularCars(List<StatItemVO> popularCars) {
        this.popularCars = popularCars;
    }

    public List<StatItemVO> getRecommendStatusDistribution() {
        return recommendStatusDistribution;
    }

    public void setRecommendStatusDistribution(List<StatItemVO> recommendStatusDistribution) {
        this.recommendStatusDistribution = recommendStatusDistribution;
    }

    public List<StatItemVO> getEnergyTypeDistribution() {
        return energyTypeDistribution;
    }

    public void setEnergyTypeDistribution(List<StatItemVO> energyTypeDistribution) {
        this.energyTypeDistribution = energyTypeDistribution;
    }

    public List<StatItemVO> getBodyTypeDistribution() {
        return bodyTypeDistribution;
    }

    public void setBodyTypeDistribution(List<StatItemVO> bodyTypeDistribution) {
        this.bodyTypeDistribution = bodyTypeDistribution;
    }

    public List<StatItemVO> getSatisfactionDistribution() {
        return satisfactionDistribution;
    }

    public void setSatisfactionDistribution(List<StatItemVO> satisfactionDistribution) {
        this.satisfactionDistribution = satisfactionDistribution;
    }

    public List<StatItemVO> getFeedbackReasonDistribution() {
        return feedbackReasonDistribution;
    }

    public void setFeedbackReasonDistribution(List<StatItemVO> feedbackReasonDistribution) {
        this.feedbackReasonDistribution = feedbackReasonDistribution;
    }

    public Long getFeedbackCount() {
        return feedbackCount;
    }

    public void setFeedbackCount(Long feedbackCount) {
        this.feedbackCount = feedbackCount;
    }

    public BigDecimal getAverageSatisfaction() {
        return averageSatisfaction;
    }

    public void setAverageSatisfaction(BigDecimal averageSatisfaction) {
        this.averageSatisfaction = averageSatisfaction;
    }
}
