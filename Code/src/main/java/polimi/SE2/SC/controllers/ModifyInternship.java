package polimi.SE2.SC.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.User;
import polimi.SE2.SC.utils.ConnectionHandler;

import static polimi.SE2.SC.utils.UtilityFunctions.parsingChecker;

@WebServlet("/ModifyInternship")
@MultipartConfig
public class ModifyInternship extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public ModifyInternship() {
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
        String name = request.getParameter("name");
        String description = request.getParameter("description");

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        // Checking "name" validity
        if (name == null || name.length() <= 0 || name.length() > 45) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid name (a valid name has more than one character and less than 45)!");
            return;
        }

        // Checking "description" validity
        if (description == null || description.length() <= 0 || description.length() > 500) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid description (a valid description has more than one character and less than 500)!");
            return;
        }

        // Making sure the user requesting the modification is actually a company
        if (user instanceof Student) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("No internship can be modified by a student");
            return;
        }

        // Parsing the cv ID string back into an integer
        int internshipId;
        Optional<Integer> parsedId = parsingChecker(request, response, internshipIdString);
        if (parsedId.isPresent())			internshipId = parsedId.get();
        else	return;

        // Extracting from the DB the internship associated to the given id
        InternshipDAO internshipDAO = new InternshipDAO(connection);
        Internship internship = null;
        try {
            internship = internshipDAO.getInternshipById(internshipId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Unauthorized since the given cv ID does not exist
        if (internship == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("The internship you are trying to modify does not exists");
            return;
        }

        // Checking if the internship is actually associated with the company in the current session
        if (internship.getCompanyId() != user.getId()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("You can only modify your own internship");
            return;
        }

        // Updating the database
        try {
            internshipDAO.modifyInternship(internshipId, name, description);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
    }
}
