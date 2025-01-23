package polimi.SE2.SC.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import polimi.SE2.SC.DAOs.ComplaintDAO;
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.beans.*;
import polimi.SE2.SC.utils.ConnectionHandler;

import static polimi.SE2.SC.utils.UtilityFunctions.parsingChecker;

@WebServlet("/UploadComplaint")
@MultipartConfig
public class UploadComplaint extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public UploadComplaint() {
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
        String complaint = request.getParameter("complaint");
        String ongoingInternshipIdString = request.getParameter("ongoingInternshipId");

        HttpSession session = request.getSession();
        User user    = (User)session.getAttribute("currentUser");

        int ongoingInternshipId;
        boolean isStudent = user instanceof Student;

        // Checking complaint validity
        if (complaint == null || complaint.length() > 255 ||  complaint.length() <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid complaint (a valid complaint has more than one character and less than 255)!");
            return;
        }

        // Checking if the id of the given ongoing internship (studentHasInternship) is valid
        if(ongoingInternshipIdString == null || ongoingInternshipIdString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("No valid id for the ongoing internship was associated to the request!");
            return;
        }

        // Parsing process for receiver id
        Optional<Integer> parsedId = parsingChecker(request, response, ongoingInternshipIdString);
        if (parsedId.isPresent())		ongoingInternshipId = parsedId.get();
        else		return;

        // Making sure the association student-company exists
        StudentHasInternshipDAO studentHasInternshipDAO = new StudentHasInternshipDAO(connection);
        StudentHasInternship ongoingInternship = null;
        try {
            ongoingInternship = studentHasInternshipDAO.getOngoingInternship(ongoingInternshipId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        if (ongoingInternship == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("There is no ongoing internship between student and company with the given id");
            return;
        }

        // Checking if the given ongoing internship is actually started
        if (ongoingInternship.getStartDate() == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("You are authorized to issue a complaint only after the internship period has started");
            return;
        }

        // Checking if the current user in session is actually associated to the given ongoing internship
        if (isStudent) {
            if (ongoingInternship.getStudentId() != user.getId()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().println("The operation was block because you are not the student associated to the given ongoing internship");
                return;
            }
        } else {
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
            if (!companyInternshipsIds.contains(ongoingInternship.getInternshipId())) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().println("The operation was block because you are not the company responsible for the given internship");
                return;
            }
        }

        // Creating the new complaint in the database
        ComplaintDAO complaintDAO = new ComplaintDAO(connection);
        try {
            complaintDAO.uploadComplaint(complaint, ongoingInternshipId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Retrieving all tye complaints that are still open for the given ongoing internship
        ArrayList<Complaint> complaints = null;
        try {
            complaints = complaintDAO.getOngoingInternshipOpenComplaints(ongoingInternshipId);
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
        Gson gson = new GsonBuilder().create();

        jsonObject.put("openComplaints", complaints);

        String json = gson.toJson(jsonObject);
        response.getWriter().write(json);
    }

}
