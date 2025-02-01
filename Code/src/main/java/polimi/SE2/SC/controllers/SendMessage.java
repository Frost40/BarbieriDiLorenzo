package polimi.SE2.SC.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
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
import polimi.SE2.SC.DAOs.*;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Message;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.User;
import polimi.SE2.SC.utils.ConnectionHandler;

import static polimi.SE2.SC.utils.UtilityFunctions.parsingChecker;

@WebServlet("/SendMessage")
@MultipartConfig
public class SendMessage extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;
    protected MessageDAO createMessageDAO() {
        return new MessageDAO(connection);
    }
    protected InternshipDAO createInternshipDAO() {
        return new InternshipDAO(connection);
    }
    protected StudentDAO createStudentDAO() {
        return new StudentDAO(connection);
    }
    public SendMessage() {
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
        String message = request.getParameter("message");
        String receiverIdString = request.getParameter("receiverId");
        String senderIdString = null;

        HttpSession session = request.getSession();
        User sender    = (User)session.getAttribute("currentUser");

        User studentReceiver = null;
        Internship internshipReceiver = null;
        ArrayList<Message> chat = null;
        boolean isStudent = sender instanceof Student;
        int receiverId;
        int senderId = 0;

        // Checking message validity
        if (message == null || message.length() > 255 ||  message.length() <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid message (a valid message has more than one character and less than 255)!");
            return;
        }

        // Checking if the given receiver id is valid
        if(receiverIdString == null || receiverIdString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Null receiver id!");
            return;
        }

        // Parsing process for receiver id
        Optional<Integer> parsedId = parsingChecker(request, response, receiverIdString);
        if (parsedId.isPresent())		receiverId = parsedId.get();
        else		return;

        // Accessing the database to retrieve info regarding the receiver user type
        MessageDAO messageDAO = createMessageDAO();
        InternshipDAO internshipDAO = createInternshipDAO();
        if (isStudent) {
            // In case the sender is a student the receiver must be a valid internship
            try {
                internshipReceiver = internshipDAO.getInternshipById(receiverId);
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println(e.getMessage());
                return;
            }

            if (internshipReceiver == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().println("Either the account you are interacting with does not exists or you are not authorized to chat with it");
                return;
            }
        } else {
            // In case the sender is an internship the receiver must be a valid student
            senderIdString = request.getParameter("senderId");

            // Checking if the given sender id is valid
            if(senderIdString == null || senderIdString.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().println("Null receiver id!");
                return;
            }

            // Parsing process for sender id
            parsedId = parsingChecker(request, response, senderIdString);
            if (parsedId.isPresent())		senderId = parsedId.get();
            else		return;

            // Extracting the internship used as a sender by the company]
            Internship internshipSender = null;
            try {
                internshipSender = internshipDAO.getInternshipById(senderId);
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println(e.getMessage());
                return;
            }

            // Making sure the internship who sent the message exists
            if (internshipSender == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().println("The internship you are using to chat does not exists");
                return;
            }

            ArrayList<Internship> companyInternships = null;
            try {
                companyInternships = internshipDAO.getCompanyInternships(sender.getId());
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println(e.getMessage());
                return;
            }

            List<Integer> companyInternshipsIds = companyInternships.stream().map(Internship::getId).toList();
            if (!companyInternshipsIds.contains(senderId)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().println("The operation was block because you are not the company responsible for the given internship");
                return;
            }

            // Extracting the student receiver
            StudentDAO studentDAO = createStudentDAO();
            try {
                studentReceiver = studentDAO.getStudentById(receiverId);
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println(e.getMessage());
                return;
            }

            // Checking that an account for the given user exists
            if (studentReceiver == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().println("Either the account you are interacting with does not exists or you are not authorized to chat with it");
                return;
            }

            // The internship is not authorized to start a conversation with a student if there is no message history between them
            try {
                chat = messageDAO.getAllChatMessages(studentReceiver.getId(), sender.getId());
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println(e.getMessage());
                return;
            }

            // An empty chat means that the first message would be the one sent by the internship
            if (chat == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().println("Only students are authorized to initiate a message conversation");
                return;
            }

        }

        // Adding the message to the database and setting all previous chat messages to read
        if (isStudent) {
            // If a student is the current user: student = sender and internship = receiver
            try {
                messageDAO.insertMessage(message, false, sender.getId(), internshipReceiver.getId());
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println(e.getMessage());
                return;
            }

            try {
                messageDAO.setChatToRead(sender.getId(), internshipReceiver.getId(), true);
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println(e.getMessage());
                return;
            }
        } else {
            // If a company is the current user: internship = sender and student = receiver
            try {
                messageDAO.insertMessage(message, true, senderId, studentReceiver.getId());
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println(e.getMessage());
                return;
            }

            try {
                messageDAO.setChatToRead(studentReceiver.getId(), senderId, false);
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println(e.getMessage());
                return;
            }
        }

        // Getting the newly updated chat
        try {
            if (isStudent) {
                chat = messageDAO.getAllChatMessages(sender.getId(), internshipReceiver.getId());
            } else {
                chat = messageDAO.getAllChatMessages(studentReceiver.getId(), sender.getId());
            }

            //If an error occurred during the process the user is redirected to errorPage
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

        jsonObject.put("chat", chat);

        String json = gson.toJson(jsonObject);
        response.getWriter().write(json);
    }

}
