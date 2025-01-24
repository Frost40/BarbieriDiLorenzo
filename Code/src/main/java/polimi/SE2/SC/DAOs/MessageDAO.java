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

    /**
     * Method used to retrieve from the database all messages not read by the given user
     *
     * @param receiverId Identifier for the user of which we want to extract the messages
     * @param isStudent Boolean variable use to distinguish between the student and the company type of user
     * @return Array list containing the list of unread messages
     * @throws SQLException
     */
    public ArrayList<Message> getNotReadMessages(int receiverId, boolean isStudent) throws SQLException {
        ArrayList<Message> messages = new ArrayList<>();
        String performedAction = " extracting all messages that have not been read by the user ";
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

    /**
     * Method used to retrieve the list of internships with which the student has started a chat
     *
     * @param studentId Identifier for the student account
     * @return Array list containing the internships
     * @throws SQLException
     */
    public ArrayList<Internship> getAllChatsForStudent(int studentId) throws SQLException {
        ArrayList<Internship> internships = new ArrayList<>();
        String performedAction = " extracting all internships the student has open chats with ";
        String query = "SELECT DISTINCT " +
                "    Internship.*, " +
                "    Internship.id AS internshipId " +
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
                internship.setName(resultSet.getString("name"));
                internship.setDescription(resultSet.getString("description"));
                internship.setRating(resultSet.getDouble("rating"));
                internship.setCompanyId(resultSet.getInt("companyId"));

                internships.add(internship);
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
        return internships;
    }

    /**
     * Method used to retrieve all the internships of the company that have no empty 
     * chats and, for each one, the list of students with which the chats are
     * 
     * @param companyId Identifier for the company eho posses the internships 
     * @return HashMap that for each internship associates the list of students it has non-empty chats with
     * @throws SQLException
     */
    public HashMap<Internship, ArrayList<Student>> getAllChatsForCompany(int companyId) throws SQLException {
        HashMap<Internship, ArrayList<Student>> allChats = new HashMap<>();
        String performedAction = " extracting all chats the company has open ";
        String query = "SELECT DISTINCT" +
                "    Internship.*, " +
                "    Student.*, " +
                "    Internship.id AS internshipId, " +
                "    Internship.name AS internshipName, " +
                "    Student.id AS studentId, " +
                "    Student.name AS studentName " +
                "FROM Message " +
                "JOIN Internship " +
                "    ON (Message.senderId = Internship.id AND Message.fromInternship = true) " +
                "    OR (Message.receiverId = Internship.id AND Message.fromInternship = false) " +
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
                // Extract Internship (all columns)
                Internship internship = new Internship();
                internship.setId(resultSet.getInt("internshipId"));
                internship.setName(resultSet.getString("internshipName"));
                internship.setDescription(resultSet.getString("description"));
                internship.setRating(resultSet.getInt("rating"));
                internship.setCompanyId(resultSet.getInt("companyId"));

                // Extract Student (all columns)
                Student student = new Student();
                student.setId(resultSet.getInt("studentId"));
                student.setName(resultSet.getString("studentName"));
                student.setSurname(resultSet.getString("surname"));
                student.setEmail(resultSet.getString("email"));
                student.setPassword(resultSet.getString("password"));
                student.setNationality(resultSet.getString("nationality"));
                student.setCityOfBirth(resultSet.getString("cityOfBirth"));

                // Organize data in the HashMap
                allChats
                        .computeIfAbsent(internship, k -> new ArrayList<>()) // Get or create List<Student> for this Internship
                        .add(student); // Add the Student to the list
            }

        } catch (SQLException e) {
            throw new SQLException("Error accessing the DB when" + performedAction + "[ " + e.getMessage() + " ]");

        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (Exception e) {
                    throw new SQLException("Error closing the result set when" + performedAction + "[ " + e.getMessage() + " ]");
                }
            }

            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {
                    throw new SQLException("Error closing the statement when" + performedAction + "[ " + e.getMessage() + " ]");
                }
            }
        }

        return allChats;
    }

    /**
     * Method used to retrieve the entirety of the chat between a student and an internship
     *
     * @param account1 The first account identifier (does not matter if is a student id or an internship id)
     * @param account2 The second account identifier (does not matter if is a student id or an internship id)
     * @return List of messages exchanged between student and internship
     * @throws SQLException
     */
    public ArrayList<Message> getAllChatMessages(int account1, int account2) throws SQLException {
        ArrayList<Message> messages = new ArrayList<>();
        String performedAction = " extracting all messages of the chat between userId and internshipId ";
        String query =  "SELECT * FROM Message " +
                        "WHERE (receiverId = ? AND senderId = ?) " +
                        "OR (receiverId = ? AND senderId = ?)";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, account1);
            preparedStatement.setInt(2, account2);
            preparedStatement.setInt(3, account2);
            preparedStatement.setInt(4, account1);

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

    /**
     * Method used to insert into the database a new message
     *
     * @param message Body of the message
     * @param fromInternship Boolean to indicate whether the message was sent from an internship (true) or a student (false)
     * @param senderId Identifier of the sender
     * @param receiverId Identifier of the receiver
     * @throws SQLException
     */
    public void insertMessage(String message, boolean fromInternship, int senderId, int receiverId) throws SQLException {
        String performedAction = " adding a new message into the database ";
        String query = "INSERT Into Message (message, `read`, fromInternship, senderId, receiverId) VALUES (?,FALSE,?,?,?)";
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, message);
            preparedStatement.setBoolean(2, fromInternship);
            preparedStatement.setInt(3, senderId);
            preparedStatement.setInt(4, receiverId);

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new SQLException("Error accessing the DB when" + performedAction + "[ " + e.getMessage() + " ]");

        } finally {

            try {
                preparedStatement.close();

            } catch (Exception e) {
                throw new SQLException("Error closing the statement when" + performedAction + "[ " + e.getMessage() + " ]");
            }
        }
    }

    public void hasBeenRead(int messageId) throws SQLException {

    }

    /**
     * Method used to set all messages inside a chat to read
     * 
     * @param userId Student identifier
     * @param internshipId Internship identifier
     * @throws SQLException
     */
    public void setChatToRead(int userId, int internshipId) throws SQLException {
        String performedAction = " setting all message in the chat between student and internship to read ";
        String query = "UPDATE Message " +
                "SET `read` = ? " +
                "WHERE (receiverId = ? AND senderId = ?) " +
                "OR (receiverId = ? AND senderId = ?)";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setBoolean(1, true);
            preparedStatement.setInt(2, userId);
            preparedStatement.setInt(3, internshipId);
            preparedStatement.setInt(4, internshipId);
            preparedStatement.setInt(5, userId);

            preparedStatement.executeUpdate();
            
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
    }

}
