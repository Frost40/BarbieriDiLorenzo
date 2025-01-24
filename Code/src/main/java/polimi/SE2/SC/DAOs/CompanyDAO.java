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

    /**
     * Method used to uniquely identify a company type of user via email and password
     * 
     * @param email Company email
     * @param password Account password
     * @return Company associated to those credentials
     * @throws SQLException
     */
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

    /**
     * Method used to retrieve a company using its unique id
     *
     * @param id Company identifier
     * @return Company associated with the given email
     * @throws SQLException
     */
    public Company getCompanyById(int id) throws SQLException {
        Company company = null;
        String performedAction = " finding a company type user using the unique identifier ";
        String query = "SELECT * FROM Company WHERE id = ?";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, id);

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

    /**
     * Method used to retrieve a company using its unique email
     * 
     * @param email Company email
     * @return Company associated with the given email
     * @throws SQLException
     */
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

    /**
     * Method used to register a new company into the S&C database
     * 
     * @param email Company email
     * @param password Associated password
     * @param name Company name
     * @param mainOffice Company main office 
     * @param offices Other offices the company has
     * @return The aut generated id relative to the newly added company
     * @throws SQLException
     */
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
