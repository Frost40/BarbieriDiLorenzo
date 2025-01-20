package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Complaint;
import polimi.SE2.SC.beans.Feedback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ComplaintDAO {
    private Connection connection;

    public ComplaintDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Method used to extract a specific complaint given its id 
     * 
     * @param id Complaint id
     * @return The associated compliant
     * @throws SQLException
     */
    public Complaint getComplaint(int id) throws SQLException {
        Complaint complaint = null;
        String performedAction = " extracting complaint using unique id ";
        String query = "SELECT * FROM Complaint where id = ?";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, id);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                complaint = new Complaint();

                complaint.setId(resultSet.getInt("id"));
                complaint.setText(resultSet.getString("text"));
                complaint.setRead(resultSet.getBoolean("read"));
                complaint.setClosedByStudent(resultSet.getBoolean("closedByStudent"));
                complaint.setClosedByCompany(resultSet.getBoolean("closedByCompany"));
                complaint.setStudentHasInternshipId(resultSet.getInt("studentHasInternshipId"));
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
        return complaint;
    }

    public ArrayList<Complaint> getOngoingInternshipsComplaints(ArrayList<Integer> internshipsIds) throws SQLException {
        ArrayList<Complaint> complaints = null;

        return complaints;
    }

    public Complaint getOngoingInternshipComplaints(int internshipId) throws SQLException {
        Complaint complaint = null;

        return complaint;
    }

    /**
     * Method used to retrieve all complaints a given user has not read yet
     * 
     * @param userId The user we are interested in
     * @param isStudent Boolean used to distinguish between Student and Company table in the DB
     * @return List of unread complaints
     * @throws SQLException
     */
    public ArrayList<Complaint> getNotReadComplaints(int userId, boolean isStudent) throws SQLException {
        ArrayList<Complaint> complaints = new ArrayList<>();
        String performedAction = " extracting all complaints that have not been read by the user ";
        String query = null;

        if (isStudent) {
            query = "SELECT c.*, shi.id AS studentInternshipId " +
                    "FROM Complaint c " +
                    "JOIN StudentHasInternship shi ON c.studentHasInternshipId = shi.id " +
                    "WHERE shi.studentId = ? AND c.read = FALSE;";
        } else {
            query = "SELECT c.*, shi.id AS studentInternshipId, i.id AS internshipId, i.companyId " +
                    "FROM Complaint c " +
                    "JOIN StudentHasInternship shi ON c.studentHasInternshipId = shi.id " +
                    "JOIN Internship i ON shi.internshipId = i.id " +
                    "WHERE i.companyId = ? AND c.read = FALSE;";
        }

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, userId);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Complaint complaint = new Complaint();

                complaint.setId(resultSet.getInt("id"));
                complaint.setText(resultSet.getString("text"));
                complaint.setRead(resultSet.getBoolean("read"));
                complaint.setClosedByStudent(resultSet.getBoolean("closedByStudent"));
                complaint.setClosedByCompany(resultSet.getBoolean("closedByCompany"));
                complaint.setStudentHasInternshipId(resultSet.getInt("studentHasInternshipId"));

                complaints.add(complaint);
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
        return complaints;
    }

    /**
     * Method used to retrieve all complaints relative to a specific user
     *
     * @param userId The user we are interested in
     * @param isStudent Boolean used to distinguish between Student and Company table in the DB
     * @return List of user complaints
     * @throws SQLException
     */
    public ArrayList<Complaint> getUserComplaints(int userId, boolean isStudent) throws SQLException {
        ArrayList<Complaint> complaints = new ArrayList<>();
        String performedAction = " extracting all complaints that have not been read by the user ";
        String query = null;

        if (isStudent) {
            query = "SELECT c.*, shi.id AS studentInternshipId " +
                    "FROM Complaint c " +
                    "JOIN StudentHasInternship shi ON c.studentHasInternshipId = shi.id " +
                    "WHERE shi.studentId = ?;";
        } else {
            query = "SELECT c.*, shi.id AS studentInternshipId, i.id AS internshipId, i.companyId " +
                    "FROM Complaint c " +
                    "JOIN StudentHasInternship shi ON c.studentHasInternshipId = shi.id " +
                    "JOIN Internship i ON shi.internshipId = i.id " +
                    "WHERE i.companyId = ?;";
        }

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, userId);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Complaint complaint = new Complaint();

                complaint.setId(resultSet.getInt("id"));
                complaint.setText(resultSet.getString("text"));
                complaint.setRead(resultSet.getBoolean("read"));
                complaint.setClosedByStudent(resultSet.getBoolean("closedByStudent"));
                complaint.setClosedByCompany(resultSet.getBoolean("closedByCompany"));
                complaint.setStudentHasInternshipId(resultSet.getInt("studentHasInternshipId"));

                complaints.add(complaint);
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
        return complaints;
    }


    public void uploadComplaint(String text) throws SQLException {

    }

    public void hasBeenRead(int complaintId) throws SQLException {

    }

    /**
     * Method that updated the closedByStudent or closedByCompany columns
     * based on the type of user who requested the complaint closure
     *
     * @param complaintId Complaint to be updated
     * @param requestFromStudent Boolean true if was the student to request the closure
     * @throws SQLException
     */
    public void requestClosure(int complaintId, boolean requestFromStudent) throws SQLException {
        String performedAction = " setting which between company and student requested the complaint closure ";
        String query;
        if (requestFromStudent) {
            query = "UPDATE Complaint " +
                    "SET closedByStudent = TRUE " +
                    "WHERE id = ?";
        } else {
            query = "UPDATE Complaint " +
                    "SET closedByCompany = TRUE " +
                    "WHERE id = ?";
        }
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, complaintId);

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

    public void rejectClosure(int complaintId) throws SQLException {

    }
}
