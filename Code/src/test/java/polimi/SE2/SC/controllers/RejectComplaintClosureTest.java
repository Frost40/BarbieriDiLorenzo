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

import polimi.SE2.SC.DAOs.ComplaintDAO;
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RejectComplaintClosureTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private Connection testConnection;

    @Mock private ComplaintDAO complaintDAO;
    @Mock private StudentHasInternshipDAO studentHasInternshipDAO;
    @Mock private InternshipDAO internshipDAO;

    private RejectComplaintClosure rejectComplaintClosureServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Spy the servlet
        rejectComplaintClosureServlet = spy(new RejectComplaintClosure());

        // Use TestUtils for the mocked ServletConfig/Context
        ServletConfig servletConfig = testUtils.setUp();
        rejectComplaintClosureServlet.init(servletConfig);

        // Inject the mocked connection
        Field connField = RejectComplaintClosure.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connField.set(rejectComplaintClosureServlet, testConnection);

        // Override DAO creation
        doReturn(complaintDAO).when(rejectComplaintClosureServlet).createComplaintDAO();
        doReturn(studentHasInternshipDAO).when(rejectComplaintClosureServlet).createStudentHasInternshipDAO();
        doReturn(internshipDAO).when(rejectComplaintClosureServlet).createInternshipDAO();

        // Prepare response writer
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // Default session
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Student rejects complaint closure successfully (happy path)
    // -------------------------------------------------------------------------
    @Test
    public void testStudentRejectsClosureSuccess() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // Provide valid complaintId
        when(request.getParameter("complaintId")).thenReturn("555");

        // DB returns complaint #555
        Complaint complaint = new Complaint();
        complaint.setId(555);
        // It's not closed by both sides
        complaint.setClosedByStudent(false);
        complaint.setClosedByCompany(true); // So the company proposed closure, student can reject
        doReturn(complaint).when(complaintDAO).getComplaint(555);

        // The DB has a StudentHasInternship with studentId=50
        StudentHasInternship shi = new StudentHasInternship();
        shi.setStudentId(50);
        shi.setInternshipId(999);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(complaint.getStudentHasInternshipId());

        // The student is the correct user, so no mismatch
        // The user hasn't proposed closure, so can reject
        doNothing().when(complaintDAO).rejectClosure(555);

        rejectComplaintClosureServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        // no body => presumably empty
        assertTrue(responseWriter.toString().isEmpty(), "No error text expected on success");
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Missing complaintId param
    // -------------------------------------------------------------------------
    @Test
    public void testMissingComplaintId() throws Exception {
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // complaintId is null
        when(request.getParameter("complaintId")).thenReturn(null);

        rejectComplaintClosureServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("The id for the complaint is null!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: complaintId parse error
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidComplaintIdFormat() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaintId")).thenReturn("abc");

        rejectComplaintClosureServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: Complaint not found => SC_UNAUTHORIZED
    // -------------------------------------------------------------------------
    @Test
    public void testComplaintNotFound() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaintId")).thenReturn("555");

        // DB => null
        doReturn(null).when(complaintDAO).getComplaint(555);

        rejectComplaintClosureServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("does not exists"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: SQLException retrieving complaint
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetComplaint() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaintId")).thenReturn("555");

        doThrow(new SQLException("DB error retrieving complaint"))
                .when(complaintDAO).getComplaint(555);

        rejectComplaintClosureServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error retrieving complaint"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: StudentHasInternship retrieval fails
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetStudentHasInternship() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaintId")).thenReturn("555");

        Complaint c = new Complaint();
        c.setId(555);
        c.setStudentHasInternshipId(777);
        doReturn(c).when(complaintDAO).getComplaint(555);

        doThrow(new SQLException("DB error retrieving StudentHasInternship"))
                .when(studentHasInternshipDAO).getStudentHasInternship(777);

        rejectComplaintClosureServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error retrieving StudentHasInternship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: Student mismatch => not enrolled
    // -------------------------------------------------------------------------
    @Test
    public void testStudentMismatch() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaintId")).thenReturn("555");

        // DB => complaint #555 => belongs to StudentHasInternship #777
        Complaint c = new Complaint();
        c.setId(555);
        c.setStudentHasInternshipId(777);
        doReturn(c).when(complaintDAO).getComplaint(555);

        // DB => #777 => studentId=999 => mismatch
        StudentHasInternship shi = new StudentHasInternship();
        shi.setStudentId(999);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(777);

        rejectComplaintClosureServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("You are not enrolled for the given internship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 8: Student tries to reject but they previously proposed closure
    // -------------------------------------------------------------------------
    @Test
    public void testStudentPreviouslyProposedClosure() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaintId")).thenReturn("555");

        Complaint c = new Complaint();
        c.setId(555);
        c.setStudentHasInternshipId(777);
        c.setClosedByStudent(true);
        c.setClosedByCompany(false); // only the student has proposed closure
        doReturn(c).when(complaintDAO).getComplaint(555);

        StudentHasInternship shi = new StudentHasInternship();
        shi.setStudentId(200);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(777);

        rejectComplaintClosureServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("Since you are the one who proposed the closure"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: Company mismatch
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyMismatch() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("complaintId")).thenReturn("555");

        // DB => complaint #555 => StudentHasInternship #777
        Complaint c = new Complaint();
        c.setId(555);
        c.setStudentHasInternshipId(777);
        doReturn(c).when(complaintDAO).getComplaint(555);

        // DB => #777 => internshipId=999
        StudentHasInternship shi = new StudentHasInternship();
        shi.setInternshipId(999);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(777);

        // The company #300 only owns #888 => mismatch
        ArrayList<Internship> owned = new ArrayList<>();
        Internship i888 = new Internship();
        i888.setId(888);
        owned.add(i888);
        doReturn(owned).when(internshipDAO).getCompanyInternships(300);

        rejectComplaintClosureServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("you are not the company responsible"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 10: Company tries to reject but they previously proposed closure
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyPreviouslyProposedClosure() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("complaintId")).thenReturn("555");

        // DB => complaint #555 => closedByCompany=true, closedByStudent=false => company previously proposed
        Complaint c = new Complaint();
        c.setId(555);
        c.setStudentHasInternshipId(777);
        c.setClosedByCompany(true);
        c.setClosedByStudent(false);
        doReturn(c).when(complaintDAO).getComplaint(555);

        // DB => #777 => internshipId=999
        StudentHasInternship shi = new StudentHasInternship();
        shi.setInternshipId(999);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(777);

        // The company #300 does own #999
        ArrayList<Internship> owned = new ArrayList<>();
        Internship i999 = new Internship();
        i999.setId(999);
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(300);

        rejectComplaintClosureServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("Since you are the one who proposed the closure you are not authorize to reject it"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 11: Complaint already closed by both => can't reject
    // -------------------------------------------------------------------------
    @Test
    public void testAlreadyClosedByBoth() throws Exception {
        // Could be student or company. Let's pick student
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaintId")).thenReturn("555");

        Complaint c = new Complaint();
        c.setId(555);
        c.setStudentHasInternshipId(777);
        c.setClosedByCompany(true);
        c.setClosedByStudent(true); // both sides => fully closed
        doReturn(c).when(complaintDAO).getComplaint(555);

        StudentHasInternship shi = new StudentHasInternship();
        shi.setStudentId(200);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(777);

        rejectComplaintClosureServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("The complaint was already closed"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 12: SQLException rejecting closure
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionRejectClosure() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaintId")).thenReturn("555");

        // DB => complaint #555 => closedByCompany=true, closedByStudent=false => can be rejected by student
        Complaint c = new Complaint();
        c.setId(555);
        c.setStudentHasInternshipId(777);
        c.setClosedByStudent(false);
        c.setClosedByCompany(true);
        doReturn(c).when(complaintDAO).getComplaint(555);

        // DB => #777 => belongs to student #200
        StudentHasInternship shi = new StudentHasInternship();
        shi.setStudentId(200);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(777);

        // Throw DB error
        doThrow(new SQLException("DB error rejectClosure"))
                .when(complaintDAO).rejectClosure(555);

        rejectComplaintClosureServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error rejectClosure"));
    }
}
