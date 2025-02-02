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
import polimi.SE2.SC.beans.StudentHasInternship;
import polimi.SE2.SC.beans.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EndInternshipTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private Connection testConnection;

    @Mock private StudentHasInternshipDAO studentHasInternshipDAO;
    @Mock private InternshipDAO internshipDAO;

    private EndInternship endInternshipServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        endInternshipServlet = spy(new EndInternship());

        ServletConfig servletConfig = testUtils.setUp();
        endInternshipServlet.init(servletConfig);

        // Inject mocked Connection
        Field connField = EndInternship.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connField.set(endInternshipServlet, testConnection);

        // Override factory methods
        doReturn(studentHasInternshipDAO).when(endInternshipServlet).createStudentHasInternshipDAO();
        doReturn(internshipDAO).when(endInternshipServlet).createInternshipDAO();

        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Company ends internship successfully
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyEndsInternshipSuccess() throws Exception {
        // Company user
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        // DB has a StudentHasInternship #555 => "ongoing" + startDate != null
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("ongoing");
        shi.setStartDate(new java.util.Date()); // not null
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // Company #300 owns internship #999
        ArrayList<Internship> owned = new ArrayList<>();
        Internship i999 = new Internship();
        i999.setId(999);
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(300);

        // endInternshipExperience => no exception
        doNothing().when(studentHasInternshipDAO).endInternshipExperience(555);

        endInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertTrue(responseWriter.toString().isEmpty(), "No error expected on success");
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Missing studentHasInternshipId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingStudentHasInternshipId() throws Exception {
        Company companyUser = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // param is null
        when(request.getParameter("studentHasInternshipId")).thenReturn(null);

        endInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("The id for the student-internship is null!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: StudentHasInternship not found
    // -------------------------------------------------------------------------
    @Test
    public void testStudentHasInternshipNotFound() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        doReturn(null).when(studentHasInternshipDAO).getStudentHasInternship(555);

        endInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("No student-internship association found"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: User is a student => not authorized
    // -------------------------------------------------------------------------
    @Test
    public void testUserIsStudent() throws Exception {
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        endInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: Company not owning the internship
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyNotOwningInternship() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(400);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        // DB => #555 => internshipId=999
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("ongoing");
        shi.setStartDate(new java.util.Date());
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company #400 owns e.g. #123 => mismatch
        ArrayList<Internship> owned = new ArrayList<>();
        Internship i123 = new Internship();
        i123.setId(123);
        owned.add(i123);
        doReturn(owned).when(internshipDAO).getCompanyInternships(400);

        endInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("you are not the company responsible"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: Internship not in "ongoing" or no start date => not correct period
    // -------------------------------------------------------------------------
    @Test
    public void testNotOngoingOrNoStartDate() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        // DB => #555 => e.g. "selectionProcess" or no start date
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("selectionProcess"); // Not "ongoing"
        shi.setStartDate(null);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company does own #999
        ArrayList<Internship> owned = new ArrayList<>();
        Internship i999 = new Internship();
        i999.setId(999);
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(300);

        endInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("Your not in the correct period to start the internship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: SQLException retrieving StudentHasInternship
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetStudentHasInternship() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        doThrow(new SQLException("DB error getStudentHasInternship"))
                .when(studentHasInternshipDAO).getStudentHasInternship(555);

        endInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error getStudentHasInternship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 8: SQLException retrieving companyInternships
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetCompanyInternships() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("ongoing");
        shi.setStartDate(new java.util.Date());
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        doThrow(new SQLException("DB error getCompanyInternships"))
                .when(internshipDAO).getCompanyInternships(300);

        endInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error getCompanyInternships"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: SQLException ending the internship
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionEndInternship() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("ongoing");
        shi.setStartDate(new java.util.Date());
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        ArrayList<Internship> owned = new ArrayList<>();
        Internship i999 = new Internship();
        i999.setId(999);
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(300);

        doThrow(new SQLException("DB error endInternship"))
                .when(studentHasInternshipDAO).endInternshipExperience(555);

        endInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error endInternship"));
    }
}
