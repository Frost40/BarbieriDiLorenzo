package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Feedback;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class FeedbackDAO {

    public HashMap<String, ArrayList<Feedback>> getInternshipsFeedbacks(ArrayList<Integer> internshipsIds) throws SQLException {
        HashMap<String, ArrayList<Feedback>> internshipsWithFeedbacks = new HashMap<>();

        return internshipsWithFeedbacks;
    }

    public ArrayList<Feedback> getInternshipFeedbacks(int internshipId) throws SQLException {
        ArrayList<Feedback> feedbacks = null;

        return feedbacks;
    }

    public void uploadFeedback(String text, int rating) throws SQLException {

    }

    public void hasBeenRead(int feedbackId) throws SQLException {

    }
}
