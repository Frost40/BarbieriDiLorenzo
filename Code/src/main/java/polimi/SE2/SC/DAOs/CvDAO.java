package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Complaint;
import polimi.SE2.SC.beans.Cv;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static polimi.SE2.SC.utils.UtilityFunctions.splitStringByComma;

public class CvDAO {
    private Connection connection;

    public CvDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Method used to retrieve the unique CV associated to the id of its owner
     * 
     * @param studentId Student owner of the CV
     * @return The extracted CV
     * @throws SQLException
     */
    public Cv getStudentCv(int studentId) throws SQLException {
        Cv cv = null;
        String performedAction = " extracting the CV relative to the given studentId ";
        String query = "SELECT * FROM Cv where studentId = ?";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, studentId);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                cv = new Cv();

                cv.setId(resultSet.getInt("id"));
                cv.setFilePath(resultSet.getString("filePath"));
                cv.setStudentId(resultSet.getInt("studentId"));

                // Retrieve other data as a string and splitting it based on the comma
                String tmp = resultSet.getString("experiences");
                if (tmp != null && !tmp.isEmpty()) {
                    cv.setExperiences(splitStringByComma(tmp));
                } else {
                    cv.setExperiences(new ArrayList<>()); // Assign an empty list if null or empty
                }
                tmp = resultSet.getString("programmingSkills");
                if (tmp != null && !tmp.isEmpty()) {
                    cv.setProgrammingSkills(splitStringByComma(tmp));
                } else {
                    cv.setProgrammingSkills(new ArrayList<>()); // Assign an empty list if null or empty
                }
                tmp = resultSet.getString("otherSkills");
                if (tmp != null && !tmp.isEmpty()) {
                    cv.setOtherSkills(splitStringByComma(tmp));
                } else {
                    cv.setOtherSkills(new ArrayList<>()); // Assign an empty list if null or empty
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
        return cv;
    }

    public ArrayList<Cv> getAllCvs() throws SQLException {
        ArrayList<Cv> cvs = null;

        return cvs;
    }

    public void uploadNewCv(String filePath, String experiences, String programmingSkills, String otherSkills) throws SQLException {

    }

    /**
     * Method used to update the experiences, programming skills and other general skills in an existing CV
     * 
     * @param id Cv to update
     * @param experiences New set of experiences to be added
     * @param programmingSkills New set of programmingSkills to be added
     * @param otherSkills New set of otherSkills to be added
     * @throws SQLException
     */
    public void updateCv(int id, String experiences, String programmingSkills, String otherSkills) throws SQLException {
        String performedAction = " overwriting data in an already existing CV ";
        String query = "UPDATE Cv " +
                "SET experiences = ?, programmingSkills = ?, otherSkills = ? " +
                "WHERE id = ?";
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, experiences);
            preparedStatement.setString(2, programmingSkills);
            preparedStatement.setString(3, otherSkills);
            preparedStatement.setInt(4, id);

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
