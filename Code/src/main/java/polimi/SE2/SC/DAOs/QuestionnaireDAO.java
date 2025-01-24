package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Questionnaire;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class QuestionnaireDAO {
    private Connection connection;

    public QuestionnaireDAO(Connection connection) {
        this.connection = connection;
    }

    public HashMap<String, ArrayList<Questionnaire>> getOngoingInternshipsQuestionnaires(ArrayList<Integer> internshipsIds) throws SQLException {
        HashMap<String, ArrayList<Questionnaire>> internshipsWithQuestionnaires = new HashMap<>();

        return internshipsWithQuestionnaires;
    }

    public void uploadQuestionnaire(InputStream file) throws SQLException {

    }

    public void setCompanyDecision(int questionnaireId, boolean decision) throws SQLException {

    }

    public void deleteQuestionnaire(int questionnaireId) throws SQLException {

    }
}
