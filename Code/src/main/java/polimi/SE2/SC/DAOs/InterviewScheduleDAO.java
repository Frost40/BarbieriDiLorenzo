package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.InterviewSchedule;
import polimi.SE2.SC.beans.Questionnaire;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class InterviewScheduleDAO {
    private Connection connection;

    public InterviewScheduleDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Method used to retrieve all interview schedule dates for a given student-internship association
     *
     * @param studentHasInternshipId Identifier for the given student-internship association
     * @return Array list containing all the interview scheduled
     * @throws SQLException
     */
    public ArrayList<InterviewSchedule> getOngoingInternshipsInterviewSchedules(int studentHasInternshipId) throws SQLException {
        ArrayList<InterviewSchedule> interviewSchedules = new ArrayList<>();
        String performedAction = " extracting all interview schedules relative to the given student-internship association ";
        String query = "SELECT * FROM InterviewSchedule WHERE studentHasInternshipId = ?";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, studentHasInternshipId);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                InterviewSchedule interviewSchedule = new InterviewSchedule();

                interviewSchedule.setId(resultSet.getInt("id"));
                interviewSchedule.setDate(resultSet.getDate("date"));
                interviewSchedule.setEvaluation(resultSet.getBoolean("evaluation"));
                interviewSchedule.setHasBeenRejected(resultSet.getBoolean("hasBeenRejected"));
                interviewSchedule.setStudentHasInternshipId(resultSet.getInt("studentHasInternshipId"));

                interviewSchedules.add(interviewSchedule);
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
        return interviewSchedules;
    }

    public void scheduleInterview(Date date) throws SQLException {

    }

    public void cancelInterview(int id) throws SQLException {

    }

    public void setCompanyDecision(int interviewId, boolean decision) throws SQLException {

    }
}
