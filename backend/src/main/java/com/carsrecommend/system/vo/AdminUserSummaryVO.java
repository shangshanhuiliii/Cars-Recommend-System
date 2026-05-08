package com.carsrecommend.system.vo;

public class AdminUserSummaryVO {

    private long demandCount;
    private long recommendRecordCount;
    private long favoriteCount;
    private long feedbackCount;

    public long getDemandCount() {
        return demandCount;
    }

    public void setDemandCount(long demandCount) {
        this.demandCount = demandCount;
    }

    public long getRecommendRecordCount() {
        return recommendRecordCount;
    }

    public void setRecommendRecordCount(long recommendRecordCount) {
        this.recommendRecordCount = recommendRecordCount;
    }

    public long getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(long favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public long getFeedbackCount() {
        return feedbackCount;
    }

    public void setFeedbackCount(long feedbackCount) {
        this.feedbackCount = feedbackCount;
    }
}
