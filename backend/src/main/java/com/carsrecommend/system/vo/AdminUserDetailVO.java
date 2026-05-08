package com.carsrecommend.system.vo;

import java.util.List;

public class AdminUserDetailVO {

    private AdminUserBaseVO user;
    private AdminUserSummaryVO summary;
    private AdminUserDemandVO latestDemand;
    private List<RecommendationHistoryItemVO> recentRecommendRecords;
    private List<UserFavoriteItemVO> favorites;
    private List<RecommendationFeedbackVO> feedbacks;

    public AdminUserBaseVO getUser() {
        return user;
    }

    public void setUser(AdminUserBaseVO user) {
        this.user = user;
    }

    public AdminUserSummaryVO getSummary() {
        return summary;
    }

    public void setSummary(AdminUserSummaryVO summary) {
        this.summary = summary;
    }

    public AdminUserDemandVO getLatestDemand() {
        return latestDemand;
    }

    public void setLatestDemand(AdminUserDemandVO latestDemand) {
        this.latestDemand = latestDemand;
    }

    public List<RecommendationHistoryItemVO> getRecentRecommendRecords() {
        return recentRecommendRecords;
    }

    public void setRecentRecommendRecords(List<RecommendationHistoryItemVO> recentRecommendRecords) {
        this.recentRecommendRecords = recentRecommendRecords;
    }

    public List<UserFavoriteItemVO> getFavorites() {
        return favorites;
    }

    public void setFavorites(List<UserFavoriteItemVO> favorites) {
        this.favorites = favorites;
    }

    public List<RecommendationFeedbackVO> getFeedbacks() {
        return feedbacks;
    }

    public void setFeedbacks(List<RecommendationFeedbackVO> feedbacks) {
        this.feedbacks = feedbacks;
    }
}
