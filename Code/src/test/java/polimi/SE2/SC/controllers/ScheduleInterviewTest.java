package polimi.SE2.SC.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
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

import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.InterviewScheduleDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.InterviewSchedule;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.StudentHasInternship;
import polimi.SE2.SC.beans.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ScheduleInterviewTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private Connection testConnection;

    @Mock private StudentHasInternshipDAO studentHasInternshipDAO;
    @Mock private InternshipDAO internshipDAO;
    @Mock private InterviewScheduleDAO interviewScheduleDAO;

    private ScheduleInterview scheduleInterviewServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Spy the servlet
        scheduleInterviewServlet = spy(new ScheduleInterview());

        // Use TestUtils to set up mocked ServletConfig / ServletContext
        ServletConfig servletConfig = testUtils.setUp();
        scheduleInterviewServlet.init(servletConfig);

        // Inject our mocked connection via reflection
        Field connectionField = ScheduleInterview.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(scheduleInterviewServlet, testConnection);

        // Override the DAO creation methods
        doReturn(studentHasInternshipDAO).when(scheduleInterviewServlet).createStudentHasInternshipDAO();
        doReturn(internshipDAO).when(scheduleInterviewServlet).createInternshipDAO();
        doReturn(interviewScheduleDAO).when(scheduleInterviewServlet).createInterviewScheduleDAO();

        // Prepare a StringWriter
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // Return our mocked session
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Company schedules an interview successfully
    // -------------------------------------------------------------------------
    @Test
    public void testCompanySchedulesInterviewSuccess() throws Exception {
        // Company user
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // Provide valid date param + internship ID
        when(request.getParameter("date")).thenReturn("2025-12-31 10:30");
        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        // DB returns a valid StudentHasInternship
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // Company owns internship #999
        Internship i999 = new Internship();
        i999.setId(999);
        ArrayList<Internship> owned = new ArrayList<>();
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(100);

        // We schedule the interview
        doNothing().when(interviewScheduleDAO).scheduleInterview(any(), eq(555));

        // We return some interview schedules
        ArrayList<InterviewSchedule> schedules = new ArrayList<>();
        InterviewSchedule s1 = new InterviewSchedule();
        s1.setId(1);
        schedules.add(s1);
        doReturn(schedules).when(interviewScheduleDAO).getOngoingInternshipInterviewSchedules(555);

        // Execute
        scheduleInterviewServlet.doPost(request, response);

        // Expect SC_OK
        verify(response).setStatus(HttpServletResponse.SC_OK);
        // We can parse the JSON for "interviewSchedules" if we want
        assertFalse(responseWriter.toString().isEmpty());
        assertTrue(responseWriter.toString().contains("interviewSchedules"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Missing date param
    // -------------------------------------------------------------------------
    @Test
    public void testMissingDateParam() throws Exception {
        Company companyUser = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // date is null
        when(request.getParameter("date")).thenReturn(null);
        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        scheduleInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Invalid date format
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidDateFormat() throws Exception {
        Company companyUser = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // Provide an incorrect format
        when(request.getParameter("date")).thenReturn("31-12-2025");
        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        scheduleInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("Invalid date format"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: Missing studentHasInternshipId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingStudentHasInternshipId() throws Exception {
        Company companyUser = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("date")).thenReturn("2025-12-31 10:30");
        // no studentHasInternshipId
        when(request.getParameter("studentHasInternshipId")).thenReturn(null);

        scheduleInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("No valid id"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: StudentHasInternship not found
    // -------------------------------------------------------------------------
    @Test
    public void testStudentHasInternshipNotFound() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("date")).thenReturn("2025-12-31 10:30");
        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        doReturn(null).when(studentHasInternshipDAO).getStudentHasInternship(555);

        scheduleInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("There is no ongoing internship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: Wrong time to schedule (applicationStatus != 'selectionProcess')
    // -------------------------------------------------------------------------
    @Test
    public void testWrongTimeToSchedule() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("date")).thenReturn("2025-12-31 10:30");
        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("ongoing"); // not selectionProcess
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company does own #999
        Internship i999 = new Internship();
        i999.setId(999);
        ArrayList<Internship> owned = new ArrayList<>();
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(100);

        scheduleInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("not authorized to schedule an interview"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: User is Student -> not allowed
    // -------------------------------------------------------------------------
    @Test
    public void testUserIsStudent() throws Exception {
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("date")).thenReturn("2025-12-31 10:30");
        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        scheduleInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // -------------------------------------------------------------------------
    // ★ Test 8: Company does not own the internship
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyNotOwner() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("date")).thenReturn("2025-12-31 10:30");
        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company #100 doesn't own #999
        ArrayList<Internship> owned = new ArrayList<>();
        // e.g. the company only owns #888
        Internship i888 = new Internship();
        i888.setId(888);
        owned.add(i888);
        doReturn(owned).when(internshipDAO).getCompanyInternships(100);

        scheduleInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("you are not the company responsible"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: SQLException on scheduleInterview
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionScheduleInterview() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("date")).thenReturn("2025-12-31 10:30");
        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        // DB data
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        Internship i999 = new Internship();
        i999.setId(999);
        ArrayList<Internship> owned = new ArrayList<>();
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(100);

        doThrow(new SQLException("DB error scheduling interview"))
                .when(interviewScheduleDAO).scheduleInterview(any(), eq(555));

        scheduleInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error scheduling interview"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 10: SQLException retrieving interview schedules
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetInterviewSchedules() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("date")).thenReturn("2025-12-31 10:30");
        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        Internship i999 = new Internship();
        i999.setId(999);
        ArrayList<Internship> owned = new ArrayList<>();
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(100);

        doNothing().when(interviewScheduleDAO).scheduleInterview(any(), eq(555));

        doThrow(new SQLException("DB error retrieving interview schedules"))
                .when(interviewScheduleDAO).getOngoingInternshipInterviewSchedules(555);

        scheduleInterviewServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error retrieving interview schedules"));
    }
}
