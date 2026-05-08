package com.carsrecommend.system.vo;

public class AdminUserListItemVO extends AdminUserBaseVO {

    private long recommendRecordCount;
    private long favoriteCount;
    private long feedbackCount;

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
