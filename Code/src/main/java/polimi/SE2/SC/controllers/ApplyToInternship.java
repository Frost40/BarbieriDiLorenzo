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
import java.util.Optional;

import static polimi.SE2.SC.utils.UtilityFunctions.parsingChecker;

@WebServlet("/ApplyToInternship")
@MultipartConfig
public class ApplyToInternship extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public ApplyToInternship() {
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
        String internshipIdString = request.getParameter("internshipId");

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        int internshipId;
        boolean isStudent = user instanceof Student;

        // Checking if the given id is valid
        if(internshipIdString == null || internshipIdString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("The id for the internship is null!");
            return;
        }

        // Parsing the internship ID string back into an integer
        Optional<Integer> parsedId = parsingChecker(request, response, internshipIdString);
        if (parsedId.isPresent())			internshipId = parsedId.get();
        else	return;

        InternshipDAO internshipDAO = new InternshipDAO(connection);
        Internship internship = null;
        try {
            internship = internshipDAO.getInternshipById(internshipId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Checking if the given internship exists
        if (internship == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("No internship found for the given id");
            return;
        }

        // Checking that the user in the current session is a student
        if (!isStudent) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("Only students can apply to an internship");
            return;
        }

        // Saving the application in the database
        StudentHasInternshipDAO studentHasInternshipDAO = new StudentHasInternshipDAO(connection);
        try {
            studentHasInternshipDAO.addApplication(internshipId, user.getId());
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
    }
}