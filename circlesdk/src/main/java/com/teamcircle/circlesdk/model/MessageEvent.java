package com.teamcircle.circlesdk.model;

public class MessageEvent {
    public MessageEventType type;
    public Object data;

    public MessageEvent(MessageEventType type) {
        this.type = type;
    }

    public MessageEvent(MessageEventType type, Object data) {
        this.type = type;
        this.data = data;
    }

    public enum MessageEventType {
        FINISH_AUTH,
        SIGN_OUT,
        CHANGE_PROFILE,
        DONE_CHANGE_PROFILE_IMAGE,
        ADD_TAG,
        DELETE_TAG,
        DONE_TAG_PRODUCT,
        UPDATE_POST,
        DONE_SEND_POST,
        DONE_SEND_POST_MAIN,
        DONE_SEND_POST_ME,
        FOLLOW,
        EDIT_COMMENT,
        HIDE_TAB_BUTTONS,
        SHOW_TAB_BUTTONS,
        TRIM_VIDEO,
        SHOW_MAX_VIDEO_TIME,
        CHANGE_VOLUME,
        UPDATE_PRODUCTS,
        DONE_DOWNLOAD
    }
}
