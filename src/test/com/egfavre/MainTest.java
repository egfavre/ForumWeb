package com.egfavre;

import org.junit.Test;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Created by user on 6/14/16.
 */
public class MainTest {
    public Connection startConnection() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:test");
        Main.createTables(conn);
        return conn;
    }

    @Test
    public void testUser() throws SQLException {
        Connection conn = startConnection();
        Main.insertUser(conn, "Alice", "");
        User user = Main.selectUser(conn, "Alice");
        conn.close();

        assertTrue(user != null);
    }

    @Test
    public void testMessage() throws SQLException {
        Connection conn = startConnection();
        Main.insertUser(conn, "Alice", "");
        User user = Main.selectUser(conn, "Alice");
        Main.insertMessage(conn, -1, "Hello", 1);
        Message msg = Main.selectMessage(conn, 1);
        conn.close();

        assertTrue(msg != null);
    }


    @Test
    public void testAuthor() throws SQLException {
        Connection conn = startConnection();
        Main.insertUser(conn, "Alice", "");
        User user = Main.selectUser(conn, "Alice");
        Main.insertMessage(conn, -1, "Hello", 1);
        Message msg = Main.selectMessage(conn, 1);
        conn.close();

        assertTrue(msg.author.equals("Alice"));
    }

    @Test
    public void testReplies() throws SQLException {
        Connection conn = startConnection();
        Main.insertUser(conn, "Alice", "");
        Main.insertUser(conn, "Bob", "");
        User alice = Main.selectUser(conn, "Alice");
        User bob = Main.selectUser(conn, "Bob");
        Main.insertMessage(conn, -1, "Hello World!", alice.id);
        Main.insertMessage(conn, 1, "this is a reply", bob.id);
        Main.insertMessage(conn, 1, "Another", bob.id);

        ArrayList<Message> msgs = Main.selectReplies(conn, 1);
        conn.close();
        assertTrue(msgs.size() == 2);
    }

    @Test
    public void testDeleteMsg() throws SQLException {
        Connection conn = startConnection();
        Main.insertUser(conn, "Alice", "");
        Main.insertMessage(conn, -1, "Hi", 1);
        Main.deleteMessage(conn, 1);
        Message msg = Main.selectMessage(conn, 1);
        conn.close();
        assertTrue(msg == null);
    }
}