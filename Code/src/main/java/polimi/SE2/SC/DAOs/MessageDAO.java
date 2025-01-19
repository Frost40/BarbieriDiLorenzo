package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Complaint;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Message;
import polimi.SE2.SC.beans.Student;

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

    public ArrayList<Message> getNotReadMessages(int receiverId, boolean isStudent) throws SQLException {
        ArrayList<Message> messages = new ArrayList<>();
        String performedAction = " extracting all feedbacks that have not been read by the user ";
        String query;
        if (isStudent) {
            query = "SELECT * FROM Message WHERE receiverId = ? AND fromInternship = TRUE AND `read` = FALSE";
        } else {
            query = "SELECT Message.* " +
                    "FROM Message " +
                    "JOIN Internship ON Message.receiverId = Internship.id " +
                    "WHERE Internship.companyId = ? AND fromInternship = FALSE AND Message.read = FALSE";
        }
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
                message.setFromInternship(resultSet.getBoolean("fromInternship"));
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

    public HashMap<Internship, ArrayList<Message>> getAllChatsForStudent(int studentId) throws SQLException {
        HashMap<Internship, ArrayList<Message>> allChats = new HashMap<>();
        String performedAction = " extracting all chats the student has open ";
        String query =  "SELECT Message.*, " +
                        "       Internship.id AS internshipId, " +
                        "       Internship.name AS internshipName, " +
                        "       Internship.description AS internshipDescription, " +
                        "       Internship.companyId AS internshipCompanyId " +
                        "FROM Message " +
                        "JOIN Internship ON (Message.senderId = Internship.id AND Message.fromInternship = true) " +
                        "               OR (Message.receiverId = Internship.id AND Message.fromInternship = false) " +
                        "WHERE (Message.receiverId = ? AND Message.fromInternship = true) " +
                        "   OR (Message.senderId = ? AND Message.fromInternship = false)";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, studentId);
            preparedStatement.setInt(2, studentId);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                // Retrieve Internship Data using aliases
                Internship internship = new Internship();
                internship.setId(resultSet.getInt("internshipId")); // Alias used here
                internship.setName(resultSet.getString("internshipName")); // Alias used here
                internship.setDescription(resultSet.getString("internshipDescription")); // Alias used here
                internship.setRating(resultSet.getInt("rating"));
                internship.setCompanyId(resultSet.getInt("internshipCompanyId")); // Alias used here

                // Retrieve Message Data
                Message message = new Message();
                message.setId(resultSet.getInt("id")); // No alias required since 'id' is unique in Message
                message.setMessage(resultSet.getString("message"));
                message.setRead(resultSet.getBoolean("read"));
                message.setFromInternship(resultSet.getBoolean("fromInternship"));
                message.setSenderId(resultSet.getInt("senderId"));
                message.setReceiverId(resultSet.getInt("receiverId"));

                // Add the message to the corresponding internship in the HashMap
                allChats.computeIfAbsent(internship, k -> new ArrayList<>()).add(message);
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
        return allChats;
    }

    public HashMap<Internship, HashMap<Student, ArrayList<Message>>> getAllChatsForCompany(int companyId) throws SQLException {
        HashMap<Internship, HashMap<Student, ArrayList<Message>>> allChats = new HashMap<>();
        String performedAction = " extracting all chats the company has open ";
        String query =  "SELECT Message.*, " +
                        "       Internship.id AS internshipId, " +
                        "       Internship.name AS internshipName, " +
                        "       Student.id AS studentId, " +
                        "       Student.name AS studentName " +
                        "FROM Message " +
                        "JOIN Internship ON (Message.senderId = Internship.id AND Message.fromInternship = true) " +
                        "               OR (Message.receiverId = Internship.id AND Message.fromInternship = false) " +
                        "JOIN Student " +
                        "    ON (Message.senderId = Student.id AND Message.fromInternship = false) " +
                        "    OR (Message.receiverId = Student.id AND Message.fromInternship = true) " +
                        "WHERE Internship.companyId = ?";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, companyId);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                // Extract Internship
                Internship internship = new Internship();
                internship.setId(resultSet.getInt("internshipId"));
                internship.setName(resultSet.getString("internshipName"));
                internship.setDescription(resultSet.getString("description"));
                internship.setRating(resultSet.getInt("rating"));
                internship.setCompanyId(resultSet.getInt("companyId"));

                // Extract Student
                Student student = new Student();
                student.setId(resultSet.getInt("studentId"));
                student.setEmail(resultSet.getString("email"));
                student.setPassword(resultSet.getString("password"));
                student.setName(resultSet.getString("studentName"));
                student.setSurname(resultSet.getString("surname"));
                student.setNationality(resultSet.getString("nationality"));
                student.setCityOfBirth(resultSet.getString("cityOfBirth"));

                // Extract Message
                Message message = new Message();
                message.setId(resultSet.getInt("id")); // No alias required since 'id' is unique in Message
                message.setMessage(resultSet.getString("message"));
                message.setRead(resultSet.getBoolean("read"));
                message.setFromInternship(resultSet.getBoolean("fromInternship"));
                message.setSenderId(resultSet.getInt("senderId"));
                message.setReceiverId(resultSet.getInt("receiverId"));

                // Organize data in the nested HashMap
                allChats
                        .computeIfAbsent(internship, k -> new HashMap<>()) // Get or create HashMap<Student, ArrayList<Message>> for this Internship
                        .computeIfAbsent(student, k -> new ArrayList<>()) // Get or create ArrayList<Message> for this Student
                        .add(message); // Add the Message to the list
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
        return allChats;
    }

    /**
     * Method used to retrieve the entirety of the chat between a student and an internship
     *
     * @param userId The student
     * @param internshipId The internship
     * @return List of messages exchanged between student and internship
     * @throws SQLException
     */
    public ArrayList<Message> getAllChatMessages(int userId, int internshipId) throws SQLException {
        ArrayList<Message> messages = new ArrayList<>();
        String performedAction = " extracting all messages of the chat between userId and internshipId ";
        String query =  "SELECT * FROM Message " +
                        "WHERE (receiverId = ? AND senderId = ?) " +
                        "OR (receiverId = ? AND senderId = ?)";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, internshipId);
            preparedStatement.setInt(3, internshipId);
            preparedStatement.setInt(4, userId);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Message message = new Message();

                message.setId(resultSet.getInt("id"));
                message.setMessage(resultSet.getString("message"));
                message.setRead(resultSet.getBoolean("read"));
                message.setFromInternship(resultSet.getBoolean("fromInternship"));
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

    public void insertMessage(String message) throws SQLException {

    }

    public void hasBeenRead(int messageId) throws SQLException {

    }
}
