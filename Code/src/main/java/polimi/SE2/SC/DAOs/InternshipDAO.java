package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class InternshipDAO {
    private Connection connection;

    public InternshipDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Method used to retrieve all internships created by a specific company account given its unique id
     * 
     * @param companyId Company identifier
     * @return Array list containing all internships of the company
     * @throws SQLException
     */
    public ArrayList<Internship> getCompanyInternships(int companyId) throws SQLException {
        ArrayList<Internship> internships = new ArrayList<>();
        String performedAction = " extracting all internships of a specific company ";
        String query = "SELECT * FROM Internship WHERE companyId = ?";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, companyId);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Internship internship = new Internship();

                internship.setId(resultSet.getInt("id"));
                internship.setName(resultSet.getString("name"));
                internship.setDescription(resultSet.getString("description"));
                internship.setRating(resultSet.getInt("rating"));
                internship.setCompanyId(resultSet.getInt("companyId"));

                internships.add(internship);
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

        return internships;
    }

    /**
     * Method used to retrieve a specific internship from the database given its unique id
     * 
     * @param id Internship identifier
     * @return Internship associated to the given id
     * @throws SQLException
     */
    public Internship getInternshipById(int id) throws SQLException {
        Internship internship = null;
        String performedAction = " extracting a specific internship given its unique id ";
        String query = "SELECT * FROM Internship WHERE id = ?";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, id);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                internship = new Internship();

                internship.setId(resultSet.getInt("id"));
                internship.setName(resultSet.getString("name"));
                internship.setDescription(resultSet.getString("description"));
                internship.setRating(resultSet.getInt("rating"));
                internship.setCompanyId(resultSet.getInt("companyId"));
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
        
        return internship;
    }

    /**
     * Method used to retrieve from the database all internships registered in the S&C app
     *
     * @return Array list containg all available internships
     * @throws SQLException
     */
    public ArrayList<Internship> getAllInternships() throws SQLException {
        ArrayList<Internship> internships = new ArrayList<>();
        String performedAction = " extracting all internships available in S&C ";
        String query = "SELECT * FROM Internship";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Internship internship = new Internship();

                internship.setId(resultSet.getInt("id"));
                internship.setName(resultSet.getString("name"));
                internship.setDescription(resultSet.getString("description"));
                internship.setRating(resultSet.getInt("rating"));
                internship.setCompanyId(resultSet.getInt("companyId"));

                internships.add(internship);
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
        return internships;
    }

    public void uploadNewInternship(String name, String description) throws SQLException {

    }

    /**
     * Method used to update info of an already existing internship given its unique id
     * 
     * @param id Identifier for the internship to modify 
     * @param name Name to be added into the database
     * @param description Description to be added into the database
     * @throws SQLException
     */
    public void modifyInternship(int id, String name, String description) throws SQLException {
        String performedAction = " overwriting data from an already existing internship ";
        String query =  "UPDATE Internship " +
                        "SET name = ?, description = ? " +
                        "WHERE id = ?";
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, description);
            preparedStatement.setInt(3, id);

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new SQLException("Error accessing the DB when" + performedAction + "[ " + e.getMessage() + " ]");

        } finally {

            try {
                preparedStatement.close();

            } catch (Exception e) {
                throw new SQLException("Error closing the statement when" + performedAction + "[ " + e.getMessage() + " ]");
            }
        }
    }
}
