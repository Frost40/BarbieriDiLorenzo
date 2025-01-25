package polimi.SE2.SC.controllers;

import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.StudentHasInternship;
import polimi.SE2.SC.beans.User;
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

@WebServlet("/EndInternship")
@MultipartConfig
public class EndInternship extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public EndInternship() {
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

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        int studentHasInternshipId;
        boolean isStudent = user instanceof Student;

        // Checking if the given id is valid
        if(studentHasInternshipIdString == null || studentHasInternshipIdString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("The id for the student-internship is null!");
            return;
        }

        // Parsing the studentHasInternship ID string back into an integer
        Optional<Integer> parsedId = parsingChecker(request, response, studentHasInternshipIdString);
        if (parsedId.isPresent())			studentHasInternshipId = parsedId.get();
        else	return;

        StudentHasInternshipDAO studentHasInternshipDAO = new StudentHasInternshipDAO(connection);
        StudentHasInternship studentHasInternship = null;
        try {
            studentHasInternship = studentHasInternshipDAO.getStudentHasInternship(studentHasInternshipId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        if (isStudent) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("Only the company can decide to start the internship period");
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

        if (studentHasInternship.getApplicationStatus().equals("ongoing") && studentHasInternship.getStartDate() != null) {
            // Proceeding to end the internship period
            try {
                studentHasInternshipDAO.endInternshipExperience(studentHasInternshipId);
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println(e.getMessage());
                return;
            }
        } else {
            // The internship period has not started yet
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("Closure blocked: the internship is not started yet");
            return;
        }

        // Servlet ended successfully
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
