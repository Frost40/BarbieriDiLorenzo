package polimi.SE2.SC.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.http.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import polimi.SE2.SC.DAOs.*;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OpenMyInternshipInformationSpaceTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private Connection testConnection;

    @Mock private StudentHasInternshipDAO studentHasInternshipDAO;
    @Mock private MessageDAO messageDAO;
    @Mock private CvDAO cvDAO;
    @Mock private StudentDAO studentDAO;
    @Mock private CompanyDAO companyDAO;
    @Mock private FeedbackDAO feedbackDAO;
    @Mock private ComplaintDAO complaintDAO;
    @Mock private InternshipDAO internshipDAO;
    @Mock private InterviewScheduleDAO interviewScheduleDAO;
    @Mock private QuestionnaireDAO questionnaireDAO;

    private OpenMyInternshipInformationSpace openMyInternshipInfoSpaceServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Spy the servlet
        openMyInternshipInfoSpaceServlet = spy(new OpenMyInternshipInformationSpace());

        ServletConfig servletConfig = testUtils.setUp();
        openMyInternshipInfoSpaceServlet.init(servletConfig);

        // Inject the mocked connection
        Field connField = OpenMyInternshipInformationSpace.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connField.set(openMyInternshipInfoSpaceServlet, testConnection);

        // Override the DAO creation methods
        doReturn(studentHasInternshipDAO).when(openMyInternshipInfoSpaceServlet).createStudentHasInternshipDAO();
        doReturn(messageDAO).when(openMyInternshipInfoSpaceServlet).createMessageDAO();
        doReturn(cvDAO).when(openMyInternshipInfoSpaceServlet).createCvDAO();
        doReturn(studentDAO).when(openMyInternshipInfoSpaceServlet).createStudentDAO();
        doReturn(companyDAO).when(openMyInternshipInfoSpaceServlet).createCompanyDAO();
        doReturn(feedbackDAO).when(openMyInternshipInfoSpaceServlet).createFeedbackDAO();
        doReturn(complaintDAO).when(openMyInternshipInfoSpaceServlet).createComplaintDAO();
        doReturn(internshipDAO).when(openMyInternshipInfoSpaceServlet).createInternshipDAO();
        doReturn(interviewScheduleDAO).when(openMyInternshipInfoSpaceServlet).createInterviewScheduleDAO();
        doReturn(questionnaireDAO).when(openMyInternshipInfoSpaceServlet).createQuestionnaireDAO();

        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // By default session is mocked
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Missing studentHasInternshipId => 400
    // -------------------------------------------------------------------------
    @Test
    public void testMissingStudentHasInternshipId() throws Exception {
        // e.g. a Student
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn(null);

        openMyInternshipInfoSpaceServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("Enable to retrieve data since the provided id is null"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: StudentHasInternship not found => 401
    // -------------------------------------------------------------------------
    @Test
    public void testStudentHasInternshipNotFound() throws Exception {
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        // DB => null
        doReturn(null).when(studentHasInternshipDAO).getStudentHasInternship(555);

        openMyInternshipInfoSpaceServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("There is no student-internship association for the given id"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Student mismatch => 401
    // -------------------------------------------------------------------------
    @Test
    public void testStudentMismatch() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(999);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        // DB => #555 => studentId=123 => mismatch
        StudentHasInternship shi = new StudentHasInternship();
        shi.setStudentId(123);
        shi.setInternshipId(1000);
        shi.setApplicationStatus("evaluation");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        openMyInternshipInfoSpaceServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("You did not enrolled"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: Company mismatch => 401
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyMismatch() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(400);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        // DB => #555 => internshipId=999
        StudentHasInternship shi = new StudentHasInternship();
        shi.setStudentId(300);
        shi.setInternshipId(999);
        shi.setApplicationStatus("evaluation");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company #400 owns e.g. internship #101 => mismatch
        ArrayList<Internship> owned = new ArrayList<>();
        Internship i101 = new Internship();
        i101.setId(101);
        owned.add(i101);

        doReturn(owned).when(internshipDAO).getCompanyInternships(400);

        openMyInternshipInfoSpaceServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("You can only visualize data from your own internships"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: DB error retrieving StudentHasInternship => 500
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionStudentHasInternship() throws Exception {
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        doThrow(new SQLException("DB error getStudentHasInternship"))
                .when(studentHasInternshipDAO).getStudentHasInternship(555);

        openMyInternshipInfoSpaceServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error getStudentHasInternship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: DB error retrieving companyInternships => 500
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionCompanyInternships() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(400);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        // DB => #555 => internshipId=999
        StudentHasInternship shi = new StudentHasInternship();
        shi.setInternshipId(999);
        shi.setApplicationStatus("evaluation");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // getCompanyInternships => throw
        doThrow(new SQLException("DB error getCompanyInternships"))
                .when(internshipDAO).getCompanyInternships(400);

        openMyInternshipInfoSpaceServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error getCompanyInternships"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: DB error retrieving feedbacks => 500
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionFeedbacks() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        // DB => #555 => same student => internshipId=999 => applicationStatus=ended (for instance)
        StudentHasInternship shi = new StudentHasInternship();
        shi.setStudentId(50);
        shi.setInternshipId(999);
        shi.setApplicationStatus("ended");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // feedbackDAO => throw
        doThrow(new SQLException("DB error getInternshipFeedbacks"))
                .when(feedbackDAO).getInternshipFeedbacks(999);

        openMyInternshipInfoSpaceServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error getInternshipFeedbacks"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 8: applicationStatus = evaluation => user is company => retrieve student + cv
    // -------------------------------------------------------------------------
    @Test
    public void testEvaluationStatusCompany() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setStudentId(777);
        shi.setInternshipId(999);
        shi.setApplicationStatus("evaluation");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company #100 owns internship #999
        ArrayList<Internship> owned = new ArrayList<>();
        Internship i999 = new Internship();
        i999.setId(999);
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(100);

        // feedbackDAO => return some feedback
        ArrayList<Feedback> feedbacks = new ArrayList<>();
        doReturn(feedbacks).when(feedbackDAO).getInternshipFeedbacks(999);

        // studentDAO => get student #777
        Student st = new Student();
        st.setId(777);
        doReturn(st).when(studentDAO).getStudentById(777);

        // cvDAO => get student cv
        Cv cv = new Cv();
        cv.setId(10);
        doReturn(cv).when(cvDAO).getStudentCv(777);

        openMyInternshipInfoSpaceServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String out = responseWriter.toString();
        assertTrue(out.contains("\"student\""));
        assertTrue(out.contains("\"cv\""));
        assertTrue(out.contains("\"feedbacks\""));
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: applicationStatus = evaluation => user is student => no student/cv data
    // -------------------------------------------------------------------------
    @Test
    public void testEvaluationStatusStudent() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(777);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setStudentId(777);
        shi.setInternshipId(999);
        shi.setApplicationStatus("evaluation");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // feedback
        doReturn(new ArrayList<Feedback>()).when(feedbackDAO).getInternshipFeedbacks(999);

        openMyInternshipInfoSpaceServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String out = responseWriter.toString();
        // There's no "student" or "cv" keys in the JSON => but "feedbacks" is present
        assertTrue(out.contains("\"feedbacks\""));
        assertFalse(out.contains("\"student\""));
        assertFalse(out.contains("\"cv\""));
    }

    // -------------------------------------------------------------------------
    // ★ Test 10: applicationStatus = selectionProcess => gather questionnaires + interviewSchedules
    // -------------------------------------------------------------------------
    @Test
    public void testSelectionProcess() throws Exception {
        // Suppose user is the student
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(555);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("777");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setStudentId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(777);

        // feedback
        doReturn(new ArrayList<Feedback>()).when(feedbackDAO).getInternshipFeedbacks(999);

        // questionnaires
        ArrayList<Questionnaire> questionnaires = new ArrayList<>();
        doReturn(questionnaires).when(questionnaireDAO).getOngoingInternshipQuestionnaires(777);

        // interviewSchedules
        ArrayList<InterviewSchedule> schedules = new ArrayList<>();
        doReturn(schedules).when(interviewScheduleDAO).getOngoingInternshipInterviewSchedules(777);

        openMyInternshipInfoSpaceServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String out = responseWriter.toString();
        assertTrue(out.contains("\"questionnaires\""));
        assertTrue(out.contains("\"interviewSchedules\""));
        assertTrue(out.contains("\"feedbacks\""));
    }

    // -------------------------------------------------------------------------
    // ★ Test 11: applicationStatus = ongoing => gather open complaints
    // -------------------------------------------------------------------------
    @Test
    public void testOngoing() throws Exception {
        // Suppose user is the company
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("777");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setStudentId(123);
        shi.setInternshipId(999);
        shi.setApplicationStatus("ongoing");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(777);

        ArrayList<Internship> owned = new ArrayList<>();
        Internship i999 = new Internship();
        i999.setId(999);
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(300);

        // feedbacks
        doReturn(new ArrayList<Feedback>()).when(feedbackDAO).getInternshipFeedbacks(999);

        // complaints
        ArrayList<Complaint> complaints = new ArrayList<>();
        doReturn(complaints).when(complaintDAO).getOngoingInternshipOpenComplaints(777);

        openMyInternshipInfoSpaceServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String out = responseWriter.toString();
        assertTrue(out.contains("\"complaints\""));
        assertTrue(out.contains("\"feedbacks\""));
    }

    // -------------------------------------------------------------------------
    // ★ Test 12: applicationStatus = ended => only feedbacks
    // -------------------------------------------------------------------------
    @Test
    public void testEnded() throws Exception {
        // Suppose user is the student
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("777");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setStudentId(100);
        shi.setInternshipId(999);
        shi.setApplicationStatus("ended");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(777);

        doReturn(new ArrayList<Feedback>()).when(feedbackDAO).getInternshipFeedbacks(999);

        openMyInternshipInfoSpaceServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String out = responseWriter.toString();
        // No special data for ended, but "feedbacks" is always included
        assertTrue(out.contains("\"feedbacks\""));
    }

    // -------------------------------------------------------------------------
    // ★ Test 13: applicationStatus = rejected => gather failed Q + interviews
    // -------------------------------------------------------------------------
    @Test
    public void testRejected() throws Exception {
        // Suppose user is the student
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(500);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("999");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setStudentId(500);
        shi.setInternshipId(1234);
        shi.setApplicationStatus("rejected");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(999);

        // feedback
        doReturn(new ArrayList<Feedback>()).when(feedbackDAO).getInternshipFeedbacks(1234);

        // failed Q
        ArrayList<Questionnaire> failedQs = new ArrayList<>();
        doReturn(failedQs).when(questionnaireDAO).getFailedQuestionnaires(999);

        // failed interviews
        ArrayList<InterviewSchedule> failedInterviews = new ArrayList<>();
        doReturn(failedInterviews).when(interviewScheduleDAO).getFailedInterviews(999);

        openMyInternshipInfoSpaceServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String out = responseWriter.toString();
        assertTrue(out.contains("\"questionnaires\""));
        assertTrue(out.contains("\"interviewSchedules\""));
        assertTrue(out.contains("\"feedbacks\""));
    }

    // -------------------------------------------------------------------------
    // ★ Test 14: DB errors retrieving second-phase data (like questionnaires, etc.)
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionInPhase2() throws Exception {
        // Suppose user is student
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(500);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("777");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setStudentId(500);
        shi.setInternshipId(999);
        shi.setApplicationStatus("selectionProcess"); // triggers questionnaires + interviews
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(777);

        // feedback
        doReturn(new ArrayList<Feedback>()).when(feedbackDAO).getInternshipFeedbacks(999);

        // But throw an exception retrieving questionnaires
        doThrow(new SQLException("DB error getOngoingInternshipQuestionnaires"))
                .when(questionnaireDAO).getOngoingInternshipQuestionnaires(777);

        openMyInternshipInfoSpaceServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error getOngoingInternshipQuestionnaires"));
    }
}
