package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Complaint;
import polimi.SE2.SC.beans.Feedback;
import polimi.SE2.SC.beans.Student;

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
        String performedAction = " extracting complaint using its unique id ";
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
                complaint.setReadByStudent(resultSet.getBoolean("readByStudent"));
                complaint.setReadByInternship(resultSet.getBoolean("readByInternship"));
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

    /**
     * Method used to retrieve from the database all complaints that are still
     * open for a giving ongoing internship (studentHasInternship connection)
     *
     * @param studentHasInternshipId Identifier for the ongoing internship
     * @return Array list containing all extracted complaints
     * @throws SQLException
     */
    public ArrayList<Complaint> getOngoingInternshipOpenComplaints(int studentHasInternshipId) throws SQLException {
        ArrayList<Complaint> complaints = new ArrayList<>();
        String performedAction = " extracting all complaints that are still open for a given ongoing internship ";
        String query = "SELECT * FROM Complaint WHERE studentHasInternshipId = ? AND NOT (closedByCompany = TRUE AND closedByStudent = TRUE)";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, studentHasInternshipId);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Complaint complaint = new Complaint();

                complaint.setId(resultSet.getInt("id"));
                complaint.setText(resultSet.getString("text"));
                complaint.setReadByStudent(resultSet.getBoolean("readByStudent"));
                complaint.setReadByInternship(resultSet.getBoolean("readByInternship"));
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
                    "WHERE shi.studentId = ? AND c.readByStudent = FALSE;";
        } else {
            query = "SELECT c.*, shi.id AS studentInternshipId, i.id AS internshipId, i.companyId " +
                    "FROM Complaint c " +
                    "JOIN StudentHasInternship shi ON c.studentHasInternshipId = shi.id " +
                    "JOIN Internship i ON shi.internshipId = i.id " +
                    "WHERE i.companyId = ? AND c.readByInternship = FALSE;";
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
                complaint.setReadByStudent(resultSet.getBoolean("readByStudent"));
                complaint.setReadByInternship(resultSet.getBoolean("readByInternship"));
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
        String performedAction = " extracting all complaints relative to a user ";
        String query = null;

        if (isStudent) {
            query = "SELECT c.*, shi.id AS studentInternshipId " +
                    "FROM Complaint c " +
                    "JOIN StudentHasInternship shi ON c.studentHasInternshipId = shi.id " +
                    "WHERE shi.studentId = ?";
        } else {
            query = "SELECT c.*, shi.id AS studentInternshipId, i.id AS internshipId, i.companyId " +
                    "FROM Complaint c " +
                    "JOIN StudentHasInternship shi ON c.studentHasInternshipId = shi.id " +
                    "JOIN Internship i ON shi.internshipId = i.id " +
                    "WHERE i.companyId = ?";
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
                complaint.setReadByStudent(resultSet.getBoolean("readByStudent"));
                complaint.setReadByInternship(resultSet.getBoolean("readByInternship"));
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
     * Method used to upload a new complaint into the database
     * It automatically set the 'readByStudent' and 'readByInternship' to TRUE or FALSE depending on the user type who did the upload
     *
     * @param text Message text describing the problem the user wants to complaint about
     * @param studentHasInternshipId Identifier for the ongoing internship the complaint is related to
     * @param uploadedByStudent Boolean variable used to inject two different queries into the database depending on which type of user did the upload
     * @throws SQLException
     */
    public void uploadComplaint(String text, int studentHasInternshipId, boolean uploadedByStudent) throws SQLException {
        String performedAction = " inserting a new complaint into the DB ";
        String query;
        if (uploadedByStudent) {
            query = "INSERT into Complaint (text, readByStudent, readByInternship, closedByStudent, closedByCompany, studentHasInternshipId) VALUES (?, TRUE, FALSE, FALSE, FALSE, ?)";
        } else {
            query = "INSERT into Complaint (text, readByStudent, readByInternship, closedByStudent, closedByCompany, studentHasInternshipId) VALUES (?, FALSE, TRUE, FALSE, FALSE, ?)";
        }
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, text);
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

    /**
     * Method used to reject the closure of a given complaint
     *
     * @param complaintId Identifier for the complaint
     * @throws SQLException
     */
    public void rejectClosure(int complaintId) throws SQLException {
        String performedAction = " rejecting the complaint closure ";
        String query = "UPDATE Complaint " +
                    "SET closedByCompany = FALSE and closedByStudent = FALSE " +
                    "WHERE id = ?";
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
}
