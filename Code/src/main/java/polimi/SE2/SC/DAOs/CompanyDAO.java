package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Company;

import java.sql.Connection;
import java.sql.SQLException;

public class CompanyDAO {
    private Connection connection;

    public CompanyDAO(Connection connection) {
        this.connection = connection;
    }

    public Company findCompanyWithCredential(String email, String password) throws SQLException {
        Company company = null;

        return company;
    }

    public Company getCompanyById(int id) throws SQLException {
        Company company = null;

        return company;
    }

    public int registerCompany(String email, String password, String name, String userType, String mainOffice, String offices) throws SQLException {
        int generatedID = 0;

        return generatedID;
    }
}
