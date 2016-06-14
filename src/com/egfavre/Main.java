package com.egfavre;

import org.h2.tools.Server;
import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.lang.reflect.Array;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
    // posts will be public so keep global
//    static HashMap<String, User> users = new HashMap<>();
//    static ArrayList<Message> messages = new ArrayList<>();

    public static void  deleteMessage (Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM messages WHERE id = ?");
        stmt.setInt(1, id);
        stmt.execute();
    }

    public static void createTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS users (id IDENTITY, name VARCHAR, password VARCHAR)");
        stmt.execute("CREATE TABLE IF NOT EXISTS messages (id IDENTITY, reply_id INT, text VARCHAR, user_id INT)");
    }

    public static void insertUser(Connection conn, String name, String password) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO users VALUES (NULL, ?, ?)");
        stmt.setString(1, name);
        stmt.setString(2, password);
        stmt.execute();
    }

    public static User selectUser (Connection conn, String name) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE name = ?");
        stmt.setString(1,name);
        ResultSet results = stmt.executeQuery();
        if (results.next()){
            int id = results.getInt("id");
            String password = results.getString("password");
            return new User(id, name, password);
        }
        return null;
    }

    public static void insertMessage(Connection conn, int replyId, String text, int userId) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO messages VALUES (NULL, ?, ?, ?)");
        stmt.setInt(1, replyId);
        stmt.setString(2, text);
        stmt.setInt(3, userId);
        stmt.execute();
    }

    public static Message selectMessage (Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM messages INNER JOIN users ON messages.user_id = users.id WHERE users.id = ?");
        stmt.setInt(1, id);
        ResultSet results = stmt.executeQuery();
        if (results.next()){
            int replyId = results.getInt("messages.reply_id");
            String text = results.getString("messages.text");
            String author = results.getString("users.name");
            return new Message(id, replyId, author, text);
        }
        return null;
    }

    public static ArrayList<Message> selectReplies(Connection conn, int replyId) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM messages INNER JOIN users ON messages.user_id = users.id WHERE messages.reply_id = ?");
        stmt.setInt(1, replyId);
        ResultSet results = stmt.executeQuery();
        ArrayList<Message> msgs = new ArrayList<>();
        while(results.next()){
            int id = results.getInt("id");
            String text = results.getString("messages.text");
            String author = results.getString("users.name");
            Message msg = new Message(id, replyId, author, text);
            msgs.add(msg);
        }
        return msgs;
    }

    public static void main(String[] args) throws SQLException {
        Server.createWebServer().start();
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        createTables(conn);


        //create some test objects  and message in separate methods
//        addTestUsers();
//        addTestMessages();

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
//                ArrayList<Message> subset = new ArrayList<Message>();
//                for (Message msg : messages){
//                    //if reply id is -1; add to subset
//                    if (msg.replyId == replyId) {
//                        subset.add(msg);
//                    }
 //               }
                ArrayList<Message> subset = selectReplies(conn, replyId);

                Message parentMessage = null;
                if (replyId >= 0){
                  //  parentMessage = messages.get(replyId);
                    parentMessage = selectMessage(conn, replyId);
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

                    User user = selectUser(conn, username);
                    if (user == null) {
                        user = new User(username, "");
                        insertUser(conn, username, "");
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

                    User user = selectUser(conn, username);

                    insertMessage(conn, replyId, text, user.id);

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
                    Message m = selectMessage(conn, id);
                    if(!m.author.equals(username)){
                        throw new Exception("you can't delete this");
                    }

                    deleteMessage(conn, id);

                    //messages.remove(id);
                    //reset id's
//                    int index = 0;
//                    for (Message msg:messages) {
//                        msg.id = index;
//                        index++;
//                    }

                    response.redirect("/");
                    return "";
                }
        );

    }

//    static void addTestUsers(){
//        users.put("Alice", new User("Alice", ""));
//        users.put("Bob", new User("Bob", ""));
//        users.put("Charlie", new User("Charlie", ""));
//    }
//
//    static void addTestMessages(){
//        messages.add(new Message(0, -1, "Alice", "Hello World"));
//        messages.add(new Message(1, -1, "Bob", "This is a new Thread"));
//        messages.add(new Message(2, 0, "Charlie", "Cool thread, Alice"));
//    }
}
