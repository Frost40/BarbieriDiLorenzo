package polimi.SE2.SC.controllers;

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

@WebServlet("/ManageStudentApplication")
@MultipartConfig
public class ManageStudentApplication extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public ManageStudentApplication() {
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
        String evaluation = request.getParameter("evaluation");

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        boolean evaluationBoolean;
        int studentHasInternshipId;
        boolean isStudent = user instanceof Student;

        // Checking if the given student-internship association id is valid
        if(studentHasInternshipIdString == null || studentHasInternshipIdString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("The id for the student-internship is null!");
            return;
        }

        // Checking if the inserted evaluation is valid
        if (evaluation == null || (!evaluation.equalsIgnoreCase("true") && !evaluation.equalsIgnoreCase("false"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid evaluation parameter! Expected 'true' or 'false'");
            return;
        }
        evaluationBoolean = Boolean.parseBoolean(evaluation);

        // Parsing the studentHasInternship ID string back into an integer
        Optional<Integer> parsedId = parsingChecker(request, response, studentHasInternshipIdString);
        if (parsedId.isPresent())			studentHasInternshipId = parsedId.get();
        else	return;

        // Checking if the given student-internship association exists
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
            response.getWriter().println("There is no student-internship association relative to the given id");
            return;
        }

        // Checking that the user in the current session is a company
        if (isStudent) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("Only the company is authorized to manage a student application request");
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

        // Checking if the time period to manage an application request is correct
        String applicationStatus = studentHasInternship.getApplicationStatus();

        // If the status is not "evaluation," check further conditions
        if (!applicationStatus.equals("evaluation")) {
            if (applicationStatus.equals("selectionProcess")) {
                // If the status is "selectionProcess" and evaluationBoolean is true, reject the request
                if (evaluationBoolean) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().println("From the selection process, you can only start the internship period or reject an application.");
                    return;
                }
            } else {
                // If the status is neither "evaluation" nor "selectionProcess," reject the request
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().println("This is not the correct time to manage the application status.");
                return;
            }
        }

        // Uploading the company decision into the database
        try {
            studentHasInternshipDAO.updateApplicationStatus(studentHasInternshipId, evaluationBoolean);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
    }
}