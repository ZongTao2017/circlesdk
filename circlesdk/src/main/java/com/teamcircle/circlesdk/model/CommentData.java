package com.teamcircle.circlesdk.model;

import com.teamcircle.circlesdk.helper.AppSocialGlobal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommentData implements Serializable {
    public int commentId;
    public UserData user;
    public long date;
    public String content;
    public int likeNumber;
    public boolean isLiked;
    public ArrayList<CommentData> replies;
    public ArrayList<String> hashtags;
    public ArrayList<String> contents;

    public CommentData(UserData user) {
        this.user = user;
        this.replies = new ArrayList<>();
        this.hashtags = new ArrayList<>();
        this.contents = new ArrayList<>();
    }

    public CommentData(int commentId, UserData user) {
        this(user);
        this.commentId = commentId;
    }

    public void setContent(String content) {
        this.content = content;
        Matcher matcher = Pattern.compile(AppSocialGlobal.HashtagPattern).matcher(content);
        while (matcher.find()) {
            this.hashtags.add(matcher.group());
        }
        String[] ss = content.split(AppSocialGlobal.HashtagPattern);
        if (ss.length == 0) {
            this.contents.add("");
            this.contents.add(this.hashtags.get(0));
        } else {
            for (int i = 0; i < ss.length; i++) {
                this.contents.add(ss[i]);
                if (i < this.hashtags.size()) {
                    this.contents.add(this.hashtags.get(i));
                }
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof CommentData)) return false;
        return this.commentId == ((CommentData) obj).commentId;
    }
}
