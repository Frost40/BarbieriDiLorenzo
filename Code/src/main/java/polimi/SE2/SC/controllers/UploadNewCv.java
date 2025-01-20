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
            response.getWriter().println("Invalid experiences set (a valid one has more than one character and less than 255)!");
            return;
        }

        if (programmingSkills == null || programmingSkills.length() <= 0 || programmingSkills.length() > 255) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid set of programming skills (a valid one has more than one character and less than 255)!");
            return;
        }

        if (otherSkills == null || otherSkills.length() <= 0 || otherSkills.length() > 255) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid set of generic skills (a valid one has more than one character and less than 255)!");
            return;
        }

        if (user instanceof Company) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("No cv is associated to a company");
            return;
        }

        CvDAO cvDAO = new CvDAO(connection);
        Cv cv = null;
        try {
            cv = cvDAO.getStudentCv(user.getId());
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Checking if the student already has a CV saved
        if (cv != null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("You already have a CV saved");
            return;
        }

        // Starting the process to save the file in server storage
        String contentType = filePart.getContentType();
        String fileExtension = "";

        // Checking the file format
        switch (contentType) {
            case "application/pdf" -> fileExtension = ".pdf";
            case "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                    fileExtension = ".docx"; // Handle both .doc and .docx
            case "text/plain" -> fileExtension = ".txt";
            default -> {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().println("File's format not permitted!");
                return;
            }
        }

        // Ensure saving path is a folder
        if (!cvStorage.endsWith(File.separator)) {
            cvStorage += File.separator;
        }

        String fileName = user.getId() + fileExtension;

        //InputStream fileContent = filePart.getInputStream();
        String outputFilePath = cvStorage + fileName;
        File file = new File(outputFilePath);

        try (InputStream fileContent = filePart.getInputStream()) {
            // Use REPLACE_EXISTING to overwrite the file if it already exists
            Files.copy(fileContent, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File saved correctly!");

        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Saving updated info in the DB
        String path = "http://localhost:8080/cvStorage/" + fileName;
        try {
            cvDAO.uploadNewCv(path, experiences, programmingSkills, otherSkills, user.getId());

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
    }

}
