package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Complaint;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.StudentHasInternship;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class StudentHasInternshipDAO {
    private Connection connection;

    public StudentHasInternshipDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Method used to retrieve form the database all information regarding internship and association 
     * student-internship for all studentHasInternship related to the provided student identifier
     * 
     * @param studentId Identifier for the student account
     * @return Hash map containing for each internship the relative student-internship association info
     * @throws SQLException
     */
    public HashMap<Internship, StudentHasInternship> getAllStudentInternships(int studentId) throws SQLException {
        HashMap<Internship, StudentHasInternship> allStudentInternships = new HashMap<>();
        String performedAction = " retrieving all studentHasInternships for a given student ";
        String query = "SELECT * FROM Internship i JOIN StudentHasInternship shi ON i.id = shi.internshipId WHERE shi.studentId = ?";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, studentId);

            resultSet = preparedStatement.executeQuery();

            // Process the results
            while (resultSet.next()) {
                Internship internship = new Internship();
                internship.setId(resultSet.getInt("i.id"));
                internship.setName(resultSet.getString("i.name"));
                internship.setDescription(resultSet.getString("i.description"));
                internship.setRating(resultSet.getDouble("i.rating"));
                internship.setCompanyId(resultSet.getInt("i.companyId"));

                StudentHasInternship studentHasInternship = new StudentHasInternship();
                studentHasInternship.setId(resultSet.getInt("shi.id"));
                studentHasInternship.setStartDate(resultSet.getTimestamp("shi.startDate"));
                studentHasInternship.setEndDate(resultSet.getTimestamp("shi.endDate"));
                studentHasInternship.setCompanyAccepted(resultSet.getBoolean("shi.companyAccepted"));
                studentHasInternship.setApplicationStatus(resultSet.getString("shi.applicationStatus"));
                studentHasInternship.setStudentId(resultSet.getInt("shi.studentId"));
                studentHasInternship.setInternshipId(resultSet.getInt("shi.internshipId"));

                // Add the internship and StudentHasInternship to the HashMap
                allStudentInternships.put(internship, studentHasInternship);
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

        return allStudentInternships;
    }

    /**
     * Method used to retrieve all internship of a company and, for each one, the student and the relative student-internship association
     *
     * @param companyId Identifier of the company
     * @return
     * @throws SQLException
     */
    public HashMap<Internship, HashMap<Student, StudentHasInternship>> getCompanyOngoingInternships(int companyId) throws SQLException {
        HashMap<Internship, HashMap<Student, StudentHasInternship>> companyOngoingInternships = new HashMap<>();
        String performedAction = " retrieving all internships, students, and their associations, for a given company";
        String query = "SELECT * " +
                "FROM Internship i " +
                "JOIN StudentHasInternship shi ON i.id = shi.internshipId " +
                "JOIN Student s ON shi.studentId = s.id " +
                "WHERE i.companyId = ?";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, companyId);

            resultSet = preparedStatement.executeQuery();

            // Process the results
            while (resultSet.next()) {
                // Create and populate the Internship object
                Internship internship = new Internship();
                internship.setId(resultSet.getInt("i.id"));
                internship.setName(resultSet.getString("i.name"));
                internship.setDescription(resultSet.getString("i.description"));
                internship.setRating(resultSet.getDouble("i.rating"));
                internship.setCompanyId(resultSet.getInt("i.companyId"));

                // Create and populate the Student object
                Student student = new Student();
                student.setId(resultSet.getInt("s.id"));
                student.setName(resultSet.getString("s.name"));
                student.setSurname(resultSet.getString("s.surname"));
                student.setEmail(resultSet.getString("s.email"));
                student.setPassword(resultSet.getString("s.password"));
                student.setNationality(resultSet.getString("s.nationality"));
                student.setCityOfBirth(resultSet.getString("s.cityOfBirth"));

                // Create and populate the StudentHasInternship object
                StudentHasInternship studentHasInternship = new StudentHasInternship();
                studentHasInternship.setId(resultSet.getInt("shi.id"));
                studentHasInternship.setStartDate(resultSet.getTimestamp("shi.startDate"));
                studentHasInternship.setEndDate(resultSet.getTimestamp("shi.endDate"));
                studentHasInternship.setCompanyAccepted(resultSet.getBoolean("shi.companyAccepted"));
                studentHasInternship.setApplicationStatus(resultSet.getString("shi.applicationStatus"));
                studentHasInternship.setStudentId(resultSet.getInt("shi.studentId"));
                studentHasInternship.setInternshipId(resultSet.getInt("shi.internshipId"));

                // Organize data into the HashMap structure
                companyOngoingInternships
                        .computeIfAbsent(internship, k -> new HashMap<>()) // Get or create HashMap<Student, StudentHasInternship> for this Internship
                        .put(student, studentHasInternship); // Add the Student and their association
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

        return companyOngoingInternships;
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

    /**
     * Method used to add a student application into the database
     *
     * @param internshipId Identifier for the internship
     * @param studentId Identifier for the student
     * @throws SQLException
     */
    public void addApplication(int internshipId, int studentId) throws SQLException {
        String performedAction = " inserting a new application into the DB ";
        String query = "INSERT into StudentHasInternship (startDate, endDate, companyAccepted, applicationStatus, studentId, internshipId) VALUES (NULL, NULL, FALSE, ?, ?, ?)";

        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, "evaluation");
            preparedStatement.setInt(2, studentId);
            preparedStatement.setInt(3, internshipId);


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
     * Method used to update the company decision regarding a specific application request from a student
     * In case of a positive evaluation, the application state advances to 'selectionProcess'
     *
     * @param id Identifier for the student application request
     * @param status Boolean variable: TRUE if the application is accepted, FALSE otherwise
     * @throws SQLException
     */
    public void updateApplicationStatus(int id, boolean status) throws SQLException {
        System.out.println(status);
        String performedAction = " updating the application status ";
        String query = "UPDATE StudentHasInternship " +
                "SET companyAccepted = ?, applicationStatus = ? " +
                "WHERE id = ?";
        String applicationStatus;
        if (status) {
            applicationStatus = "selectionProcess";
        } else applicationStatus = "rejected";

        PreparedStatement preparedStatement = null;
        System.out.println(applicationStatus);

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setBoolean(1, status);
            preparedStatement.setString(2, applicationStatus);
            preparedStatement.setInt(3, id);


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
     * Method used to update the given internship period (studentHasInternship) with the current date as end date
     *
     * @param id Identifier for the internship period experience
     * @throws SQLException
     */
    public void endInternshipExperience(int id) throws SQLException {
        String performedAction = " setting the end date for the internship experience ";
        String query = "UPDATE StudentHasInternship " +
                    "SET endDate = CURDATE() AND applicationStatus = ? " +
                    "WHERE id = ?";

        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, "ended");
            preparedStatement.setInt(2, id);


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
     * Method used to update the given internship period (studentHasInternship) with the current date as start date
     *
     * @param id Identifier for the internship period experience
     * @throws SQLException
     */
    public void startInternshipExperience(int id) throws SQLException {
        String performedAction = " setting the start date for the internship experience ";
        String query = "UPDATE StudentHasInternship " +
                "SET startDate = CURDATE() AND applicationStatus = ? " +
                "WHERE id = ?";

        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, "ongoing");
            preparedStatement.setInt(2, id);

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
