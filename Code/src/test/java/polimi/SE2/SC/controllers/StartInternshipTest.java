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

import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StartInternshipTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private Connection testConnection;

    @Mock private StudentHasInternshipDAO studentHasInternshipDAO;
    @Mock private InternshipDAO internshipDAO;

    private StartInternship startInternshipServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        startInternshipServlet = spy(new StartInternship());

        ServletConfig servletConfig = testUtils.setUp();
        startInternshipServlet.init(servletConfig);

        // Reflection injection
        Field connField = StartInternship.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connField.set(startInternshipServlet, testConnection);

        doReturn(studentHasInternshipDAO).when(startInternshipServlet).createStudentHasInternshipDAO();
        doReturn(internshipDAO).when(startInternshipServlet).createInternshipDAO();

        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Company starts internship (applicationStatus="selectionProcess" & no end date)
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyStartsInternshipSuccess() throws Exception {
        // Company
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setApplicationStatus("selectionProcess");
        shi.setInternshipId(999);
        shi.setEndDate(null);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company owns #999
        ArrayList<Internship> owned = new ArrayList<>();
        Internship i999 = new Internship();
        i999.setId(999);
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(300);

        // DB success
        doNothing().when(studentHasInternshipDAO).startInternshipExperience(555);

        startInternshipServlet.doPost(request, response);

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

        when(request.getParameter("studentHasInternshipId")).thenReturn(null);

        startInternshipServlet.doPost(request, response);

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

        startInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("No student-internship association found"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: User is student => unauthorized
    // -------------------------------------------------------------------------
    @Test
    public void testUserIsStudent() throws Exception {
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        startInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: Company not owning the internship
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyNotOwningInternship() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company #300 owns #888, not #999
        ArrayList<Internship> owned = new ArrayList<>();
        Internship i888 = new Internship();
        i888.setId(888);
        owned.add(i888);
        doReturn(owned).when(internshipDAO).getCompanyInternships(300);

        startInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("you are not the company responsible"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: Not the correct time => e.g. "ongoing" or endDate != null
    // -------------------------------------------------------------------------
    @Test
    public void testNotCorrectTime() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("ongoing"); // not "selectionProcess"
        shi.setEndDate(null);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company does own #999
        ArrayList<Internship> owned = new ArrayList<>();
        Internship i999 = new Internship();
        i999.setId(999);
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(300);

        startInternshipServlet.doPost(request, response);

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

        startInternshipServlet.doPost(request, response);

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
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        doThrow(new SQLException("DB error getCompanyInternships"))
                .when(internshipDAO).getCompanyInternships(300);

        startInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error getCompanyInternships"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: SQLException starting internship
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionStartInternship() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        ArrayList<Internship> owned = new ArrayList<>();
        Internship i999 = new Internship();
        i999.setId(999);
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(300);

        doThrow(new SQLException("DB error startInternship"))
                .when(studentHasInternshipDAO).startInternshipExperience(555);

        startInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error startInternship"));
    }
}
