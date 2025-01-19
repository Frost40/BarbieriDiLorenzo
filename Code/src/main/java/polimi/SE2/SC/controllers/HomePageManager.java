package polimi.SE2.SC.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import polimi.SE2.SC.DAOs.*;
import polimi.SE2.SC.beans.*;
import polimi.SE2.SC.utils.ConnectionHandler;

@WebServlet("/HomePageManager")
@MultipartConfig
public class HomePageManager extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public HomePageManager() {
        super();
    }

    @Override
    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init() throws ServletException {
        ServletContext servletContext = getServletContext();
        this.connection = ConnectionHandler.getConnection(servletContext);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        CvDAO cvDAO = null;
        InternshipDAO internshipDAO = null;
        boolean isStudent = false;

        Cv cv = null;
        ArrayList<Internship> internships = null;

        if (user instanceof Student student) {
            // Handle student-specific logic
            cvDAO = new CvDAO(connection);
            try {
                cv = cvDAO.getStudentCv(user.getId());
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println(e.getMessage());
                return;
            }

            isStudent = true;

        } else if (user instanceof Company company) {
            // Handle company-specific logic
            internshipDAO = new InternshipDAO(connection);
            try {
                internships = internshipDAO.getCompanyInternships(user.getId());
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println(e.getMessage());
                return;
            }
        }

        MessageDAO messageDAO = new MessageDAO(connection);
        ArrayList<Message> unreadMessages = null;
        try {
            unreadMessages = messageDAO.getNotReadMessages(user.getId(), isStudent);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        ComplaintDAO complaintDAO = new ComplaintDAO(connection);
        ArrayList<Complaint> unreadComplaints = null;
        try {
            unreadComplaints = complaintDAO.getNotReadComplaints(user.getId(), isStudent);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        FeedbackDAO feedbackDAO = new FeedbackDAO(connection);
        ArrayList<Feedback> unreadFeedbacks = null;
        try {
            unreadFeedbacks = feedbackDAO.getNotReadFeedbacks(user.getId());
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // JSON serialization
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Gson gson = new GsonBuilder().create();

        Map<String, Object> jsonObject = new HashMap<>();
        if (isStudent) {
            jsonObject.put("cv", cv);
        } else {
            jsonObject.put("internships", internships);
        }
        jsonObject.put("unreadMessages", unreadMessages);
        jsonObject.put("unreadComplaints", unreadComplaints);
        jsonObject.put("unreadFeedbacks", unreadFeedbacks);

        String json = gson.toJson(jsonObject);
        response.getWriter().write(json);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}
}
