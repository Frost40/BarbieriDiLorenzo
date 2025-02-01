package polimi.SE2.SC.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ModifyInternshipTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;

    @Mock
    private InternshipDAO internshipDAO;
    @Mock
    private Connection testConnection;

    private ModifyInternship modifyInternshipServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create a spy of ModifyInternship so we can override createInternshipDAO().
        modifyInternshipServlet = spy(new ModifyInternship());

        // Use TestUtils to create a mocked ServletConfig and ServletContext.
        ServletConfig servletConfig = testUtils.setUp();
        modifyInternshipServlet.init(servletConfig);

        // Inject the mocked connection into the servlet's private 'connection' field.
        Field connectionField = ModifyInternship.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(modifyInternshipServlet, testConnection);

        // Override the factory method to return our mocked InternshipDAO.
        doReturn(internshipDAO).when(modifyInternshipServlet).createInternshipDAO();

        // Prepare a StringWriter to capture servlet output.
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // When request.getSession() is called, return our mocked session.
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Successful modification by a Company user
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyModifiesInternshipSuccess() throws Exception {
        // Simulate a company user
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(company);

        // Provide valid params
        when(request.getParameter("internshipId")).thenReturn("500");
        when(request.getParameter("name")).thenReturn("Updated Internship");
        when(request.getParameter("description")).thenReturn("Updated description here");

        // The DB has an internship with id=500 belonging to company #200
        Internship internship = new Internship();
        internship.setId(500);
        internship.setCompanyId(200);
        internship.setName("Old Name");
        internship.setDescription("Old Desc");

        doReturn(internship).when(internshipDAO).getInternshipById(500);

        // Stub DAO modifyInternship
        doNothing().when(internshipDAO).modifyInternship(eq(500), anyString(), anyString());

        // Execute servlet
        modifyInternshipServlet.doPost(request, response);

        // Verify success
        verify(response).setStatus(HttpServletResponse.SC_OK);

        // Parse JSON
        String output = responseWriter.toString();
        Map<String, Object> result = new Gson().fromJson(output, new TypeToken<HashMap<String, Object>>(){}.getType());
        assertTrue(result.containsKey("modifiedInternship"), "JSON should contain 'modifiedInternship'");

        Map<String, Object> internshipJson = (Map<String, Object>) result.get("modifiedInternship");
        assertEquals("Updated Internship", internshipJson.get("name"));
        assertEquals("Updated description here", internshipJson.get("description"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Invalid name (empty or too long)
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidName() throws Exception {
        Company company = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(company);

        // Provide invalid name
        when(request.getParameter("internshipId")).thenReturn("500");
        when(request.getParameter("name")).thenReturn(""); // invalid name
        when(request.getParameter("description")).thenReturn("Valid description");

        modifyInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("Invalid name"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Invalid description
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidDescription() throws Exception {
        Company company = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(company);

        // Provide invalid description (empty)
        when(request.getParameter("internshipId")).thenReturn("500");
        when(request.getParameter("name")).thenReturn("Valid name");
        when(request.getParameter("description")).thenReturn("");

        modifyInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("Invalid description"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: Student tries to modify internship (Unauthorized)
    // -------------------------------------------------------------------------
    @Test
    public void testStudentModifiesInternship() throws Exception {
        Student student = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(student);

        when(request.getParameter("internshipId")).thenReturn("500");
        when(request.getParameter("name")).thenReturn("Name");
        when(request.getParameter("description")).thenReturn("Desc");

        modifyInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("No internship can be modified by a student"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: Missing or invalid internshipId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingInternshipId() throws Exception {
        Company company = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(company);

        when(request.getParameter("internshipId")).thenReturn(null); // missing
        when(request.getParameter("name")).thenReturn("Name");
        when(request.getParameter("description")).thenReturn("Desc");

        modifyInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: Internship not found in DB
    // -------------------------------------------------------------------------
    @Test
    public void testInternshipNotFound() throws Exception {
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(company);

        when(request.getParameter("internshipId")).thenReturn("500");
        when(request.getParameter("name")).thenReturn("Name");
        when(request.getParameter("description")).thenReturn("Desc");

        // The DB returns null
        doReturn(null).when(internshipDAO).getInternshipById(500);

        modifyInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("The internship you are trying to modify does not exists"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: Internship belongs to a different company
    // -------------------------------------------------------------------------
    @Test
    public void testDifferentOwner() throws Exception {
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(company);

        when(request.getParameter("internshipId")).thenReturn("600");
        when(request.getParameter("name")).thenReturn("NewName");
        when(request.getParameter("description")).thenReturn("NewDesc");

        Internship internship = new Internship();
        internship.setId(600);
        internship.setCompanyId(999); // belongs to someone else
        doReturn(internship).when(internshipDAO).getInternshipById(600);

        modifyInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("You can only modify your own internship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 8: SQLException during modifyInternship
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionModify() throws Exception {
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(company);

        when(request.getParameter("internshipId")).thenReturn("500");
        when(request.getParameter("name")).thenReturn("Updated Name");
        when(request.getParameter("description")).thenReturn("Updated Desc");

        // The DB has an internship #500 belonging to company #200
        Internship internship = new Internship();
        internship.setId(500);
        internship.setCompanyId(200);
        doReturn(internship).when(internshipDAO).getInternshipById(500);

        doThrow(new SQLException("DB modifyInternship error"))
                .when(internshipDAO).modifyInternship(eq(500), anyString(), anyString());

        modifyInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB modifyInternship error"));
    }
}
