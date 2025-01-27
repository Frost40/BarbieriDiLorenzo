package polimi.SE2.SC.controllers;

import polimi.SE2.SC.DAOs.ComplaintDAO;
import polimi.SE2.SC.DAOs.FeedbackDAO;
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.beans.*;
import polimi.SE2.SC.utils.ConnectionHandler;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static polimi.SE2.SC.utils.UtilityFunctions.parsingChecker;

@WebServlet("/ReadFeedback")
@MultipartConfig
public class ReadFeedback extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public ReadFeedback() {
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

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String feedbackIdString = request.getParameter("feedbackId");

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        int feedbackId;
        boolean isStudent = user instanceof Student;

        // Checking if the given id is valid
        if(feedbackIdString == null || feedbackIdString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("The id for the complaint is null!");
            return;
        }

        // Parsing the feedback ID string back into an integer
        Optional<Integer> parsedId = parsingChecker(request, response, feedbackIdString);
        if (parsedId.isPresent())			feedbackId = parsedId.get();
        else	return;

        // Retrieving the feedback from the database
        FeedbackDAO feedbackDAO = new FeedbackDAO(connection);
        Feedback feedback = null;
        try {
            feedback = feedbackDAO.getFeedback(feedbackId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Checking that the feedback exists in the DB
        if (feedback == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("The feedback you are trying to close does not exists");
            return;
        }

        // Retrieving the studentHasInternship associated with the feedback
        StudentHasInternshipDAO studentHasInternshipDAO = new StudentHasInternshipDAO(connection);
        StudentHasInternship studentHasInternship = null;
        try {
            studentHasInternship = studentHasInternshipDAO.getStudentHasInternship(feedback.getStudentHasInternshipId());
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Only companies receive feedbacks
        if (isStudent) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("Only companies receive feedbacks");
            return;
        }

        // Checking that the company in the current session is actually the owner of the internship associated to the feedback
        InternshipDAO internshipDAO = new InternshipDAO(connection);
        ArrayList<Internship> companyInternships = null;
        try {
            companyInternships = internshipDAO.getCompanyInternships(user.getId());
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        List<Integer> companyInternshipsIds = companyInternships.stream().map(Internship::getId).toList();
        if (!companyInternshipsIds.contains(studentHasInternship.getInternshipId())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("The operation was block because you are not the company responsible for the given internship");
            return;
        }

        // Setting the feedback to read
        try {
            feedbackDAO.hasBeenRead(feedbackId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Servlet ended successfully
        response.setStatus(HttpServletResponse.SC_OK);
    }
}