package com.csctracker.androidtracker.misc;

public class Message {

    public Message(String text){
        this.text = text;
        this.app = "andorid-notification-log";
        this.from = "andorid-notification-log";
    }

    private String from;
    private String text;
    private String app;

    public String getText() {
        return text;
    }

    public String getFrom() {
        return from;
    }

    public String getApp() {
        return app;
    }
}