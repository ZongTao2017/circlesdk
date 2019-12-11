package com.teamcircle.circlesdk.model;

import java.io.Serializable;
import java.util.LinkedHashMap;

public class UserData implements Serializable {
    public int userId;
    public String username;
    public String email;
    public String photoUrl;
    public String bio;
    public LinkedHashMap<Integer, PostData> posts;
    public int favoriteNumber;
    public int followerNumber;
    public int followingNumber;
    public boolean followed;
    public boolean followingMe;
    public int postNumber;

    public UserData(int userId) {
        this.userId = userId;
        this.username = "";
        this.email = "";
        this.photoUrl = null;
        this.bio = "";
        this.posts = new LinkedHashMap<>();
        this.favoriteNumber = 0;
        this.followerNumber = 0;
        this.followingNumber = 0;
        this.followed = false;
        this.followingMe = false;
        this.postNumber = 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof UserData)) return false;
        return this.userId == ((UserData) obj).userId;
    }
}
