package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Student;

import java.sql.*;
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

    public Company getCompanyByEmail(String email) throws SQLException {
        Company company = null;
        String performedAction = " finding a company type user by email ";
        String query = "SELECT * FROM Company WHERE email = ?";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, email);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                company = new Company();

                company.setId(resultSet.getInt("id"));
                company.setPassword(resultSet.getString("password"));
                company.setEmail(resultSet.getString("email"));
                company.setName(resultSet.getString("name"));
                company.setMainOffice(resultSet.getString("mainOffice"));

                // Retrieve offices as a string and split it
                String officesString = resultSet.getString("offices");
                if (officesString != null && !officesString.isEmpty()) {
                    company.setOffices(splitStringByComma(officesString));
                } else {
                    company.setOffices(new ArrayList<>()); // Assign an empty list if null or empty
                }            }

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

    public int registerCompany(String email, String password, String name, String mainOffice, String offices) throws SQLException {
        int generatedId = 0;
        String performedAction = " creating a new company in the database ";
        String query = "INSERT Into Company (email,password,name,mainOffice,offices) VALUES(?,?,?,?,?)";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            // Deactivate auto-commit to ensure an atomic transaction
            connection.setAutoCommit(false);

            preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, email);
            preparedStatement.setString(2, password);
            preparedStatement.setString(3, name);
            preparedStatement.setString(4, mainOffice);
            preparedStatement.setString(5, offices);

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
