package com.teamcircle.circlesdk.model;

import java.io.Serializable;

public class VideoData extends PhotoData implements Serializable {
    public int duration;
    public int start;
    public int end;
    public String videoUrl;
    public boolean isMuted;
    public float startPercentX, startPercentY, endPercentX, endPercentY;

    public VideoData(String path, int width, int height) {
        super(path, width, height);
        this.videoUrl = path;
        isMuted = false;
    }

    public VideoData(String path, int duration, int width, int height) {
        super(path, width, height);
        this.videoUrl = path;
        this.duration = duration;
        this.start = 0;
        this.end = duration;
        isMuted = false;
    }

    public VideoData(VideoData videoData) {
        super(videoData.photoUrl, videoData.width, videoData.height);
        this.videoUrl = videoData.videoUrl;
        this.duration = videoData.duration;
        this.start = videoData.start;
        this.end = videoData.end;
        this.startPercentX = videoData.startPercentX;
        this.endPercentX = videoData.endPercentX;
        this.startPercentY = videoData.startPercentY;
        this.endPercentY = videoData.endPercentY;
        isMuted = false;
    }
}

