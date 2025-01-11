package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.InterviewSchedule;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class InterviewScheduleDAO {
    private Connection connection;

    public InterviewScheduleDAO(Connection connection) {
        this.connection = connection;
    }

    public HashMap<String, ArrayList<InterviewSchedule>> getOngoingInternshipsInterviewSchedules(ArrayList<Integer> internshipsId) throws SQLException {
        HashMap<String, ArrayList<InterviewSchedule>> internshipsWithInterviews = new HashMap<>();

        return internshipsWithInterviews;
    }

    public void scheduleInterview(Date date) throws SQLException {

    }

    public void cancelInterview(int id) throws SQLException {

    }

    public void setCompanyDecision(int interviewId, boolean decision) throws SQLException {

    }
}
