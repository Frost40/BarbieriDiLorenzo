package polimi.SE2.SC.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;
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
import polimi.SE2.SC.beans.Complaint;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.User;
import polimi.SE2.SC.utils.ConnectionHandler;

@WebServlet("/CloseComplaint")
@MultipartConfig
public class CloseComplaint extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public CloseComplaint() {
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
        String complaintIdString = request.getParameter("id");
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        boolean isStudent = user instanceof Student;
        int complaintId;

        if(complaintIdString == null || complaintIdString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Null complaint id!");
            return;
        }

        // Parsing the complaint ID string back into an integer
        Optional<Integer> parsedId = parsingChecker(request, response, complaintIdString);
        if (parsedId.isPresent())			complaintId = parsedId.get();
        else	return;

        ComplaintDAO complaintDAO = new ComplaintDAO(connection);

        // Checking whether the given id refers to an existing complaint
        Complaint complaint = null;
        try {
            complaint = complaintDAO.getComplaint(complaintId);

        // If an error occurred during the process the user is redirected to errorPage
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        if (complaint == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("The complaint you are trying to close does not exist");
            return;
        }

        ArrayList<Complaint> userComplaints = null;
        try {
            userComplaints = complaintDAO.getUserComplaints(user.getId(), isStudent);

        // If an error occurred during the process the user is redirected to errorPage
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Checking if the user is authorized to perform the modifications
        if (!userComplaints.contains(complaint)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("You are not authorized to modify this complaint");
            return;
        }


        try {
            complaintDAO.requestClosure(complaintId, isStudent);

        // If an error occurred during the process the user is redirected to errorPage
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
    }

    private Optional<Integer> parsingChecker(HttpServletRequest request, HttpServletResponse response, String stringToParse) throws ServletException, IOException {
        int idToReturn;

        try {
            idToReturn = Integer.parseInt(stringToParse);

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return Optional.empty();
        }

        return Optional.of(idToReturn);
    }
}
