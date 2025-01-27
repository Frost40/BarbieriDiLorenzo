package polimi.SE2.SC.controllers;

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

@WebServlet("/EvaluateQuestionnaire")
@MultipartConfig
public class EvaluateQuestionnaire extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public EvaluateQuestionnaire() {
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
        String questionnaireIdString = request.getParameter("questionnaireId");
        String evaluation = request.getParameter("evaluation");

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        boolean evaluationBoolean;
        int questionnaireId;
        int studentHasInternshipId;
        boolean isStudent = user instanceof Student;

        // Checking if the given student-internship association id is valid
        if(studentHasInternshipIdString == null || studentHasInternshipIdString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("The id for the student-internship is null!");
            return;
        }

        // Checking if the given id for the questionnaire is valid
        if(questionnaireIdString == null || questionnaireIdString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("The id for the questionnaire is null!");
            return;
        }

        // Checking if the inserted evaluation is valid
        if (evaluation == null || (!evaluation.equalsIgnoreCase("true") && !evaluation.equalsIgnoreCase("false"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid evaluation parameter! Expected 'true' or 'false'.");
            return;
        }
        evaluationBoolean = Boolean.parseBoolean(evaluation);

        // Parsing the interviewSchedule ID string back into an integer
        Optional<Integer> parsedId = parsingChecker(request, response, questionnaireIdString);
        if (parsedId.isPresent())			questionnaireId = parsedId.get();
        else	return;

        // Parsing the studentHasInternship ID string back into an integer
        parsedId = parsingChecker(request, response, studentHasInternshipIdString);
        if (parsedId.isPresent())			studentHasInternshipId = parsedId.get();
        else	return;

        // Checking if the given questionnaire exists
        QuestionnaireDAO questionnaireDAO = new QuestionnaireDAO(connection);
        Questionnaire questionnaire = null;
        try {
            questionnaire = questionnaireDAO.getQuestionnaire(questionnaireId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        if (questionnaire == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("No questionnaire found for the given id");
            return;
        }

        // Checking that the given student-internship association matches the id found in the retrieved interview schedule
        if (questionnaire.getStudentHasInternshipId() != studentHasInternshipId) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("The given studentHasInternship id does not match the one found for the questionnaire");
            return;
        }

        // Retrieving the student-internship association
        StudentHasInternshipDAO studentHasInternshipDAO = new StudentHasInternshipDAO(connection);
        StudentHasInternship studentHasInternship = null;
        try {
            studentHasInternship = studentHasInternshipDAO.getStudentHasInternship(studentHasInternshipId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Checking that the user in the current session is a company
        if (isStudent) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("Only the company is authorized to evaluate an interview");
            return;
        }

        // Checking that the company in the current session is actually the owner of the given internship
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

        if (!studentHasInternship.getApplicationStatus().equals("selectionProcess")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("This is not the correct time to evaluate an questionnaire");
            return;
        }

        // Uploading the evaluation into the database
        try {
            questionnaireDAO.setCompanyDecision(questionnaireId, evaluationBoolean);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
    }
}