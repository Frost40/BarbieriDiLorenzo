package polimi.SE2.SC.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.MessageDAO;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Message;
import polimi.SE2.SC.beans.Student;
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
import java.util.*;

import static polimi.SE2.SC.utils.UtilityFunctions.parsingChecker;

@WebServlet("/OpenChatWindow")
@MultipartConfig
public class OpenChatWindow extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public OpenChatWindow() {
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

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String otherAccountIdString = request.getParameter("otherAccountId"); // The one the user wants to chat with
        String myAccountIdString; // Variable expected only if the user is a company

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        int otherAccountId;
        int myAccountId = 0;
        boolean isStudent = user instanceof Student;

        // Checking if the given otherAccountIdString is valid
        if(otherAccountIdString == null || otherAccountIdString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("The id for the account you are trying to chat with is null!");
            return;
        }

        // Parsing the otherAccount ID string back into an integer
        Optional<Integer> parsedId = parsingChecker(request, response, otherAccountIdString);
        if (parsedId.isPresent())			otherAccountId = parsedId.get();
        else	return;

        if (!isStudent) {
            myAccountIdString = request.getParameter("internshipId");

            // Checking if the given myAccountIdString is valid
            if (myAccountIdString == null || myAccountIdString.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().println("The id for the internship used to chat with the student is null!");
                return;
            }

            // Parsing the myAccount ID string back into an integer
            parsedId = parsingChecker(request, response, myAccountIdString);
            if (parsedId.isPresent()) myAccountId = parsedId.get();
            else return;

            // Checking that the current company user posses the internship he is trying to use
            InternshipDAO internshipDAO = new InternshipDAO(connection);
            ArrayList<Internship> companyInternships;
            try {
                companyInternships = internshipDAO.getCompanyInternships(user.getId());
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println(e.getMessage());
                return;
            }

            List<Integer> companyInternshipsIds = companyInternships.stream().map(Internship::getId).toList();
            if (!companyInternshipsIds.contains(myAccountId)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().println("You do not posses the internship used to chat with the student");
                return;
            }
        }

        MessageDAO messageDAO = new MessageDAO(connection);
        ArrayList<Message> chat = null;
        try {
            if (isStudent) {
                chat = messageDAO.getAllChatMessages(user.getId(), otherAccountId);
            } else {
                chat = messageDAO.getAllChatMessages(myAccountId, otherAccountId);
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // An empty chat is not possible in the Messages Page
        // The student should have first started a conversation via DisplaySingleChat in the Search Page
        if (chat == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("It is impossible to start a conversation from this page");
            return;
        }

        // JSON serialization
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Gson gson = new GsonBuilder().create();

        Map<String, Object> jsonObject = new HashMap<>();
        jsonObject.put("chat", chat);

        String json = gson.toJson(jsonObject);
        response.getWriter().write(json);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}

}
