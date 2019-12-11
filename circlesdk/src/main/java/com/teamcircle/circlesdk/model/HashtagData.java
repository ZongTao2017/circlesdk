package com.teamcircle.circlesdk.model;

public class HashtagData {
    public int hashtagId;
    public String hashtag;
    public int number;
    public String photoUrl;

    public HashtagData(String hashtag, int number, String photoUrl) {
        this.hashtag = hashtag;
        this.number = number;
        this.photoUrl = photoUrl;
    }

    public HashtagData(int hashtagId, String hashtag, int number, String photoUrl) {
        this.hashtagId = hashtagId;
        this.hashtag = hashtag;
        this.number = number;
        this.photoUrl = photoUrl;
    }
}
