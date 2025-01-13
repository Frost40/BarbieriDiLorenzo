package polimi.SE2.SC.DAOs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import polimi.SE2.SC.beans.Student;

public class StudentDAO {
    private Connection connection;

    public StudentDAO(Connection connection) {
        this.connection = connection;
    }

    public Student findStudentWithCredentials(String email, String password) throws SQLException {
        Student student = null;
        String performedAction = " finding a user by username and password ";
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

    public ArrayList<Student> getAllStudents() throws SQLException {
        ArrayList<Student> students = null;

        return students;
    }

    public int registerStudent(String email, String password, String name, String userType, String surname, String nationality, String cityOfBrith) throws SQLException {
        int generatedId = 0;

        return generatedId;
    }
}
