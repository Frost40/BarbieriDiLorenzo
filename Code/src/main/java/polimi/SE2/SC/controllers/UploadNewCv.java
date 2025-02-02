package polimi.SE2.SC.controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
import polimi.SE2.SC.DAOs.CompanyDAO;
import polimi.SE2.SC.DAOs.CvDAO;
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Cv;
import polimi.SE2.SC.beans.User;
import polimi.SE2.SC.utils.ConnectionHandler;

import static polimi.SE2.SC.utils.UtilityFunctions.parsingChecker;

@WebServlet("/UploadNewCv")
@MultipartConfig
public class UploadNewCv extends HttpServlet{
    private static final long serialVersionUID = 1L;
    private Connection connection;
    private String cvStorage;
    protected CvDAO createCvDAO() {
        return new CvDAO(connection);
    }
    public UploadNewCv() {
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
        cvStorage = getServletContext().getInitParameter("database");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String experiences = request.getParameter("experiences");
        String programmingSkills = request.getParameter("programmingSkills");
        String otherSkills = request.getParameter("otherSkills");
        Part filePart = request.getPart("cv");

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        if (experiences == null || experiences.length() <= 0 || experiences.length() > 255) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid experiences set!");
            return;
        }

        if (programmingSkills == null || programmingSkills.length() <= 0 || programmingSkills.length() > 255) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid set of programming skills!");
            return;
        }

        if (otherSkills == null || otherSkills.length() <= 0 || otherSkills.length() > 255) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid set of generic skills!");
            return;
        }

        if (user instanceof Company) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("No CV is associated to a company");
            return;
        }

        CvDAO cvDAO = createCvDAO();
        Cv cv = null;
        try {
            cv = cvDAO.getStudentCv(user.getId());
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Check if the student already has a CV saved
        if (cv != null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("You already have a CV saved");
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

        try (InputStream fileContent = filePart.getInputStream()) {
            // Save the CV file as a binary object directly in the database
            cvDAO.uploadNewCv(fileContent, fileType, experiences, programmingSkills, otherSkills, user.getId());
        } catch (IOException | SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
    }

}
