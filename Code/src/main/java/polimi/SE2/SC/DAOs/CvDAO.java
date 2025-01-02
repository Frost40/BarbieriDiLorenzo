package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Cv;

import java.sql.SQLException;
import java.util.ArrayList;

public class CvDAO {
    public Cv getStudentCv(int studentId) throws SQLException {
        Cv cv = null;

        return cv;
    }

    public ArrayList<Cv> getAllCvs() throws SQLException {
        ArrayList<Cv> cvs = null;

        return cvs;
    }

    public void uploadNewCv(String filePath, String experiences, String programmingSkills, String otherSkills) throws SQLException {

    }

    public void updateCv(String experiences, String programmingSkills, String otherSkills) throws SQLException {

    }
}
