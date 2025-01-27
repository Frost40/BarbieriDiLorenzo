package polimi.SE2.SC.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToDoubleBiFunction;
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
import polimi.SE2.SC.DAOs.MessageDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.StudentHasInternship;
import polimi.SE2.SC.beans.User;
import polimi.SE2.SC.utils.ConnectionHandler;

@WebServlet("/MyInternshipsManager")
@MultipartConfig
public class MyInternshipsManager extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public MyInternshipsManager() {
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
        HashMap<Internship, StudentHasInternship> allStudentHasInternshipForStudent = null;
        HashMap<Internship, HashMap<Student, StudentHasInternship>> allStudentHasInternshipForCompany = null;

        StudentHasInternshipDAO studentHasInternshipDAO = new StudentHasInternshipDAO(connection);
        try {
            if (isStudent) {
                allStudentHasInternshipForStudent = studentHasInternshipDAO.getAllStudentInternships(user.getId());
            } else {
                allStudentHasInternshipForCompany = studentHasInternshipDAO.getCompanyOngoingInternships(user.getId());
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Prepare JSON response
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        Map<String, Object> jsonObject = new HashMap<>();
        if (isStudent) {
            // Flatten map for JSON serialization
            ArrayList<Map<String, Object>> studentData = new ArrayList<>();
            for (Map.Entry<Internship, StudentHasInternship> entry : allStudentHasInternshipForStudent.entrySet()) {
                Map<String, Object> studentEntry = new HashMap<>();
                studentEntry.put("internship", entry.getKey()); // Gson will serialize the Internship object
                studentEntry.put("studentHasInternship", entry.getValue()); // Gson will serialize the StudentHasInternship object
                studentData.add(studentEntry);
            }
            jsonObject.put("data", studentData);
        } else {
            // Flatten map for JSON serialization
            ArrayList<Map<String, Object>> companyData = new ArrayList<>();
            for (Map.Entry<Internship, HashMap<Student, StudentHasInternship>> entry : allStudentHasInternshipForCompany.entrySet()) {
                Map<String, Object> companyEntry = new HashMap<>();
                companyEntry.put("internship", entry.getKey()); // Serialize the Internship object
                ArrayList<Map<String, Object>> studentList = new ArrayList<>();
                for (Map.Entry<Student, StudentHasInternship> innerEntry : entry.getValue().entrySet()) {
                    Map<String, Object> studentEntry = new HashMap<>();
                    studentEntry.put("student", innerEntry.getKey()); // Serialize the Student object
                    studentEntry.put("studentHasInternship", innerEntry.getValue()); // Serialize the StudentHasInternship object
                    studentList.add(studentEntry);
                }
                companyEntry.put("students", studentList);
                companyData.add(companyEntry);
            }
            jsonObject.put("data", companyData);
        }

        String json = gson.toJson(jsonObject);
        response.getWriter().write(json);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}
}
