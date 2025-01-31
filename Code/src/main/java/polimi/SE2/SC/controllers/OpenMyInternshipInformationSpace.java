package polimi.SE2.SC.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.*;

import static polimi.SE2.SC.utils.UtilityFunctions.parsingChecker;

@WebServlet("/OpenMyInternshipInformationSpace")
@MultipartConfig
public class OpenMyInternshipInformationSpace extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public OpenMyInternshipInformationSpace() {
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
        String studentHasInternshipIdString = request.getParameter("studentHasInternshipId"); // The one the user wants to chat with

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        int studentHasInternshipId;
        boolean isStudent = user instanceof Student;

        // Checking if the given otherAccountIdString is valid
        if(studentHasInternshipIdString == null || studentHasInternshipIdString.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Enable to retrieve data since the provided id is null");
            return;
        }

        // Parsing the student-internship association ID string back into an integer
        Optional<Integer> parsedId = parsingChecker(request, response, studentHasInternshipIdString);
        if (parsedId.isPresent())			studentHasInternshipId = parsedId.get();
        else	return;

        StudentHasInternshipDAO studentHasInternshipDAO = new StudentHasInternshipDAO(connection);
        StudentHasInternship studentHasInternship = null;
        try {
            studentHasInternship = studentHasInternshipDAO.getStudentHasInternship(studentHasInternshipId);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        if (studentHasInternship == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("There is no student-internship association for the given id");
            return;
        }

        if (isStudent) {
            // In case the user in session is a student he must be the one associated to the given studentHasInternship id
            if (studentHasInternship.getStudentId() != user.getId()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().println("You did not enrolled in the given internship program");
                return;
            }
        } else {
            // Checking that the current company user posses the internship in the given student-internship association
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
            if (!companyInternshipsIds.contains(studentHasInternship.getInternshipId())) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().println("You can only visualize data from your own internships");
                return;
            }
        }

        // Extracting all internship feedbacks
        FeedbackDAO feedbackDAO = new FeedbackDAO(connection);
        ArrayList<Feedback> feedbacks = null;
        try {
            feedbacks = feedbackDAO.getInternshipFeedbacks(studentHasInternship.getInternshipId());
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return;
        }

        // Starting the JSON serialization process
        Map<String, Object> jsonObject = new HashMap<>();

        // Depending on the status of student-internship status the data to send the client will be different
        switch (studentHasInternship.getApplicationStatus()) {
            case "evaluation" -> {
                // In case the user is the company the view will display student info to help evaluate the application
                if (!isStudent) {
                    // Retrieving student information
                    StudentDAO studentDAO = new StudentDAO(connection);
                    Student student = null;
                    try {
                        student = studentDAO.getStudentById(studentHasInternship.getStudentId());
                    } catch (SQLException e) {
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        response.getWriter().println(e.getMessage());
                        return;
                    }

                    // Retrieving student cv
                    CvDAO cvDAO = new CvDAO(connection);
                    Cv cv = null;
                    try {
                        cv = cvDAO.getStudentCv(studentHasInternship.getStudentId());
                    } catch (SQLException e) {
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        response.getWriter().println(e.getMessage());
                        return;
                    }

                    // Adding data to the Json Object
                    jsonObject.put("student", student);
                    jsonObject.put("cv", cv);
                    break;
                }
            }
            case "selectionProcess" -> {
                // Retrieving all questionnaires
                QuestionnaireDAO questionnaireDAO = new QuestionnaireDAO(connection);
                ArrayList<Questionnaire> questionnaires = null;
                try {
                    questionnaires = questionnaireDAO.getOngoingInternshipQuestionnaires(studentHasInternshipId);
                } catch (SQLException e) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().println(e.getMessage());
                    return;
                }

                // Retrieving all interview schedule
                InterviewScheduleDAO interviewScheduleDAO = new InterviewScheduleDAO(connection);
                ArrayList<InterviewSchedule> interviewSchedules = null;
                try {
                    interviewSchedules = interviewScheduleDAO.getOngoingInternshipInterviewSchedules(studentHasInternshipId);
                } catch (SQLException e) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().println(e.getMessage());
                    return;
                }

                // Adding data to the Json Object
                jsonObject.put("questionnaires", questionnaires);
                jsonObject.put("interviewSchedules", interviewSchedules);
                break;
            }
            case "ongoing" -> {
                // Retrieving all complaints
                ComplaintDAO complaintDAO = new ComplaintDAO(connection);
                ArrayList<Complaint> complaints = null;
                try {
                    complaints = complaintDAO.getOngoingInternshipOpenComplaints(studentHasInternshipId);
                } catch (SQLException e) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().println(e.getMessage());
                    return;
                }
                // Adding data to the Json Object
                jsonObject.put("complaints", complaints);
                break;
            }
            case "ended" -> {
                break;
            }
            case "rejected" -> {
                // Retrieving all questionnaires
                QuestionnaireDAO questionnaireDAO = new QuestionnaireDAO(connection);
                ArrayList<Questionnaire> questionnaires = null;
                try {
                    questionnaires = questionnaireDAO.getOngoingInternshipQuestionnaires(studentHasInternshipId);
                } catch (SQLException e) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().println(e.getMessage());
                    return;
                }

                // Retrieving all interview schedule
                InterviewScheduleDAO interviewScheduleDAO = new InterviewScheduleDAO(connection);
                ArrayList<InterviewSchedule> interviewSchedules = null;
                try {
                    interviewSchedules = interviewScheduleDAO.getOngoingInternshipInterviewSchedules(studentHasInternshipId);
                } catch (SQLException e) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().println(e.getMessage());
                    return;
                }

                // Adding data to the Json Object
                jsonObject.put("questionnaires", questionnaires);
                jsonObject.put("interviewSchedules", interviewSchedules);
                break;
            }
        }

        // Adding data to the Json Object
        jsonObject.put("feedbacks", feedbacks);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Gson gson = new GsonBuilder().create();

        String json = gson.toJson(jsonObject);
        response.getWriter().write(json);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}
}
