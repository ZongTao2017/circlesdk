package com.teamcircle.circlesdk.model;

import com.teamcircle.circlesdk.helper.AppSocialGlobal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostData implements Serializable {
    public int postId;
    public UserData user;
    public long date;
    public ArrayList<PhotoData> photos;
    public VideoData video;
    public String caption;
    public ArrayList<String> hashtags;
    public ArrayList<String> captions;
    public int likeNumber;
    public ArrayList<String> likeFollowNames;
    public boolean isLiked;
    public boolean isFavored;
    public ArrayList<CommentData> comments;

    public PostData() {
        photos = new ArrayList<>();
        comments = new ArrayList<>();
        hashtags = new ArrayList<>();
        captions = new ArrayList<>();
        caption = "";
        likeFollowNames = new ArrayList<>();
    }

    public PostData(int postId) {
        this.postId = postId;
        this.photos = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.hashtags = new ArrayList<>();
        this.captions = new ArrayList<>();
        this.caption = "";
        likeFollowNames = new ArrayList<>();
    }

    public void setCaption(String caption) {
        this.caption = caption;
        this.captions.clear();
        this.hashtags.clear();
        Matcher matcher = Pattern.compile(AppSocialGlobal.HashtagPattern).matcher(caption);
        while (matcher.find()) {
            this.hashtags.add(matcher.group());
        }

        String[] ss = caption.split(AppSocialGlobal.HashtagPattern);
        if (ss.length == 0) {
            this.captions.add("");
            this.captions.add(this.hashtags.get(0));
        } else {
            for (int i = 0; i < ss.length; i++) {
                this.captions.add(ss[i]);
                if (i < this.hashtags.size()) {
                    this.captions.add(this.hashtags.get(i));
                }
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof PostData)) return false;
        return this.postId == ((PostData) obj).postId;
    }
}
