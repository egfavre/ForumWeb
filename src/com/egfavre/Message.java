package com.egfavre;

/**
 * Created by user on 6/8/16.
 */
public class Message {
    int id;
    int replyId;
    String author;
    //in db author column will be user_id
    String text;

    public Message(int id, int replyId, String author, String text) {
        this.id = id;
        this.replyId = replyId;
        this.author = author;
        this.text = text;
    }
}
