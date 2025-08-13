package org.kartikey.tweet_scheduler.model;

public class TweetEntry {
    private int id;
    private String text;

    public TweetEntry(int id, String text) {
        this.id = id;
        this.text = text;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }
}
