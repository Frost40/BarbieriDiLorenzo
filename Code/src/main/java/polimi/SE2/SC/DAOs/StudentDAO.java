package polimi.SE2.SC.DAOs;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import polimi.SE2.SC.beans.Student;

public class StudentDAO {
    private Connection connection;

    public StudentDAO(Connection connection) {
        this.connection = connection;
    }

    private Student findStudentWithCredentials(String email, String password) throws SQLException {
        Student student = null;

        return student;
    }

    private Student getStudentById(int id) throws SQLException {
        Student student = null;

        return student;
    }

    private ArrayList<Student> getAllStudents() throws SQLException {
        ArrayList<Student> students = null;

        return students;
    }

    private int registerStudent(String email, String password, String name, String userType, String surname, String nationality, String cityOfBrith) throws SQLException {
        int generatedId = 0;

        return generatedId;
    }
}
