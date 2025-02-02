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
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.StudentHasInternship;
import polimi.SE2.SC.beans.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ManageStudentApplicationTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private Connection testConnection;

    @Mock private StudentHasInternshipDAO studentHasInternshipDAO;
    @Mock private InternshipDAO internshipDAO;

    private ManageStudentApplication manageStudentApplicationServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        manageStudentApplicationServlet = spy(new ManageStudentApplication());

        ServletConfig servletConfig = testUtils.setUp();
        manageStudentApplicationServlet.init(servletConfig);

        Field connField = ManageStudentApplication.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connField.set(manageStudentApplicationServlet, testConnection);

        doReturn(studentHasInternshipDAO).when(manageStudentApplicationServlet).createStudentHasInternshipDAO();
        doReturn(internshipDAO).when(manageStudentApplicationServlet).createInternshipDAO();

        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Company approves a student application (evaluation => true)
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyApprovesEvaluation() throws Exception {
        // Company user
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // Provide studentHasInternshipId + evaluation
        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("evaluation")).thenReturn("true");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setApplicationStatus("evaluation");
        shi.setInternshipId(999);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        ArrayList<Internship> owned = new ArrayList<>();
        Internship i999 = new Internship();
        i999.setId(999);
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(300);

        // Approve the application
        doNothing().when(studentHasInternshipDAO).updateApplicationStatus(555, true);

        manageStudentApplicationServlet.doPost(request, response);

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
        when(request.getParameter("evaluation")).thenReturn("true");

        manageStudentApplicationServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("The id for the student-internship is null!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Invalid evaluation param
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidEvaluationParam() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        // e.g. "maybe"
        when(request.getParameter("evaluation")).thenReturn("maybe");

        manageStudentApplicationServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("Invalid evaluation parameter"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: StudentHasInternship not found
    // -------------------------------------------------------------------------
    @Test
    public void testNoStudentHasInternship() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("evaluation")).thenReturn("false");

        doReturn(null).when(studentHasInternshipDAO).getStudentHasInternship(555);

        manageStudentApplicationServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("There is no student-internship association"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: User is a student => unauthorized
    // -------------------------------------------------------------------------
    @Test
    public void testUserIsStudent() throws Exception {
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("evaluation")).thenReturn("true");

        manageStudentApplicationServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: Company not owning the internship
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyNotOwningInternship() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(500);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("evaluation")).thenReturn("false");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("evaluation");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company #500 doesn't own #999
        Internship i888 = new Internship();
        i888.setId(888);
        ArrayList<Internship> owned = new ArrayList<>();
        owned.add(i888);
        doReturn(owned).when(internshipDAO).getCompanyInternships(500);

        manageStudentApplicationServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("not the company responsible"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: Not "evaluation" or "selectionProcess" => can't manage
    // -------------------------------------------------------------------------
    @Test
    public void testWrongApplicationStatus() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(500);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("evaluation")).thenReturn("true");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("ongoing"); // not "evaluation" nor "selectionProcess"
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        ArrayList<Internship> owned = new ArrayList<>();
        Internship i999 = new Internship();
        i999.setId(999);
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(500);

        manageStudentApplicationServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("This is not the correct time to manage the application"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 8: "selectionProcess" with evaluation=true => not allowed
    // -------------------------------------------------------------------------
    @Test
    public void testSelectionProcessButEvaluationTrue() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(500);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("evaluation")).thenReturn("true"); // can't do that from selectionProcess

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        ArrayList<Internship> owned = new ArrayList<>();
        Internship i999 = new Internship();
        i999.setId(999);
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(500);

        manageStudentApplicationServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("From the selection process, you can only start"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: SQLException retrieving StudentHasInternship
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetStudentHasInternship() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(500);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("evaluation")).thenReturn("false");

        doThrow(new SQLException("DB error getStudentHasInternship"))
                .when(studentHasInternshipDAO).getStudentHasInternship(555);

        manageStudentApplicationServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error getStudentHasInternship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 10: SQLException retrieving company's internships
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetCompanyInternships() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(500);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("evaluation")).thenReturn("false");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("evaluation");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        doThrow(new SQLException("DB error getCompanyInternships"))
                .when(internshipDAO).getCompanyInternships(500);

        manageStudentApplicationServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error getCompanyInternships"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 11: SQLException updating application status
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionUpdateApplication() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(500);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("evaluation")).thenReturn("false");

        // DB => #555 => "evaluation"
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("evaluation");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        ArrayList<Internship> owned = new ArrayList<>();
        Internship i999 = new Internship();
        i999.setId(999);
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(500);

        doThrow(new SQLException("DB error updateApplicationStatus"))
                .when(studentHasInternshipDAO).updateApplicationStatus(555, false);

        manageStudentApplicationServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error updateApplicationStatus"));
    }
}
