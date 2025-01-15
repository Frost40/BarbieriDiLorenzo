package polimi.SE2.SC.DAOs;

import java.sql.*;
import java.util.ArrayList;

import polimi.SE2.SC.beans.Student;

public class StudentDAO {
    private Connection connection;

    public StudentDAO(Connection connection) {
        this.connection = connection;
    }

    public Student findStudentWithCredentials(String email, String password) throws SQLException {
        Student student = null;
        String performedAction = " finding a student by email and password ";
        String query = "SELECT * FROM Student WHERE email = ? AND password = ?";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, email);
            preparedStatement.setString(2, password);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                student = new Student();
                student.setId(resultSet.getInt("id"));
                student.setEmail(resultSet.getString("email"));
                student.setPassword(resultSet.getString("password"));
                student.setName(resultSet.getString("name"));
                student.setSurname(resultSet.getString("surname"));
                student.setNationality(resultSet.getString("nationality"));
                student.setCityOfBirth(resultSet.getString("cityOfBirth"));
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

        return student;
    }

    public Student getStudentById(int id) throws SQLException {
        Student student = null;

        return student;
    }

    public Student getStudentByEmail(String email) throws SQLException {
        Student student = null;
        String performedAction = " finding a user by email ";
        String query = "SELECT * FROM Student WHERE email = ?";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, email);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                student = new Student();

                student.setId(resultSet.getInt("id"));
                student.setPassword(resultSet.getString("password"));
                student.setEmail(resultSet.getString("email"));
                student.setName(resultSet.getString("name"));
                student.setSurname(resultSet.getString("surname"));
                student.setNationality(resultSet.getString("nationality"));
                student.setCityOfBirth(resultSet.getString("cityOfBirth"));
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

        return student;
    }

    public ArrayList<Student> getAllStudents() throws SQLException {
        ArrayList<Student> students = null;

        return students;
    }

    public int registerStudent(String email, String password, String name, String surname, String nationality, String cityOfBrith) throws SQLException {
        int generatedId = 0;
        String performedAction = " creating a new company in the database ";
        String query = "INSERT Into Student (email,password,name,surname,nationality,cityOfBirth) VALUES(?,?,?,?,?,?)";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            // Deactivate auto-commit to ensure an atomic transaction
            connection.setAutoCommit(false);

            preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, email);
            preparedStatement.setString(2, password);
            preparedStatement.setString(3, name);
            preparedStatement.setString(4, surname);
            preparedStatement.setString(5, nationality);
            preparedStatement.setString(6, cityOfBrith);

            preparedStatement.executeUpdate();

            resultSet = preparedStatement.getGeneratedKeys();

            if (resultSet.next()) {
                generatedId =  resultSet.getInt(1);
            } else {
                throw new SQLException("No generated key was returned after inserting a new user into the database.");
            }

            connection.commit();

        } catch (SQLException e) {
            // If an SQLException is raised a roll-back to the last commit is needed
            connection.rollback();
            throw new SQLException("Error accessing the DB when" + performedAction + "[ " + e.getMessage() + " ]");

        } finally {
            // When the transaction is completed (successfully or not) we need to resume the
            // auto-commit
            connection.setAutoCommit(true);

            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (Exception e) {
                throw new SQLException("Error closing the result set when" + performedAction + "[ " + e.getMessage() + " ]");
            }

            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (Exception e) {
                throw new SQLException("Error closing the statement when" + performedAction + "[ " + e.getMessage() + " ]");
            }
        }

        return generatedId;
    }
}
