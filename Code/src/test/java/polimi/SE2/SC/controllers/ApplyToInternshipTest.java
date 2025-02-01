package polimi.SE2.SC.controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

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

import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ApplyToInternshipTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;

    @Mock private InternshipDAO internshipDAO;
    @Mock private StudentHasInternshipDAO studentHasInternshipDAO;
    @Mock private Connection testConnection; // The DB connection is mocked.

    // We will create a spy of the servlet so we can override the DAO creation methods.
    private ApplyToInternship applyToInternshipServlet;

    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create a spy of ApplyToInternship
        applyToInternshipServlet = spy(new ApplyToInternship());

        // Use TestUtils to create mocked ServletConfig/Context
        ServletConfig servletConfig = testUtils.setUp();
        applyToInternshipServlet.init(servletConfig);

        // Inject mocked connection via reflection
        Field connectionField = ApplyToInternship.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(applyToInternshipServlet, testConnection);

        // Override factory methods to return our mocks
        doReturn(internshipDAO).when(applyToInternshipServlet).createInternshipDAO();
        doReturn(studentHasInternshipDAO).when(applyToInternshipServlet).createStudentHasInternshipDAO();

        // Prepare a StringWriter for capturing servlet output
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // When request.getSession() is called, return our mocked session
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Student successfully applies to an existing internship
    // -------------------------------------------------------------------------
    @Test
    public void testStudentAppliesSuccess() throws Exception {
        // Student user in session
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // Provide valid internshipId
        when(request.getParameter("internshipId")).thenReturn("500");

        // DB returns an existing internship #500
        Internship internship = new Internship();
        internship.setId(500);
        doReturn(internship).when(internshipDAO).getInternshipById(500);

        // Stub the addApplication method
        doNothing().when(studentHasInternshipDAO).addApplication(500, 100);

        // Execute
        applyToInternshipServlet.doPost(request, response);

        // Expect success
        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertTrue(responseWriter.toString().isEmpty(), "No error expected in response body");
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Missing internshipId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingInternshipId() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // internshipId param is missing
        when(request.getParameter("internshipId")).thenReturn(null);

        applyToInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("The id for the internship is null!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Non-numeric internshipId
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidInternshipIdFormat() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // internshipId is "abc"
        when(request.getParameter("internshipId")).thenReturn("abc");

        applyToInternshipServlet.doPost(request, response);

        // We expect a parse failure -> SC_BAD_REQUEST
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: Internship doesn't exist
    // -------------------------------------------------------------------------
    @Test
    public void testInternshipNotFound() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("internshipId")).thenReturn("500");

        // DB returns null
        doReturn(null).when(internshipDAO).getInternshipById(500);

        applyToInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("No internship found for the given id"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: A Company tries to apply to an internship
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyApplies() throws Exception {
        // Company user
        Company companyUser = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // Provide valid internshipId
        when(request.getParameter("internshipId")).thenReturn("500");

        applyToInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: SQLException retrieving internship
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetInternship() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("internshipId")).thenReturn("500");

        doThrow(new SQLException("DB error retrieving internship"))
                .when(internshipDAO).getInternshipById(500);

        applyToInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error retrieving internship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: SQLException adding the student's application
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionAddApplication() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("internshipId")).thenReturn("500");

        // DB returns an existing internship #500
        Internship internship = new Internship();
        internship.setId(500);
        doReturn(internship).when(internshipDAO).getInternshipById(500);

        // DB error on addApplication
        doThrow(new SQLException("DB error adding application"))
                .when(studentHasInternshipDAO).addApplication(500, 100);

        applyToInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String out = responseWriter.toString();
        assertTrue(out.contains("DB error adding application"));
    }
}
