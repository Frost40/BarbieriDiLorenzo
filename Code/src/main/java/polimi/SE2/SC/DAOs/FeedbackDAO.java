package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Feedback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import static polimi.SE2.SC.utils.UtilityFunctions.splitStringByComma;

public class FeedbackDAO {
    private Connection connection;

    public FeedbackDAO(Connection connection) {
        this.connection = connection;
    }

    public HashMap<String, ArrayList<Feedback>> getInternshipsFeedbacks(ArrayList<Integer> internshipsIds) throws SQLException {
        HashMap<String, ArrayList<Feedback>> internshipsWithFeedbacks = new HashMap<>();

        return internshipsWithFeedbacks;
    }

    public ArrayList<Feedback> getInternshipFeedbacks(int internshipId) throws SQLException {
        ArrayList<Feedback> feedbacks = null;

        return feedbacks;
    }

    /**
     * Method used to retrieve form the Feedback table in the database
     * the list of all feedbacks that have not been read by a company
     *
     * @param companyId Identifier of the company whose feedbacks we want to extract
     * @return Array list containing the feedbacks
     * @throws SQLException
     */
    public ArrayList<Feedback> getNotReadFeedbacks(int companyId) throws SQLException {
        ArrayList<Feedback> feedbacks = new ArrayList<>();
        String performedAction = " extracting all feedbacks that have not been read by the company ";
        String query = "SELECT f.*, shi.id AS studentInternshipId, i.id AS internshipId, i.companyId " +
                "FROM Feedback f " +
                "JOIN StudentHasInternship shi ON f.studentHasInternshipId = shi.id " +
                "JOIN Internship i ON shi.internshipId = i.id " +
                "WHERE i.companyId = ? AND f.read = FALSE;";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, companyId);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Feedback feedback = new Feedback();

                feedback.setId(resultSet.getInt("id"));
                feedback.setText(resultSet.getString("text"));
                feedback.setRead(resultSet.getBoolean("read"));
                feedback.setRating(resultSet.getInt("rating"));
                feedback.setStudentHasInternshipId(resultSet.getInt("studentHasInternshipId"));

                feedbacks.add(feedback);
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
        return feedbacks;
    }

    /**
     * Method used to retrieve all feedbacks uploaded by a given student
     * 
     * @param studentId Identifier of the student whose feedbacks we want to extract
     * @return Array list containing all feedback given by the student
     * @throws SQLException
     */
    public ArrayList<Feedback> getFeedbacksGivenByStudent(int studentId) throws SQLException {
        ArrayList<Feedback> feedbacks = new ArrayList<>();
        String performedAction = " extracting all feedbacks that have been uploaded by the student ";
        String query = "SELECT f.*, shi.id AS studentInternshipId " +
                "FROM Feedback f " +
                "JOIN StudentHasInternship shi ON f.studentHasInternshipId = shi.id " +
                "WHERE shi.studentId = ?";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, studentId);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Feedback feedback = new Feedback();

                feedback.setId(resultSet.getInt("id"));
                feedback.setText(resultSet.getString("text"));
                feedback.setRead(resultSet.getBoolean("read"));
                feedback.setRating(resultSet.getInt("rating"));
                feedback.setStudentHasInternshipId(resultSet.getInt("studentHasInternshipId"));

                feedbacks.add(feedback);
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
        return feedbacks;
    }

    /**
     * Method used to insert into the database a new feedback
     * 
     * @param text Text of the feedback that can be empty
     * @param rating Rating from 0 to 10 given by the student to the internship experience
     * @param studentHasInternshipId Identifier for the association student-internship that just ended
     * @throws SQLException
     */
    public void uploadFeedback(String text, int rating, int studentHasInternshipId) throws SQLException {
        String performedAction = " inserting a new complaint into the DB ";
        String query = "INSERT into Feedback (text, `read`, rating, studentHasInternshipId) VALUES (?, FALSE, ?, ?)";
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, text);
            preparedStatement.setInt(2, rating);
            preparedStatement.setInt(3, studentHasInternshipId);

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

    public void hasBeenRead(int feedbackId) throws SQLException {

    }
}
