package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.StudentHasInternship;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

public class StudentHasInternshipDAO {
    private Connection connection;

    public StudentHasInternshipDAO(Connection connection) {
        this.connection = connection;
    }

    public void acceptRecommendationStudent(int id) throws SQLException {

    }

    public void acceptRecommendationCompany(int id) throws SQLException {

    }

    public void declineRecommendationStudent(int id) throws SQLException {

    }

    public void declineRecommendationCompany(int id) throws SQLException {

    }

    public ArrayList<StudentHasInternship> getAllStudentInternships(int studentId) throws SQLException {
        ArrayList<StudentHasInternship> studentHasInternships = null;

        return studentHasInternships;
    }

    public ArrayList<StudentHasInternship> getCompanyOngoingInternships(int companyId) throws SQLException {
        ArrayList<StudentHasInternship> studentHasInternships = null;

        return studentHasInternships;
    }

    public StudentHasInternship getOngoingInternship (int id) throws SQLException {
        StudentHasInternship studentHasInternship = null;

        return studentHasInternship;
    }

    public void addApplication(int internshipId, int studentId) throws SQLException {

    }

    public void updateApplicationStatus(int id, String status) throws SQLException {

    }
}
