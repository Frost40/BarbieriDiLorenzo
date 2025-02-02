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

import polimi.SE2.SC.DAOs.InterviewScheduleDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RejectInterviewDateTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private Connection testConnection;

    @Mock private InterviewScheduleDAO interviewScheduleDAO;
    @Mock private StudentHasInternshipDAO studentHasInternshipDAO;
    @Mock private InternshipDAO internshipDAO;

    private RejectInterviewDate rejectInterviewDateServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Spy the servlet
        rejectInterviewDateServlet = spy(new RejectInterviewDate());

        // Use TestUtils for ServletConfig
        ServletConfig servletConfig = testUtils.setUp();
        rejectInterviewDateServlet.init(servletConfig);

        // Inject the connection
        Field connectionField = RejectInterviewDate.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(rejectInterviewDateServlet, testConnection);

        // Override DAO creation
        doReturn(interviewScheduleDAO).when(rejectInterviewDateServlet).createInterviewScheduleDAO();
        doReturn(studentHasInternshipDAO).when(rejectInterviewDateServlet).createStudentHasInternshipDAO();
        doReturn(internshipDAO).when(rejectInterviewDateServlet).createInternshipDAO();

        // Set up response writer
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // Default session
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Student rejects an interview date (happy path)
    // -------------------------------------------------------------------------
    @Test
    public void testStudentRejectsInterviewDateSuccess() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // Provide valid param
        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("interviewScheduleId")).thenReturn("999");

        // DB has an interviewSchedule #999 referencing studentHasInternshipId=555
        InterviewSchedule schedule = new InterviewSchedule();
        schedule.setId(999);
        schedule.setStudentHasInternshipId(555);
        doReturn(schedule).when(interviewScheduleDAO).getInterviewSchedule(999);

        // DB has StudentHasInternship #555 with studentId=100
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(100);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // We reject the date
        doNothing().when(interviewScheduleDAO).rejectInterviewDate(999);

        // Execute
        rejectInterviewDateServlet.doPost(request, response);

        // SC_OK
        verify(response).setStatus(HttpServletResponse.SC_OK);
        // No body or JSON returned, so presumably no further checks needed
        assertTrue(responseWriter.toString().isEmpty(), "No error text expected");
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Missing studentHasInternshipId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingStudentHasInternshipId() throws Exception {
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // Provide no studentHasInternshipId
        when(request.getParameter("studentHasInternshipId")).thenReturn(null);
        when(request.getParameter("interviewScheduleId")).thenReturn("999");

        rejectInterviewDateServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("The id for the student-internship is null!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Missing interviewScheduleId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingInterviewScheduleId() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        // no interviewScheduleId
        when(request.getParameter("interviewScheduleId")).thenReturn(null);

        rejectInterviewDateServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("The id for the interview schedule is null!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: InterviewSchedule not found
    // -------------------------------------------------------------------------
    @Test
    public void testInterviewScheduleNotFound() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("interviewScheduleId")).thenReturn("999");

        doReturn(null).when(interviewScheduleDAO).getInterviewSchedule(999);

        rejectInterviewDateServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("No interview schedule found"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: Mismatch between param's studentHasInternshipId and schedule
    // -------------------------------------------------------------------------
    @Test
    public void testMismatchStudentHasInternshipId() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("interviewScheduleId")).thenReturn("999");

        InterviewSchedule schedule = new InterviewSchedule();
        schedule.setId(999);
        schedule.setStudentHasInternshipId(12345);
        doReturn(schedule).when(interviewScheduleDAO).getInterviewSchedule(999);

        rejectInterviewDateServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("does not match the one found for the interviewSchedule"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: StudentHasInternship not found
    // -------------------------------------------------------------------------
    @Test
    public void testStudentHasInternshipNotFound() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("interviewScheduleId")).thenReturn("999");

        // DB has a schedule referencing #555
        InterviewSchedule schedule = new InterviewSchedule();
        schedule.setId(999);
        schedule.setStudentHasInternshipId(555);
        doReturn(schedule).when(interviewScheduleDAO).getInterviewSchedule(999);

        // But we can't find the StudentHasInternship
        doReturn(null).when(studentHasInternshipDAO).getStudentHasInternship(555);

        rejectInterviewDateServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    }

    // -------------------------------------------------------------------------
    // ★ Test 7: The user is a company (not a student)
    // -------------------------------------------------------------------------
    @Test
    public void testUserIsCompany() throws Exception {
        Company companyUser = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("interviewScheduleId")).thenReturn("999");

        rejectInterviewDateServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // -------------------------------------------------------------------------
    // ★ Test 8: Student is not the same one in the StudentHasInternship
    // -------------------------------------------------------------------------
    @Test
    public void testStudentMismatch() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("interviewScheduleId")).thenReturn("999");

        // DB has schedule #999 referencing #555
        InterviewSchedule schedule = new InterviewSchedule();
        schedule.setId(999);
        schedule.setStudentHasInternshipId(555);
        doReturn(schedule).when(interviewScheduleDAO).getInterviewSchedule(999);

        // DB has StudentHasInternship #555 with studentId=999 => mismatch
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(999);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        rejectInterviewDateServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("you are not the associated to the given internship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: Wrong time to evaluate (applicationStatus != 'selectionProcess')
    // -------------------------------------------------------------------------
    @Test
    public void testWrongTimeToReject() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("interviewScheduleId")).thenReturn("999");

        // DB interviewSchedule
        InterviewSchedule schedule = new InterviewSchedule();
        schedule.setId(999);
        schedule.setStudentHasInternshipId(555);
        doReturn(schedule).when(interviewScheduleDAO).getInterviewSchedule(999);

        // DB StudentHasInternship => status=ongoing
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(100);
        shi.setApplicationStatus("ongoing");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        rejectInterviewDateServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("This is not the correct time to evaluate an interview"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 10: SQLException while rejecting
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionRejecting() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("interviewScheduleId")).thenReturn("999");

        InterviewSchedule schedule = new InterviewSchedule();
        schedule.setId(999);
        schedule.setStudentHasInternshipId(555);
        doReturn(schedule).when(interviewScheduleDAO).getInterviewSchedule(999);

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(100);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        doThrow(new SQLException("DB error rejecting interview date"))
                .when(interviewScheduleDAO).rejectInterviewDate(999);

        rejectInterviewDateServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error rejecting interview date"));
    }
}
