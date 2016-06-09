package com.egfavre;

import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
    // posts will be public so keep global
    static HashMap<String, User> users = new HashMap<>();
    static ArrayList<Message> messages = new ArrayList<>();

    public static void main(String[] args) {
	//create some test objects  and message in separate methods
        addTestUsers();
        addTestMessages();

        Spark.init();

        Spark.get(
            "/",
            (request, response) -> {

                Session session = request.session();
                String username = session.attribute("username");

                String idStr = request.queryParams("replyId");
                int replyId = -1;
                if (idStr != null) {
                    replyId = Integer.valueOf(idStr);
                }

                //display only subset of messages on same thread
                ArrayList<Message> subset = new ArrayList<Message>();
                for (Message msg : messages){
                    //if reply id is -1; add to subset
                    if (msg.replyId == replyId) {
                        subset.add(msg);
                    }
                }

                Message parentMessage = null;
                if (replyId >= 0){
                    parentMessage = messages.get(replyId);
                }

                HashMap m = new HashMap();
                m.put("messages", subset);
                m.put("username", username);
                m.put("replyId", replyId);
                m.put("message", parentMessage);
                m.put("isMe", parentMessage !=null && username !=null && parentMessage.author.equals(username));
                return  new ModelAndView(m, "home.html");
            },
            new MustacheTemplateEngine()
        );

        Spark.post(
                "/login",
                (request, response) -> {
                    String username = request.queryParams("username");
                    if (username == null){
                        throw new Exception("Login name not found");
                    }

                    User user = users.get(username);
                    if (user == null) {
                        user = new User(username, "");
                        users.put(username, user);
                    }

                    Session session = request.session();
                    session.attribute("username", username);

                    response.redirect(request.headers("Referer"));
                    return "";
                }
        );

        Spark.post(
                "/logout",
                (request, response) -> {
                    Session session = request.session();
                    session.invalidate();
                    response.redirect("/");
                    return "";
                }
        );

        Spark.post(
                "/create-message",
                (request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");
                    if (username == null) {
                        throw new Exception("not logged in");
                    }

                    int replyId = Integer.valueOf(request.queryParams("replyId"));
                    String text = request.queryParams("message");

                    Message msg = new Message(messages.size(), replyId, username, text);
                    messages.add(msg);

                    response.redirect(request.headers("Referer"));
                    return "";
                }
        );

        Spark.post(
                "/deleteMessage",
                (request, response) -> {
                    int id = Integer.valueOf(request.queryParams("id"));
                    //protect from deleting other people's messages
                    Session session = request.session();
                    String username = session.attribute("username");
                    Message m = messages.get(id);
                    if(!m.author.equals(username)){
                        throw new Exception("you can't delete this");
                    }

                    messages.remove(id);
                    //reset id's
                    int index = 0;
                    for (Message msg:messages) {
                        msg.id = index;
                        index++;
                    }

                    response.redirect("/");
                    return "";
                }
        );

    }

    static void addTestUsers(){
        users.put("Alice", new User("Alice", ""));
        users.put("Bob", new User("Bob", ""));
        users.put("Charlie", new User("Charlie", ""));
    }

    static void addTestMessages(){
        messages.add(new Message(0, -1, "Alice", "Hello World"));
        messages.add(new Message(1, -1, "Bob", "This is a new Thread"));
        messages.add(new Message(2, 0, "Charlie", "Cool thread, Alice"));
    }
}
