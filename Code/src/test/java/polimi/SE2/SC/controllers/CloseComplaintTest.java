package polimi.SE2.SC.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

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

import polimi.SE2.SC.DAOs.ComplaintDAO;
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CloseComplaintTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private Connection testConnection;

    @Mock private StudentHasInternshipDAO studentHasInternshipDAO;
    @Mock private ComplaintDAO complaintDAO;
    @Mock private InternshipDAO internshipDAO;

    private CloseComplaint closeComplaintServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        closeComplaintServlet = spy(new CloseComplaint());

        ServletConfig servletConfig = testUtils.setUp();
        closeComplaintServlet.init(servletConfig);

        // Inject connection
        Field connField = CloseComplaint.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connField.set(closeComplaintServlet, testConnection);

        doReturn(complaintDAO).when(closeComplaintServlet).createComplaintDAO();
        doReturn(studentHasInternshipDAO).when(closeComplaintServlet).createStudentHasInternshipDAO();
        doReturn(internshipDAO).when(closeComplaintServlet).createInternshipDAO();

        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Student or Company closes a complaint => success
    // -------------------------------------------------------------------------
    @Test
    public void testCloseComplaintSuccess() throws Exception {
        // Could be a student or company. Let's pick a Student
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // Valid param
        when(request.getParameter("complaintId")).thenReturn("555");

        // DB returns complaint #555
        Complaint complaint = new Complaint();
        complaint.setId(555);

        ArrayList<Complaint> userComplaints = new ArrayList<>();
        userComplaints.add(complaint);

        doReturn(complaint).when(complaintDAO).getComplaint(555);
        doReturn(userComplaints).when(complaintDAO).getUserComplaints(100, true);

        doNothing().when(complaintDAO).requestClosure(555, true);

        // Return an updated open complaint list
        ArrayList<Complaint> openComplaints = new ArrayList<>();
        doReturn(openComplaints).when(complaintDAO).getOngoingInternshipOpenComplaints(complaint.getStudentHasInternshipId());

        closeComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertTrue(responseWriter.toString().contains("\"openComplaints\""));
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Missing complaintId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingComplaintId() throws Exception {
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaintId")).thenReturn(null);

        closeComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("Null complaint id!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Parse error for complaintId
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidComplaintIdFormat() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // e.g. "abc"
        when(request.getParameter("complaintId")).thenReturn("abc");

        closeComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: Complaint not found
    // -------------------------------------------------------------------------
    @Test
    public void testComplaintNotFound() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaintId")).thenReturn("555");

        doReturn(null).when(complaintDAO).getComplaint(555);

        closeComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("The complaint you are trying to close does not exist"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: Complaint not in userComplaints => unauthorized
    // -------------------------------------------------------------------------
    @Test
    public void testComplaintNotInUserComplaints() throws Exception {
        // Suppose user is student
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaintId")).thenReturn("555");

        Complaint complaint = new Complaint();
        complaint.setId(555);

        doReturn(complaint).when(complaintDAO).getComplaint(555);

        // DB returns empty userComplaints => not authorized
        doReturn(new ArrayList<Complaint>()).when(complaintDAO).getUserComplaints(100, true);

        closeComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("You are not authorized"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: SQLException retrieving complaint
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetComplaint() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaintId")).thenReturn("555");

        doThrow(new SQLException("DB error getComplaint"))
                .when(complaintDAO).getComplaint(555);

        closeComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error getComplaint"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: SQLException retrieving userComplaints
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetUserComplaints() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaintId")).thenReturn("555");

        // DB has a complaint #555
        Complaint complaint = new Complaint();
        complaint.setId(555);
        doReturn(complaint).when(complaintDAO).getComplaint(555);

        doThrow(new SQLException("DB error getUserComplaints"))
                .when(complaintDAO).getUserComplaints(100, true);

        closeComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error getUserComplaints"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 8: SQLException requesting closure
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionRequestClosure() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaintId")).thenReturn("555");

        // DB complaint
        Complaint complaint = new Complaint();
        complaint.setId(555);

        ArrayList<Complaint> userComplaints = new ArrayList<>();
        userComplaints.add(complaint);

        doReturn(complaint).when(complaintDAO).getComplaint(555);
        doReturn(userComplaints).when(complaintDAO).getUserComplaints(100, true);

        doThrow(new SQLException("DB error requestClosure"))
                .when(complaintDAO).requestClosure(555, true);

        closeComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error requestClosure"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: SQLException retrieving open complaints afterwards
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetOpenComplaints() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaintId")).thenReturn("555");

        // DB complaint #555
        Complaint complaint = new Complaint();
        complaint.setId(555);
        complaint.setStudentHasInternshipId(9999);
        ArrayList<Complaint> userComplaints = new ArrayList<>();
        userComplaints.add(complaint);

        doReturn(complaint).when(complaintDAO).getComplaint(555);
        doReturn(userComplaints).when(complaintDAO).getUserComplaints(100, true);
        doNothing().when(complaintDAO).requestClosure(555, true);

        doThrow(new SQLException("DB error getOpenComplaints"))
                .when(complaintDAO).getOngoingInternshipOpenComplaints(9999);

        closeComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error getOpenComplaints"));
    }
}
