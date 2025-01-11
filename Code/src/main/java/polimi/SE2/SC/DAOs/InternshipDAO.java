package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Internship;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

public class InternshipDAO {
    private Connection connection;

    public InternshipDAO(Connection connection) {
        this.connection = connection;
    }

    public ArrayList<Internship> getCompanyInternships(int companyId) throws SQLException {
        ArrayList<Internship> internships = null;

        return internships;
    }

    public Internship getInternshipById(int id) throws SQLException {
        Internship internship = null;

        return internship;
    }

    public ArrayList<Internship> getAllInternships() throws SQLException {
        ArrayList<Internship> internships = null;

        return internships;
    }

    public void uploadNewInternship(String name, String description) throws SQLException {

    }

    public void modifyInternship(int id, String name, String description) throws SQLException {

    }
}
