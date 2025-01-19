package polimi.SE2.SC.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.MessageDAO;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Message;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.User;
import polimi.SE2.SC.utils.ConnectionHandler;

import static polimi.SE2.SC.utils.UtilityFunctions.parsingChecker;

@WebServlet("/DisplaySingleChat")
@MultipartConfig
public class DisplaySingleChat extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public DisplaySingleChat() {
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
        String internshipIdString = request.getParameter("internshipId");
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        boolean isStudent = user instanceof Student;
        int internshipId;

        if(internshipIdString == null || internshipIdString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Null complaint id!");
            return;
        }

        // Parsing the complaint ID string back into an integer
        Optional<Integer> parsedId = parsingChecker(request, response, internshipIdString);
        if (parsedId.isPresent())			internshipId = parsedId.get();
        else	return;

        // Checking for internship existence
        InternshipDAO internshipDAO = new InternshipDAO(connection);
        Internship internship = null;
        try {
            internship = internshipDAO.getInternshipById(internshipId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        if (internship == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("The internship you are trying to chat with does not exists");
            return;
        }

        // Retrieving all previous messages inside the chat
        MessageDAO messageDAO = new MessageDAO(connection);
        ArrayList<Message> chat = null;
        try {
            chat = messageDAO.getAllChatMessages(user.getId(), internshipId);
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
        jsonObject.put("chat", chat);

        String json = gson.toJson(jsonObject);
        response.getWriter().write(json);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}
}
