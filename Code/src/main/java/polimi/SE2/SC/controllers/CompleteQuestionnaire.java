package polimi.SE2.SC.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import polimi.SE2.SC.DAOs.InternshipDAO;
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

@WebServlet("/CompleteQuestionnaire")
@MultipartConfig
public class CompleteQuestionnaire extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public CompleteQuestionnaire() {
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
        Part filePart = request.getPart("questionnaire");

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

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

        // Checking that the user in the current session is a student
        if (!isStudent) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("Only students con complete questionnaires");
            return;
        }

        // Checking that the student in the current session is actually associated to the given internship
        if (studentHasInternship.getStudentId() != user.getId()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("The operation was block because you are not a student associated to the given internship");
            return;
        }

        // Check that the time period for completing the questionnaire is correct
        if (!studentHasInternship.getApplicationStatus().equals("selectionProcess")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("This is not the correct time to complete a questionnaire");
            return;
        }

        // Checking if the questionnaire was already completed
        if (questionnaire.isCompleted()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("The operation was blocked since the questionnaire was already completed");
            return;
        }

        // Validate and retrieve the file input
        String contentType = filePart.getContentType();
        String fileType = null;
        switch (contentType) {
            case "application/pdf" -> {
                fileType = "pdf";
            }
            case "application/msword" -> {
                fileType = "doc";
            }
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> {
                fileType = "docx";
            }
            case "text/plain" -> {
                fileType = "txt";
            }
            default -> {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().println("File format not permitted!");
                return;
            }
        }

        // Saving the completed questionnaire into the database
        try (InputStream fileContent = filePart.getInputStream()) {
            // Save the questionnaire file as a binary object directly in the database
            questionnaireDAO.completeQuestionnaire(questionnaireId, fileContent, fileType);
        } catch (IOException | SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Retrieving all questionnaires for the given internship
        ArrayList<Questionnaire> questionnaires = null;
        try {
            questionnaires = questionnaireDAO.getOngoingInternshipQuestionnaires(studentHasInternshipId);
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
        Gson gson = new GsonBuilder()
                .serializeNulls() // Ensure nulls are serialized
                .create();

        jsonObject.put("questionnaires", questionnaires);

        String json = gson.toJson(jsonObject);
        response.getWriter().write(json);
    }
}