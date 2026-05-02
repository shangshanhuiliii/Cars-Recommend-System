package com.carsrecommend.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("recommend_feedback")
public class RecommendFeedback extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("record_id")
    private Long recordId;

    @TableField("satisfaction_score")
    private Integer satisfactionScore;

    @TableField("satisfaction_level")
    private String satisfactionLevel;

    @TableField("reason_tags")
    private String reasonTags;

    @TableField("comment")
    private String comment;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Integer getSatisfactionScore() {
        return satisfactionScore;
    }

    public void setSatisfactionScore(Integer satisfactionScore) {
        this.satisfactionScore = satisfactionScore;
    }

    public String getSatisfactionLevel() {
        return satisfactionLevel;
    }

    public void setSatisfactionLevel(String satisfactionLevel) {
        this.satisfactionLevel = satisfactionLevel;
    }

    public String getReasonTags() {
        return reasonTags;
    }

    public void setReasonTags(String reasonTags) {
        this.reasonTags = reasonTags;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
