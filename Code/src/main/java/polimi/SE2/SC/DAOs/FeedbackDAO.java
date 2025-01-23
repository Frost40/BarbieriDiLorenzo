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

    public void uploadFeedback(String text, int rating) throws SQLException {

    }

    public void hasBeenRead(int feedbackId) throws SQLException {

    }
}
