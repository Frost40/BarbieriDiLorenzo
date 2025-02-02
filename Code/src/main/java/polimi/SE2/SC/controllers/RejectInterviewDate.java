package polimi.SE2.SC.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.InterviewScheduleDAO;
import polimi.SE2.SC.DAOs.QuestionnaireDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.beans.*;
import polimi.SE2.SC.utils.ConnectionHandler;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static polimi.SE2.SC.utils.UtilityFunctions.parsingChecker;


@WebServlet("/RejectInterviewDate")
@MultipartConfig
public class RejectInterviewDate extends HttpServlet {
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
    public RejectInterviewDate() {
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
        String studentHasInternshipIdString = request.getParameter("studentHasInternshipId");
        String interviewScheduleIdString = request.getParameter("interviewScheduleId");

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        int interviewScheduleId;
        int studentHasInternshipId;
        boolean isStudent = user instanceof Student;

        // Checking if the given student-internship association id is valid
        if(studentHasInternshipIdString == null || studentHasInternshipIdString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("The id for the student-internship is null!");
            return;
        }

        // Checking if the given id for the interview schedule is valid
        if(interviewScheduleIdString == null || interviewScheduleIdString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("The id for the interview schedule is null!");
            return;
        }

        // Parsing the interviewSchedule ID string back into an integer
        Optional<Integer> parsedId = parsingChecker(request, response, interviewScheduleIdString);
        if (parsedId.isPresent())			interviewScheduleId = parsedId.get();
        else	return;

        // Parsing the studentHasInternship ID string back into an integer
        parsedId = parsingChecker(request, response, studentHasInternshipIdString);
        if (parsedId.isPresent())			studentHasInternshipId = parsedId.get();
        else	return;

        // Checking if the given interview schedule exists
        InterviewScheduleDAO interviewScheduleDAO = createInterviewScheduleDAO();
        InterviewSchedule interviewSchedule = null;
        try {
            interviewSchedule = interviewScheduleDAO.getInterviewSchedule(interviewScheduleId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        if (interviewSchedule == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("No interview schedule found for the given id");
            return;
        }

        // Checking that the given student-internship association matches the id found in the retrieved interview schedule
        if (interviewSchedule.getStudentHasInternshipId() != studentHasInternshipId) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("The given studentHasInternship id does not match the one found for the interviewSchedule");
            return;
        }

        // Retrieving the student-internship association
        StudentHasInternshipDAO studentHasInternshipDAO = createStudentHasInternshipDAO();
        StudentHasInternship studentHasInternship = null;
        try {
            studentHasInternship = studentHasInternshipDAO.getStudentHasInternship(studentHasInternshipId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }
        if (studentHasInternship == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("The given studentHasInternship id does not match any ongoing internship");
            return;
        }

        // Checking that the user in the current session is a student
        if (!isStudent) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("Only the student can reject a date proposed by the company");
            return;
        }

        // Checking that the student in the current session is actually the one associated to the studentHasInternship
        if (studentHasInternship.getStudentId() != user.getId()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("The operation was block because you are not the associated to the given internship");
            return;
        }

        if (!studentHasInternship.getApplicationStatus().equals("selectionProcess")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("This is not the correct time to evaluate an interview");
            return;
        }

        // Rejecting the interview date
        try {
            interviewScheduleDAO.rejectInterviewDate(interviewScheduleId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
    }
}