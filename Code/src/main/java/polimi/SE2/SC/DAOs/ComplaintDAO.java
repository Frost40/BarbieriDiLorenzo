package polimi.SE2.SC.DAOs;

import polimi.SE2.SC.beans.Complaint;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

public class ComplaintDAO {
    private Connection connection;

    public ComplaintDAO(Connection connection) {
        this.connection = connection;
    }

    public ArrayList<Complaint> getOngoingInternshipsComplaints(ArrayList<Integer> internshipsIds) throws SQLException {
        ArrayList<Complaint> complaints = null;

        return complaints;
    }

    public Complaint getOngoingInternshipComplaints(int internshipId) throws SQLException {
        Complaint complaint = null;

        return complaint;
    }

    public void uploadComplaint(String text) throws SQLException {

    }

    public void hasBeenRead(int complaintId) throws SQLException {

    }

    public void requestClosure(int complaintId, boolean requestFromStudent) throws SQLException {

    }

    public void rejectClosure(int complaintId) throws SQLException {

    }
}
