package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Complaint;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.StudentHasInternship;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class StudentHasInternshipDAO {
    private Connection connection;

    public StudentHasInternshipDAO(Connection connection) {
        this.connection = connection;
    }

    public ArrayList<StudentHasInternship> getAllStudentInternships(int studentId) throws SQLException {
        ArrayList<StudentHasInternship> studentHasInternships = null;

        return studentHasInternships;
    }

    public ArrayList<StudentHasInternship> getCompanyOngoingInternships(int companyId) throws SQLException {
        ArrayList<StudentHasInternship> studentHasInternships = null;

        return studentHasInternships;
    }

    /**
     * Method used to retrieve a specific student-internship experience independently of the application status
     * 
     * @param id Identifier for the internship experience (studentHasInternship)
     * @return The corresponding internship period experience present in the database
     * @throws SQLException
     */
    public StudentHasInternship getStudentHasInternship (int id) throws SQLException {
        StudentHasInternship studentHasInternship = null;
        String performedAction = " extracting student-internship experience (studentHasInternship) via id ";
        String query = "SELECT * FROM StudentHasInternship where id = ?";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, id);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                studentHasInternship = new StudentHasInternship();

                studentHasInternship.setId(resultSet.getInt("id"));
                studentHasInternship.setStartDate(resultSet.getDate("startDate"));
                studentHasInternship.setEndDate(resultSet.getDate("endDate"));
                studentHasInternship.setCompanyAccepted(resultSet.getBoolean("companyAccepted"));
                studentHasInternship.setApplicationStatus(resultSet.getString("applicationStatus"));
                studentHasInternship.setStudentId(resultSet.getInt("studentId"));
                studentHasInternship.setInternshipId(resultSet.getInt("internshipId"));
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
        return studentHasInternship;
    }

    public void addApplication(int internshipId, int studentId) throws SQLException {

    }

    public void updateApplicationStatus(int id, String status) throws SQLException {

    }

    /**
     * Method used to update the given internship period (studentHasInternship) with the current date as end date
     *
     * @param id Identifier for the internship period experience
     * @throws SQLException
     */
    public void endInternshipExperience(int id) throws SQLException {
        String performedAction = " setting the end date for the internship experience ";
        String query = "UPDATE StudentHasInternship " +
                    "SET endDate = CURDATE() " +
                    "WHERE id = ?";

        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, id);

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
