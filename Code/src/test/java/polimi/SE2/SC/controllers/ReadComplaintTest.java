package polimi.SE2.SC.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.DAOs.ComplaintDAO;
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Complaint;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.StudentHasInternship;
import polimi.SE2.SC.beans.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReadComplaintTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;

    @Mock private ComplaintDAO complaintDAO;
    @Mock private InternshipDAO internshipDAO;
    @Mock private StudentHasInternshipDAO studentHasInternshipDAO;
    @Mock private Connection testConnection;

    private ReadComplaint readComplaintServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create a spy of the ReadComplaint servlet
        readComplaintServlet = spy(new ReadComplaint());

        // Use TestUtils to create a mocked ServletConfig/ServletContext
        ServletConfig servletConfig = testUtils.setUp();
        readComplaintServlet.init(servletConfig);

        // Inject the mocked connection into the private 'connection' field via reflection
        Field connectionField = ReadComplaint.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(readComplaintServlet, testConnection);

        // Override the factory methods to return our mocks
        doReturn(complaintDAO).when(readComplaintServlet).createComplaintDAO();
        doReturn(internshipDAO).when(readComplaintServlet).createInternshipDAO();
        doReturn(studentHasInternshipDAO).when(readComplaintServlet).createStudentHasInternshipDAO();

        // Prepare a StringWriter to capture servlet output
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // When request.getSession() is called, return our mocked session
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Company reads a valid complaint
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyReadsComplaintSuccess() throws Exception {
        // Company user
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // Provide a valid complaint ID
        when(request.getParameter("complaintId")).thenReturn("1001");

        // DB returns a complaint #1001
        Complaint complaint = new Complaint();
        complaint.setId(1001);
        complaint.setStudentHasInternshipId(777);
        doReturn(complaint).when(complaintDAO).getComplaint(1001);

        // DB returns a StudentHasInternship #777 referencing internship #999
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(777);
        shi.setInternshipId(999);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(777);

        // Company #50 owns internship #999
        Internship internship999 = new Internship();
        internship999.setId(999);
        ArrayList<Internship> internshipsOwned = new ArrayList<>();
        internshipsOwned.add(internship999);
        doReturn(internshipsOwned).when(internshipDAO).getCompanyInternships(50);

        // Mark complaint as read
        doNothing().when(complaintDAO).hasBeenRead(1001, false);

        // Execute
        readComplaintServlet.doPost(request, response);

        // Expect SC_OK
        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertTrue(responseWriter.toString().isEmpty(), "No error text expected");
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Missing or null complaintId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingComplaintId() throws Exception {
        Company companyUser = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // complaintId is null
        when(request.getParameter("complaintId")).thenReturn(null);

        readComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("The id for the complaint is null!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Complaint not found (null from DB)
    // -------------------------------------------------------------------------
    @Test
    public void testComplaintNotFound() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("complaintId")).thenReturn("1001");

        // DB returns null
        doReturn(null).when(complaintDAO).getComplaint(1001);

        readComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("The complaint you are trying to close does not exists"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: SQLException retrieving complaint
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetComplaint() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("complaintId")).thenReturn("1001");

        doThrow(new SQLException("DB error retrieving complaint"))
                .when(complaintDAO).getComplaint(1001);

        readComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String output = responseWriter.toString();
        assertTrue(output.contains("DB error retrieving complaint"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: Student reads complaint, but it's not their internship
    // -------------------------------------------------------------------------
    @Test
    public void testStudentReadsNotOwnedComplaint() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(111);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaintId")).thenReturn("1002");

        // DB has complaint #1002 referencing StudentHasInternship #777
        Complaint complaint = new Complaint();
        complaint.setId(1002);
        complaint.setStudentHasInternshipId(777);
        doReturn(complaint).when(complaintDAO).getComplaint(1002);

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(777);
        shi.setInternshipId(999);
        // But the studentId in shi is 9999, not 111
        shi.setStudentId(9999);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(777);

        readComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("You are not enrolled for the given internship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: Student tries to read but they are the one who closed it
    //           (complaint.isClosedByStudent() == true, not yet accepted by company)
    // -------------------------------------------------------------------------
    @Test
    public void testStudentRejectsButAlreadyClosedByStudent() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(111);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaintId")).thenReturn("1003");

        // DB complaint #1003 referencing StudentHasInternship #777
        Complaint complaint = new Complaint();
        complaint.setId(1003);
        complaint.setStudentHasInternshipId(777);
        complaint.setClosedByStudent(true);
        complaint.setClosedByCompany(false);

        doReturn(complaint).when(complaintDAO).getComplaint(1003);

        // The studentHasInternship references the same student #111
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(777);
        shi.setStudentId(111);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(777);

        readComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("you are not authorize to reject it at this point"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: Company not owner of internship
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyNotOwnerOfInternship() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("complaintId")).thenReturn("1004");

        // DB has complaint #1004 referencing StudentHasInternship #888
        Complaint complaint = new Complaint();
        complaint.setId(1004);
        complaint.setStudentHasInternshipId(888);
        doReturn(complaint).when(complaintDAO).getComplaint(1004);

        // The studentHasInternship references internship #999
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(888);
        shi.setInternshipId(999);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(888);

        // The company #50 owns internship #123, not #999
        ArrayList<Internship> ownedByCompany = new ArrayList<>();
        Internship i123 = new Internship();
        i123.setId(123);
        ownedByCompany.add(i123);
        doReturn(ownedByCompany).when(internshipDAO).getCompanyInternships(50);

        readComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("you are not the company responsible"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 8: SQLException retrieving studentHasInternship
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionStudentHasInternship() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("complaintId")).thenReturn("1005");

        // DB has complaint #1005
        Complaint complaint = new Complaint();
        complaint.setId(1005);
        complaint.setStudentHasInternshipId(777);
        doReturn(complaint).when(complaintDAO).getComplaint(1005);

        doThrow(new SQLException("DB error on StudentHasInternship"))
                .when(studentHasInternshipDAO).getStudentHasInternship(777);

        readComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error on StudentHasInternship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: SQLException marking the complaint as read
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionHasBeenRead() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("complaintId")).thenReturn("1006");

        // DB has complaint #1006 referencing StudentHasInternship #777
        Complaint complaint = new Complaint();
        complaint.setId(1006);
        complaint.setStudentHasInternshipId(777);
        doReturn(complaint).when(complaintDAO).getComplaint(1006);

        // The studentHasInternship references internship #999
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(777);
        shi.setInternshipId(999);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(777);

        // The company #50 does own internship #999
        Internship i999 = new Internship();
        i999.setId(999);
        ArrayList<Internship> ownedByCompany = new ArrayList<>();
        ownedByCompany.add(i999);
        doReturn(ownedByCompany).when(internshipDAO).getCompanyInternships(50);

        // But there's an error marking complaint as read
        doThrow(new SQLException("DB error hasBeenRead"))
                .when(complaintDAO).hasBeenRead(1006, false);

        readComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error hasBeenRead"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 10: Non-numeric complaintId
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidComplaintIdFormat() throws Exception {
        // Company user
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // complaintId is "abc"
        when(request.getParameter("complaintId")).thenReturn("abc");

        readComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
