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
import java.util.Optional;

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

import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.InterviewScheduleDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EvaluateInterviewTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private Connection testConnection;

    @Mock private StudentHasInternshipDAO studentHasInternshipDAO;
    @Mock private InternshipDAO internshipDAO;
    @Mock private InterviewScheduleDAO interviewScheduleDAO;

    private EvaluateInterview evaluateInterviewServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        evaluateInterviewServlet = spy(new EvaluateInterview());

        ServletConfig servletConfig = testUtils.setUp();
        evaluateInterviewServlet.init(servletConfig);

        // Inject mocked connection
        Field connectionField = EvaluateInterview.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(evaluateInterviewServlet, testConnection);

        // Override creation
        doReturn(studentHasInternshipDAO).when(evaluateInterviewServlet).createStudentHasInternshipDAO();
        doReturn(internshipDAO).when(evaluateInterviewServlet).createInternshipDAO();
        doReturn(interviewScheduleDAO).when(evaluateInterviewServlet).createInterviewScheduleDAO();

        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Company successfully evaluates an interview
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyEvaluatesInterviewSuccess() throws Exception {
        // Company user
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // Provide valid param
        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("interviewScheduleId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("true");

        // DB has an interviewSchedule #999 referencing studentHasInternship=555
        InterviewSchedule schedule = new InterviewSchedule();
        schedule.setId(999);
        schedule.setStudentHasInternshipId(555);
        doReturn(schedule).when(interviewScheduleDAO).getInterviewSchedule(999);

        // DB has StudentHasInternship #555 referencing internship=888
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(888);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company #300 owns internship #888
        Internship i888 = new Internship();
        i888.setId(888);
        ArrayList<Internship> owned = new ArrayList<>();
        owned.add(i888);
        doReturn(owned).when(internshipDAO).getCompanyInternships(300);

        // setCompanyDecision
        doNothing().when(interviewScheduleDAO).setCompanyDecision(999, true);

        evaluateInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertTrue(responseWriter.toString().isEmpty());
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Missing studentHasInternshipId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingStudentHasInternshipId() throws Exception {
        Company companyUser = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // no studentHasInternshipId
        when(request.getParameter("studentHasInternshipId")).thenReturn(null);
        when(request.getParameter("interviewScheduleId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("true");

        evaluateInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("The id for the student-internship is null!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Missing interviewScheduleId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingInterviewScheduleId() throws Exception {
        Company companyUser = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        // no interviewScheduleId
        when(request.getParameter("interviewScheduleId")).thenReturn(null);
        when(request.getParameter("evaluation")).thenReturn("true");

        evaluateInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("The id for the interview schedule is null!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: Invalid evaluation param
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidEvaluationParam() throws Exception {
        Company companyUser = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("interviewScheduleId")).thenReturn("999");
        // e.g. "yes" => invalid
        when(request.getParameter("evaluation")).thenReturn("yes");

        evaluateInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("Invalid evaluation parameter"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: interviewSchedule not found
    // -------------------------------------------------------------------------
    @Test
    public void testInterviewScheduleNotFound() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("interviewScheduleId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("false");

        doReturn(null).when(interviewScheduleDAO).getInterviewSchedule(999);

        evaluateInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("No interview schedule found"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: Mismatch studentHasInternshipId
    // -------------------------------------------------------------------------
    @Test
    public void testMismatchStudentHasInternshipId() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("interviewScheduleId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("true");

        InterviewSchedule schedule = new InterviewSchedule();
        schedule.setId(999);
        schedule.setStudentHasInternshipId(12345); // mismatch
        doReturn(schedule).when(interviewScheduleDAO).getInterviewSchedule(999);

        evaluateInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("does not match the one found for the interviewSchedule"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: StudentHasInternship not found
    // -------------------------------------------------------------------------
    @Test
    public void testStudentHasInternshipNotFound() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("interviewScheduleId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("true");

        // DB has schedule referencing #555
        InterviewSchedule schedule = new InterviewSchedule();
        schedule.setId(999);
        schedule.setStudentHasInternshipId(555);
        doReturn(schedule).when(interviewScheduleDAO).getInterviewSchedule(999);

        // But no StudentHasInternship found
        doReturn(null).when(studentHasInternshipDAO).getStudentHasInternship(555);

        evaluateInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    }

    // -------------------------------------------------------------------------
    // ★ Test 8: User is Student -> not allowed
    // -------------------------------------------------------------------------
    @Test
    public void testUserIsStudent() throws Exception {
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("interviewScheduleId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("true");

        evaluateInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: The company does not own the internship
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyNotOwner() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("interviewScheduleId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("false");

        InterviewSchedule schedule = new InterviewSchedule();
        schedule.setId(999);
        schedule.setStudentHasInternshipId(555);
        doReturn(schedule).when(interviewScheduleDAO).getInterviewSchedule(999);

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(888);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company #300 only owns internship #777, not #888
        Internship i777 = new Internship();
        i777.setId(777);
        ArrayList<Internship> owned = new ArrayList<>();
        owned.add(i777);
        doReturn(owned).when(internshipDAO).getCompanyInternships(300);

        evaluateInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("you are not the company responsible"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 10: Wrong time (applicationStatus != 'selectionProcess')
    // -------------------------------------------------------------------------
    @Test
    public void testWrongTime() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("interviewScheduleId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("true");

        InterviewSchedule schedule = new InterviewSchedule();
        schedule.setId(999);
        schedule.setStudentHasInternshipId(555);
        doReturn(schedule).when(interviewScheduleDAO).getInterviewSchedule(999);

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(888);
        shi.setApplicationStatus("ongoing"); // not selectionProcess
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company does own #888
        Internship i888 = new Internship();
        i888.setId(888);
        ArrayList<Internship> owned = new ArrayList<>();
        owned.add(i888);
        doReturn(owned).when(internshipDAO).getCompanyInternships(300);

        evaluateInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("This is not the correct time to evaluate an interview"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 11: SQLException setCompanyDecision
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionSetCompanyDecision() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("interviewScheduleId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("false");

        InterviewSchedule schedule = new InterviewSchedule();
        schedule.setId(999);
        schedule.setStudentHasInternshipId(555);
        doReturn(schedule).when(interviewScheduleDAO).getInterviewSchedule(999);

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(888);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        Internship i888 = new Internship();
        i888.setId(888);
        ArrayList<Internship> owned = new ArrayList<>();
        owned.add(i888);
        doReturn(owned).when(internshipDAO).getCompanyInternships(300);

        doThrow(new SQLException("DB error setting decision"))
                .when(interviewScheduleDAO).setCompanyDecision(999, false);

        evaluateInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error setting decision"));
    }
}
