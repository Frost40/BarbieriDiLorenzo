package polimi.SE2.SC.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
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
import polimi.SE2.SC.DAOs.FeedbackDAO;
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.beans.*;
import polimi.SE2.SC.utils.ConnectionHandler;

import static polimi.SE2.SC.utils.UtilityFunctions.calculateAverageRating;
import static polimi.SE2.SC.utils.UtilityFunctions.parsingChecker;

@WebServlet("/UploadFeedback")
@MultipartConfig
public class UploadFeedback extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public UploadFeedback() {
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
        String feedbackText = request.getParameter("text");
        String ratingString = request.getParameter("rating");
        String ongoingInternshipIdString = request.getParameter("ongoingInternshipId");
        System.out.println(feedbackText);
        HttpSession session = request.getSession();
        User user    = (User)session.getAttribute("currentUser");

        int ongoingInternshipId;
        int rating;
        boolean isStudent = user instanceof Student;

        // Checking complaint validity
        if (feedbackText != null && feedbackText.length() > 255) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid feedback (a valid feedback has less than 255)!");
            return;
        }

        // Checking if the id of the given ongoing internship (studentHasInternship) is valid
        if(ongoingInternshipIdString == null || ongoingInternshipIdString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("No valid id for the ongoing internship was associated to the request!");
            return;
        }

        // Checking if the rating is valid
        if(ratingString == null || ratingString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("The rating given for the internship experience can not be null");
            return;
        }

        // Parsing process for ongoing internship id
        Optional<Integer> parsedId = parsingChecker(request, response, ongoingInternshipIdString);
        if (parsedId.isPresent())		ongoingInternshipId = parsedId.get();
        else		return;

        // Parsing process for rating
        parsedId = parsingChecker(request, response, ratingString);
        if (parsedId.isPresent())		rating = parsedId.get();
        else		return;

        // A valid rating must be a natural number between 0 and 10
        if (rating < 0 || rating > 10) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid rating (a valid rating is a natural number between 0 and 10)!");
            return;
        }

        // Making sure the association student-company exists
        StudentHasInternshipDAO studentHasInternshipDAO = new StudentHasInternshipDAO(connection);
        StudentHasInternship ongoingInternship = null;
        try {
            ongoingInternship = studentHasInternshipDAO.getStudentHasInternship(ongoingInternshipId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        if (ongoingInternship == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("There is no ongoing internship between student and company with the given id");
            return;
        }

        // Checking if the given ongoing internship is in the correct time period to upload a feedback
        if (!ongoingInternship.getApplicationStatus().equals("ended")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("You are authorized to write a feedback only after the internship period has ended");
            return;
        }

        // Only students type of user can upload their feedbacks ate the end of an internship period
        if (!isStudent) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("Only students are authorized to rate the experience");
            return;
        }

        // Checking if student is actually associated to the internship that just ended
        if (user.getId() != ongoingInternship.getStudentId()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("The feedback submission was block since your account is not associated to the given internship");
            return;
        }

        // Each student is authorized to upload a feedback once for each internship experience
        FeedbackDAO feedbackDAO = new FeedbackDAO(connection);
        ArrayList<Feedback> feedbacks = null;
        try {
            feedbacks = feedbackDAO.getFeedbacksGivenByStudent(user.getId());
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        List<Integer> feedbacksOngoingInternshipIds = feedbacks.stream().map(Communication::getStudentHasInternshipId).toList();
        if (feedbacksOngoingInternshipIds.contains(ongoingInternshipId)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("The operation was block because you can only evaluate each internship experience once");
            return;
        }

        // Adding the feedback to the database
        try {
            feedbackDAO.uploadFeedback(feedbackText, rating, ongoingInternshipId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Retrieving all feedback for the given internship
        try {
            feedbacks = feedbackDAO.getInternshipFeedbacks(ongoingInternship.getInternshipId());
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Updating the average rating for the internship given the new feedback
        double newInternshipRating = calculateAverageRating(feedbacks);
        InternshipDAO internshipDAO = new InternshipDAO(connection);
        try {
            internshipDAO.updateInternshipRating(ongoingInternship.getInternshipId(), newInternshipRating);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // JSON serialization
        Map<String, Object> jsonObject = new HashMap<>();
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Gson gson = new GsonBuilder().create();

        jsonObject.put("feedbacks", feedbacks);

        String json = gson.toJson(jsonObject);
        response.getWriter().write(json);
    }

}
