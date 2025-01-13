package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static polimi.SE2.SC.utils.UtilityFunctions.splitStringByComma;

public class CompanyDAO {
    private Connection connection;

    public CompanyDAO(Connection connection) {
        this.connection = connection;
    }

    public Company findCompanyWithCredential(String email, String password) throws SQLException {
        Company company = null;
        String performedAction = " finding a company by username and password ";
        String query = "SELECT * FROM Company WHERE email = ? AND password = ?";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, email);
            preparedStatement.setString(2, password);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                company = new Company();
                company.setId(resultSet.getInt("id"));
                company.setEmail(resultSet.getString("email"));
                company.setPassword(resultSet.getString("password"));
                company.setName(resultSet.getString("name"));
                company.setMainOffice(resultSet.getString("mainOffice"));

                // Retrieve offices as a string and split it
                String officesString = resultSet.getString("offices");
                if (officesString != null && !officesString.isEmpty()) {
                    company.setOffices(splitStringByComma(officesString));
                } else {
                    company.setOffices(new ArrayList<>()); // Assign an empty list if null or empty
                }
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
