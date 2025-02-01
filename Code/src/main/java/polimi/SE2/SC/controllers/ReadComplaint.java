package polimi.SE2.SC.controllers;

import polimi.SE2.SC.DAOs.ComplaintDAO;
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

@WebServlet("/ReadComplaint")
@MultipartConfig
public class ReadComplaint extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;
    protected ComplaintDAO createComplaintDAO() {
        return new ComplaintDAO(connection);
    }
    protected InternshipDAO createInternshipDAO() {
        return new InternshipDAO(connection);
    }
    protected StudentHasInternshipDAO createStudentHasInternshipDAO() {
        return new StudentHasInternshipDAO(connection);
    }

    public ReadComplaint() {
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
        String complaintIdString = request.getParameter("complaintId");

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        int complaintId;
        boolean isStudent = user instanceof Student;

        // Checking if the given id is valid
        if(complaintIdString == null || complaintIdString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("The id for the complaint is null!");
            return;
        }

        // Parsing the complaint ID string back into an integer
        Optional<Integer> parsedId = parsingChecker(request, response, complaintIdString);
        if (parsedId.isPresent())			complaintId = parsedId.get();
        else	return;

        // Retrieving the complaint from the database
        ComplaintDAO complaintDAO = createComplaintDAO();
        Complaint complaint = null;
        try {
            complaint = complaintDAO.getComplaint(complaintId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Checking that the complaint exists in the DB
        if (complaint == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("The complaint you are trying to close does not exists");
            return;
        }

        // Retrieving the studentHasInternship associated with the complaint
        StudentHasInternshipDAO studentHasInternshipDAO = createStudentHasInternshipDAO();
        StudentHasInternship studentHasInternship = null;
        try {
            studentHasInternship = studentHasInternshipDAO.getStudentHasInternship(complaint.getStudentHasInternshipId());
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        if (isStudent) {
            // Checking if the student in the current session is the one associated with the internship experience the complaint is about
            if (studentHasInternship.getStudentId() != user.getId()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().println("You are not enrolled for the given internship");
                return;
            }

            // Avoiding rejection if the student previously proposed the closure
            if (complaint.isClosedByStudent() && !complaint.isClosedByCompany()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().println("Since you are the one who proposed the closure you are not authorize to reject it at this point");
                return;
            }
        } else {
            // Checking that the company in the current session is actually the owner of the internship associated to the complaint
            InternshipDAO internshipDAO = createInternshipDAO();
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
        }

        // Setting the complaint to read
        try {
            complaintDAO.hasBeenRead(complaintId, isStudent);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Servlet ended successfully
        response.setStatus(HttpServletResponse.SC_OK);
    }
}