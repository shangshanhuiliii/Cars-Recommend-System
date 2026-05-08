package com.carsrecommend.system.vo;

import java.math.BigDecimal;
import java.util.List;

public class AdminStatOverviewVO {

    private Long userCount;
    private Long activeUserCount;
    private Long disabledUserCount;
    private Long carCount;
    private Long recommendRecordCount;
    private Long todayRecommendRecordCount;
    private Long recentRecommendRecordCount;
    private Long favoriteCount;
    private List<StatItemVO> budgetDistribution;
    private List<StatItemVO> sceneDistribution;
    private List<StatItemVO> focusFactorDistribution;
    private List<StatItemVO> popularCars;
    private List<StatItemVO> favoriteTopCars;
    private List<StatItemVO> recommendStatusDistribution;
    private List<StatItemVO> energyTypeDistribution;
    private List<StatItemVO> bodyTypeDistribution;
    private List<StatItemVO> satisfactionDistribution;
    private List<StatItemVO> feedbackReasonDistribution;
    private Long feedbackCount;
    private BigDecimal averageSatisfaction;

    public Long getUserCount() {
        return userCount;
    }

    public void setUserCount(Long userCount) {
        this.userCount = userCount;
    }

    public Long getActiveUserCount() {
        return activeUserCount;
    }

    public void setActiveUserCount(Long activeUserCount) {
        this.activeUserCount = activeUserCount;
    }

    public Long getDisabledUserCount() {
        return disabledUserCount;
    }

    public void setDisabledUserCount(Long disabledUserCount) {
        this.disabledUserCount = disabledUserCount;
    }

    public Long getCarCount() {
        return carCount;
    }

    public void setCarCount(Long carCount) {
        this.carCount = carCount;
    }

    public Long getRecommendRecordCount() {
        return recommendRecordCount;
    }

    public void setRecommendRecordCount(Long recommendRecordCount) {
        this.recommendRecordCount = recommendRecordCount;
    }

    public Long getTodayRecommendRecordCount() {
        return todayRecommendRecordCount;
    }

    public void setTodayRecommendRecordCount(Long todayRecommendRecordCount) {
        this.todayRecommendRecordCount = todayRecommendRecordCount;
    }

    public Long getRecentRecommendRecordCount() {
        return recentRecommendRecordCount;
    }

    public void setRecentRecommendRecordCount(Long recentRecommendRecordCount) {
        this.recentRecommendRecordCount = recentRecommendRecordCount;
    }

    public Long getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(Long favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

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

    public List<StatItemVO> getFavoriteTopCars() {
        return favoriteTopCars;
    }

    public void setFavoriteTopCars(List<StatItemVO> favoriteTopCars) {
        this.favoriteTopCars = favoriteTopCars;
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
