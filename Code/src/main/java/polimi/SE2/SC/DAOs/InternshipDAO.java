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

    public Internship getInternshipById(int id) throws SQLException {
        Internship internship = null;

        return internship;
    }

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

    public void modifyInternship(int id, String name, String description) throws SQLException {

    }
}
