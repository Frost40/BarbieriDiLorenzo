package polimi.SE2.SC.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import polimi.SE2.SC.DAOs.CvDAO;
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.QuestionnaireDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.beans.*;
import polimi.SE2.SC.utils.ConnectionHandler;

import static polimi.SE2.SC.utils.UtilityFunctions.calculateAverageRating;
import static polimi.SE2.SC.utils.UtilityFunctions.parsingChecker;

@WebServlet("/UploadQuestionnaire")
@MultipartConfig
public class UploadQuestionnaire extends HttpServlet{
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public UploadQuestionnaire() {
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
        Part filePart = request.getPart("questionnaire");

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        int studentHasInternshipId;
        boolean isStudent = user instanceof Student;

        // Checking if the id of the given ongoing internship (studentHasInternship) is valid
        if(studentHasInternshipIdString == null || studentHasInternshipIdString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("No valid id for the ongoing internship was associated to the request!");
            return;
        }

        if (filePart == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("No questionnaire file has been uploaded!");
            return;
        }

        // Parsing process for receiver id
        Optional<Integer> parsedId = parsingChecker(request, response, studentHasInternshipIdString);
        if (parsedId.isPresent())		studentHasInternshipId = parsedId.get();
        else		return;

        // Making sure the association student-company exists
        StudentHasInternshipDAO studentHasInternshipDAO = new StudentHasInternshipDAO(connection);
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
            response.getWriter().println("There is no ongoing internship between student and company with the given id");
            return;
        }

        // Checking that the user in the current session is not a student
        if (isStudent) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("Only a company account can upload a questionnaire");
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

        // Checking if the given ongoing internship is in the correct time period to upload a questionnaire
        if (!studentHasInternship.getApplicationStatus().equals("selectionProcess")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("You are not authorized to upload a questionnaire in the current time period");
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

        QuestionnaireDAO questionnaireDAO = new QuestionnaireDAO(connection);
        try (InputStream fileContent = filePart.getInputStream()) {
            // Save the questionnaire file as a binary object directly in the database
            questionnaireDAO.uploadQuestionnaire(fileContent, fileType, studentHasInternshipId);
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
