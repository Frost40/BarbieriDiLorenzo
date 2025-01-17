package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Complaint;
import polimi.SE2.SC.beans.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class MessageDAO {
    private Connection connection;

    public MessageDAO(Connection connection) {
        this.connection = connection;
    }

    public ArrayList<Message> getNotReadMessages(int receiverId) throws SQLException {
        ArrayList<Message> messages = new ArrayList<>();
        String performedAction = " extracting all feedbacks that have not been read by the company ";
        String query = "SELECT * FROM Message WHERE receiverId = ?";;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, receiverId);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Message message = new Message();

                message.setId(resultSet.getInt("id"));
                message.setMessage(resultSet.getString("message"));
                message.setRead(resultSet.getBoolean("read"));
                message.setSenderId(resultSet.getInt("senderId"));
                message.setReceiverId(resultSet.getInt("receiverId"));

                messages.add(message);
            }

        } catch (SQLException e) {
            throw new SQLException("Error accessing the DB when" + performedAction + "[ " + e.getMessage() + " ]");

        } finally {

            try {
                resultSet.close();

            } catch (Exception e) {
                throw new SQLException("Error closing the result set when" + performedAction + "[ " + e.getMessage() + " ]");
            }

            try {
                preparedStatement.close();

            } catch (Exception e) {
                throw new SQLException("Error closing the statement when" + performedAction + "[ " + e.getMessage() + " ]");
            }
        }
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
