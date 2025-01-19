package com.socialchat.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class Record {
    private String id;
    private String title;
    private String description;
    private String content;
    private String category;
    private List<String> tags;
    private String cover;
    private List<String> pictureList;
    private String fileList;
    private String videoList;
    private String atUserList;
    private String atUserVOList;
    private int thumbNum;
    private int favourNum;
    private int commentNum;
    private int priority;
    private int accessScope;
    private String clubId;
    private Object club;
    private String userId;
    private User user;
    private BestComment bestComment;
    private int viewNum;
    private String viewCount;
    private String shortLink;
    private boolean hasThumb;
    private boolean hasFavour;
    private int showPost;
    private int reviewStatus;
    private String reviewMessage;
    private String reviewerId;
    private long reviewTime;
    private int publishStatus;
    private int recommendStatus;
    private String recommendCount;
    private long recommendTime;
    private long editTime;
    private long createTime;
    private long updateTime;

    // Getters and Setters

    @Data
    public static class User {
        private String id;
        private String planetCode;
        private int planetPostAuth;
        private String userName;
        private String userAvatar;
        private String userThumbnailAvatar;
        private String gender;
        private String userProfile;
        private String interests;
        private String place;
        private String school;
        private String direction;
        private String graduationYear;
        private String company;
        private String job;
        private String github;
        private String blog;
        private int score;
        private String jobStatus;
        private int scoreLevel;
        private int followeeNum;
        private int followNum;
        private String followStatus;
        private long vipExpireTime;
        private String vipNumber;
        private String userRole;
        private int scoreRank;
        private String postAllThumbNum;
        private String postAllViewNum;
        private int needGuide;
        private int syncPopupLeftCount;
        private String paymentInfo;

        // Getters and Setters
    }

    @Data
    public static class BestComment {
        private String id;
        private int targetType;
        private String targetId;
        private String content;
        private String plainTextDescription;
        private String type;
        private int contentType;
        private int thumbNum;
        private String reviewStatus;
        private String reviewMessage;
        private String reviewerId;
        private Long reviewTime;
        private int priority;
        private String userId;
        private User user;
        private boolean hasThumb;
        private List<String> atUserList;
        private List<String> atUserVOList;
        private List<String> pictureList;

        // Getters and Setters
    }
}

