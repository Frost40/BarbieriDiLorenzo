package polimi.SE2.SC.controllers;

import polimi.SE2.SC.DAOs.*;
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

@WebServlet("/ReadMessage")
@MultipartConfig
public class ReadMessage extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;
    protected MessageDAO createMessageDAO() {
        return new MessageDAO(connection);
    }
    protected InternshipDAO createInternshipDAO() {
        return new InternshipDAO(connection);
    }
    public ReadMessage() {
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
        String messageIdString = request.getParameter("messageId");

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        int messageId;
        boolean isStudent = user instanceof Student;

        // Checking if the given id is valid
        if(messageIdString == null || messageIdString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("The id for the complaint is null!");
            return;
        }

        // Parsing the message ID string back into an integer
        Optional<Integer> parsedId = parsingChecker(request, response, messageIdString);
        if (parsedId.isPresent())			messageId = parsedId.get();
        else	return;

        // Retrieving the message from the database
        MessageDAO messageDAO = createMessageDAO();
        Message message = null;
        try {
            message = messageDAO.getMessage(messageId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Checking that the message exists in the DB
        if (message == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("The message you are trying to close does not exists");
            return;
        }

        if (isStudent) {
            // Since the servlet will be called once the message has been read the receiver of the message should be the one reading it
            if (message.getReceiverId() != user.getId()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().println("You are not the receiver");
                return;
            }
        } else {
            // Checking that the company in the current session is actually the owner of the internship associated to the receiver
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
            if (!companyInternshipsIds.contains(message.getReceiverId())) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().println("The operation was block because you are not the company responsible for the internship supposed to receive the message");
                return;
            }
        }

        // Setting the message to read
        try {
            messageDAO.hasBeenRead(messageId, isStudent);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Servlet ended successfully
        response.setStatus(HttpServletResponse.SC_OK);
    }
}