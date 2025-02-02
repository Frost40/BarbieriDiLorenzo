package polimi.SE2.SC.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import polimi.SE2.SC.DAOs.FeedbackDAO;
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.InterviewScheduleDAO;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static polimi.SE2.SC.utils.UtilityFunctions.calculateAverageRating;
import static polimi.SE2.SC.utils.UtilityFunctions.parsingChecker;

@WebServlet("/ScheduleInterview")
@MultipartConfig
public class ScheduleInterview extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;
    protected StudentHasInternshipDAO createStudentHasInternshipDAO() {
        return new StudentHasInternshipDAO(connection);
    }
    protected InternshipDAO createInternshipDAO() {
        return new InternshipDAO(connection);
    }
    protected InterviewScheduleDAO createInterviewScheduleDAO() {
        return new InterviewScheduleDAO(connection);
    }
    public ScheduleInterview() {
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
        String dateString = request.getParameter("date");
        String ongoingInternshipIdString = request.getParameter("studentHasInternshipId");

        HttpSession session = request.getSession();
        User user    = (User)session.getAttribute("currentUser");

        int ongoingInternshipId;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm"); // Format includes hours and minutes
        dateFormat.setLenient(false); // Strict parsing to avoid invalid dates or times
        java.sql.Timestamp parsedSQLDate = null;
        boolean isStudent = user instanceof Student;

        // Checking that the field data contains actual information
        if (dateString == null || dateString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid or missing date parameter!");
            return;
        }

        // Checking if the id of the given ongoing internship (studentHasInternship) is valid
        if(ongoingInternshipIdString == null || ongoingInternshipIdString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("No valid id for the ongoing internship was associated with the request!");
            return;
        }
        // Parsing process for the given date and time
        try {
            Date parsedDate = dateFormat.parse(dateString);
            System.out.println("Parsed date with time: " + parsedDate);

            // Convert to SQL Timestamp
            parsedSQLDate = new java.sql.Timestamp(parsedDate.getTime());
            System.out.println("SQL Timestamp (yyyy-MM-dd HH:mm:ss): " + parsedSQLDate);

        } catch (ParseException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid date format. Expected format: yyyy-MM-dd HH:mm.");
            return;
        }

        // Parsing process for ongoing internship id
        Optional<Integer> parsedId = parsingChecker(request, response, ongoingInternshipIdString);
        if (parsedId.isPresent())		ongoingInternshipId = parsedId.get();
        else		return;

        // Making sure the association student-internship exists
        StudentHasInternshipDAO studentHasInternshipDAO = createStudentHasInternshipDAO();
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

        // Checking if the given ongoing internship is in the correct time period to schedule an interview
        if (!ongoingInternship.getApplicationStatus().equals("selectionProcess")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("You are not authorized to schedule an interview in the current time period");
            return;
        }

        // Checking that the user in the current session is a company
        if (isStudent) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("Only the company is authorized to manage a student application request");
            return;
        }

        // Checking that the company in the current session is actually the owner of the given internship
        InternshipDAO internshipDAO = createInternshipDAO();
        ArrayList<Internship> companyInternships = null;
        try {
            companyInternships = internshipDAO.getCompanyInternships(user.getId());
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        List<Integer> companyInternshipsIds = companyInternships.stream().map(Internship::getId).toList();
        if (!companyInternshipsIds.contains(ongoingInternship.getInternshipId())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("The operation was block because you are not the company responsible for the given internship");
            return;
        }

        // Adding the scheduled interview to the database
        InterviewScheduleDAO interviewScheduleDAO = createInterviewScheduleDAO();
        try {
            interviewScheduleDAO.scheduleInterview(parsedSQLDate, ongoingInternshipId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Retrieving all interview schedule for the given student-internship association
        ArrayList<InterviewSchedule> interviewSchedules = null;
        try {
            interviewSchedules = interviewScheduleDAO.getOngoingInternshipInterviewSchedules(ongoingInternshipId);
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

        jsonObject.put("interviewSchedules", interviewSchedules);

        String json = gson.toJson(jsonObject);
        response.getWriter().write(json);
    }
}