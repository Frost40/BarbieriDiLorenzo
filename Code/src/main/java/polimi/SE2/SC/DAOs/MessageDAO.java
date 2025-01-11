package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Message;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class MessageDAO {
    private Connection connection;

    public MessageDAO(Connection connection) {
        this.connection = connection;
    }

    public ArrayList<Message> getNoReadMessages(int receiverId) throws SQLException {
        ArrayList<Message> messages = null;

        return messages;
    }

    public HashMap<String, ArrayList<Message>> getAllChatsForStudent(int studentId) throws SQLException {
        HashMap<String, ArrayList<Message>> allChats = new HashMap<>();

        return allChats;
    }

    public HashMap<String, HashMap<String, ArrayList<Message>>> getAllChatsForCompany(int studentId) throws SQLException {
        HashMap<String, HashMap<String, ArrayList<Message>>> allChats = new HashMap<>();

        return allChats;
    }

    public ArrayList<Message> getAllChatMessages(int senderId, int receiverId) throws SQLException {
        ArrayList<Message> messages = null;

        return messages;
    }

    public void insertMessage(String message) throws SQLException {

    }

    public void hasBeenRead(int messageId) throws SQLException {

    }
}
