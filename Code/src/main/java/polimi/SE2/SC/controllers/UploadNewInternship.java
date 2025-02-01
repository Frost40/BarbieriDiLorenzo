package polimi.SE2.SC.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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

@WebServlet("/UploadNewInternship")
@MultipartConfig
public class UploadNewInternship extends HttpServlet{
    private static final long serialVersionUID = 1L;
    private Connection connection;
    protected InternshipDAO createInternshipDAO() {
        return new InternshipDAO(connection);
    }
    public UploadNewInternship() {
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
        String name = request.getParameter("name");
        String description = request.getParameter("description");

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        // Checking name validity
        if (name == null || name.length() <= 0 || name.length() > 45) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid name (a valid name has more than one character and less than 45)!");
            return;
        }

        // Checking description validity
        if (description == null || description.length() <= 0 || description.length() > 500) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid description (a valid description has more than one character and less than 500)!");
            return;
        }

        // Blocking the upload in case the user requesting it is a student
        if (user instanceof Student) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("No internship can be upload by a student");
            return;
        }

        // Adding the new internship to the database
        InternshipDAO internshipDAO = createInternshipDAO();
        try {
            internshipDAO.uploadNewInternship(name, description, user.getId());
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Retrieving the updated list of internship for the company
        ArrayList<Internship> internships = null;
        try {
            internships = internshipDAO.getCompanyInternships(user.getId());
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // JSON serialization
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Gson gson = new GsonBuilder().create();

        Map<String, Object> jsonObject = new HashMap<>();
        jsonObject.put("internships", internships);

        String json = gson.toJson(jsonObject);
        response.getWriter().write(json);
    }

}
