package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Complaint;
import polimi.SE2.SC.beans.InterviewSchedule;
import polimi.SE2.SC.beans.Questionnaire;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class QuestionnaireDAO {
    private Connection connection;

    public QuestionnaireDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Method used to extract a specific questionnaire given its id
     *
     * @param id Questionnaire schedule id
     * @return The retrieved questionnaire
     * @throws SQLException
     */
    public Questionnaire getQuestionnaire(int id) throws SQLException{
        Questionnaire questionnaire = null;
        String performedAction = " extracting a questionnaire using its unique id ";
        String query = "SELECT * FROM Questionnaire where id = ?";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, id);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                questionnaire = new Questionnaire();

                questionnaire.setId(resultSet.getInt("id"));
                questionnaire.setFile(resultSet.getBytes("file"));
                questionnaire.setEvaluation(resultSet.getBoolean("evaluation"));
                questionnaire.setStudentHasInternshipId(resultSet.getInt("studentHasInternshipId"));
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
        return questionnaire;
    }
    
    /**
     * Method used to retrieve all interview schedule dates for a given student-internship association
     *
     * @param studentHasInternshipId Identifier for the given student-internship association
     * @return Array list containing all the interview scheduled
     * @throws SQLException
     */
    public ArrayList<Questionnaire> getOngoingInternshipQuestionnaires(int studentHasInternshipId) throws SQLException {
        ArrayList<Questionnaire> questionnaires = new ArrayList<>();
        String performedAction = " extracting all questionnaires relative to the given student-internship association ";
        String query = "SELECT * FROM Questionnaire WHERE studentHasInternshipId = ?";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, studentHasInternshipId);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Questionnaire questionnaire = new Questionnaire();

                questionnaire.setId(resultSet.getInt("id"));
                questionnaire.setFile(resultSet.getBytes("file"));
                questionnaire.setEvaluation(resultSet.getBoolean("evaluation"));
                questionnaire.setStudentHasInternshipId(resultSet.getInt("studentHasInternshipId"));

                questionnaires.add(questionnaire);
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
        return questionnaires;
    }

    public void uploadQuestionnaire(InputStream file) throws SQLException {

    }

    /**
     * Method used to set the company decision for the given questionnaire associated to the questionnaireId
     *
     * @param questionnaireId Identifier for the questionnaire
     * @param decision Company decision to upload
     * @throws SQLException
     */
    public void setCompanyDecision(int questionnaireId, boolean decision) throws SQLException {
        String performedAction = " setting the company decision for the given questionnaire ";
        String query = "UPDATE Questionnaire " +
                "SET evaluation = ? " +
                "WHERE id = ?";
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setBoolean(1, decision);
            preparedStatement.setInt(2, questionnaireId);

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

    public void deleteQuestionnaire(int questionnaireId) throws SQLException {

    }
}
