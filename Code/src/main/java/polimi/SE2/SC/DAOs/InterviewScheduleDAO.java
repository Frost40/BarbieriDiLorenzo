package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.InterviewSchedule;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;

public class InterviewScheduleDAO {
    private Connection connection;

    public InterviewScheduleDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Method used to extract a specific interview schedule given its id
     *
     * @param id Interview schedule id
     * @return The retrieved interview schedule
     * @throws SQLException
     */
    public InterviewSchedule getInterviewSchedule(int id) throws SQLException{
        InterviewSchedule interviewSchedule = null;
        String performedAction = " extracting a interview schedule using its unique id ";
        String query = "SELECT * FROM InterviewSchedule where id = ?";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, id);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                interviewSchedule = new InterviewSchedule();

                interviewSchedule.setId(resultSet.getInt("id"));
                interviewSchedule.setDate(resultSet.getDate("date"));
                interviewSchedule.setEvaluation(resultSet.getBoolean("evaluation"));
                interviewSchedule.setHasBeenRejected(resultSet.getBoolean("hasBeenRejected"));
                interviewSchedule.setStudentHasInternshipId(resultSet.getInt("studentHasInternshipId"));
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
        return interviewSchedule;
    }

    /**
     * Method used to retrieve all interview scheduled dates for a given student-internship association
     *
     * @param studentHasInternshipId Identifier for the given student-internship association
     * @return Array list containing all the interview scheduled
     * @throws SQLException
     */
    public ArrayList<InterviewSchedule> getOngoingInternshipInterviewSchedules(int studentHasInternshipId) throws SQLException {
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

    /**
     * Method used to schedule a new interview for a given student-internship association
     *
     * @param date Date and time for the scheduled interview
     * @param studentHasInternshipId Identifier for the student-internship association
     * @throws SQLException
     */
    public void scheduleInterview(Timestamp date, int studentHasInternshipId) throws SQLException {
        String performedAction = " inserting a new scheduled interview into the database ";
        String query = "INSERT into InterviewSchedule (date, evaluation, hasBeenRejected, studenthasInternshipId) VALUES (?, NULL, NULL, ?)";
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setTimestamp(1, date);
            preparedStatement.setInt(2, studentHasInternshipId);

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

    public void cancelInterview(int id) throws SQLException {

    }

    /**
     * Method used to set the company decision for the given interview associated to the interviewId
     * 
     * @param interviewId Identifier for the interview
     * @param decision Company decision to upload
     * @throws SQLException
     */
    public void setCompanyDecision(int interviewId, boolean decision) throws SQLException {
        String performedAction = " setting the company decision for the given interview ";
        String query = "UPDATE InterviewSchedule " +
                    "SET evaluation = ? " +
                    "WHERE id = ?";
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setBoolean(1, decision);
            preparedStatement.setInt(2, interviewId);

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
}
