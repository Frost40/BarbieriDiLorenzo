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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import polimi.SE2.SC.DAOs.CvDAO;
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.MessageDAO;
import polimi.SE2.SC.beans.*;
import polimi.SE2.SC.utils.ConnectionHandler;

@WebServlet("/MessagesManager")
@MultipartConfig
public class MessagesManager extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;
    protected MessageDAO createMessageDAO() {
        return new MessageDAO(connection);
    }
    public MessagesManager() {
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
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        boolean isStudent = user instanceof Student;
        ArrayList<Internship> allChatsForStudent = null;
        HashMap<Internship, ArrayList<Student>> allChatsForCompany = null;

        MessageDAO messageDAO = createMessageDAO();
        try {
            if (isStudent) {
                allChatsForStudent = messageDAO.getAllChatsForStudent(user.getId());
            } else {
                allChatsForCompany = messageDAO.getAllChatsForCompany(user.getId());
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // JSON serialization
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .create();

        Map<String, Object> jsonObject = new HashMap<>();
        if (isStudent) {
            jsonObject.put("allChats", allChatsForStudent);
        } else {
            jsonObject.put("allChats", allChatsForCompany);
        }

        String json = gson.toJson(jsonObject);
        response.getWriter().write(json);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}
}
