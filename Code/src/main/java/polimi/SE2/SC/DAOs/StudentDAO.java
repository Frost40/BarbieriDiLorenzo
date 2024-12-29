package polimi.SE2.SC.DAOs;

import java.sql.Connection;
import polimi.SE2.SC.beans.Student;

public class StudentDAO {
    private Connection connection;

    public StudentDAO(Connection connection) {
        this.connection = connection;
    }
    private Student findStudentWithCredentials(String email, String password){
        Student student = null;

        return student;
    }

    private int registerStudent(String email, String password) {
        int generatedId = 0;

        return generatedId;
    }
}
