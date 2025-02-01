package polimi.SE2.SC.controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UploadNewInternshipTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;

    @Mock
    private InternshipDAO internshipDAO;
    @Mock
    private Connection testConnection; // our dummy/mocked connection

    // We will create the spy manually for UploadNewInternship.
    private UploadNewInternship uploadNewInternshipServlet;

    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create a spy manually.
        uploadNewInternshipServlet = spy(new UploadNewInternship());

        // Use TestUtils to create a mocked ServletConfig (and ServletContext).
        ServletConfig servletConfig = testUtils.setUp();
        uploadNewInternshipServlet.init(servletConfig);

        // Override the connection field with our mocked connection using reflection.
        Field connectionField = UploadNewInternship.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(uploadNewInternshipServlet, testConnection);

        // Override the factory method to return our mocked InternshipDAO.
        doReturn(internshipDAO).when(uploadNewInternshipServlet).createInternshipDAO();

        // Prepare a StringWriter to capture the servlet’s output.
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // When request.getSession() is called, return our mocked session.
        when(request.getSession()).thenReturn(session);
    }

    // ★ Test 1: Successful Internship Upload (for a company user)
    @Test
    public void testSuccessfulUpload() throws Exception {
        // Provide valid parameters.
        String validName = "Internship Name";
        String validDescription = "This is a valid description for the internship.";
        when(request.getParameter("name")).thenReturn(validName);
        when(request.getParameter("description")).thenReturn(validDescription);

        // Simulate a logged-in company (not a Student).
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(101);
        when(session.getAttribute("currentUser")).thenReturn(company);

        // Simulate no exceptions when uploading the internship.
        // For uploadNewInternship, no return value is needed.
        doNothing().when(internshipDAO).uploadNewInternship(validName, validDescription, 101);

        // Prepare a dummy list of internships to be returned.
        ArrayList<Internship> internships = new ArrayList<>();
        Internship internship1 = new Internship();
        internship1.setId(1);
        internship1.setName(validName);
        internship1.setDescription(validDescription);
        internship1.setRating(4.5);
        internship1.setCompanyId(101);
        internships.add(internship1);

        when(internshipDAO.getCompanyInternships(101)).thenReturn(internships);

        // Execute the servlet.
        uploadNewInternshipServlet.doPost(request, response);

        // Verify that a 200 OK is set.
        verify(response).setStatus(HttpServletResponse.SC_OK);
        // Verify the content type and encoding (optional).
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");

        // Check that the JSON output contains the internship data.
        String jsonOutput = responseWriter.toString();
        Gson gson = new GsonBuilder().create();
        @SuppressWarnings("unchecked")
        Map<String, Object> result = gson.fromJson(jsonOutput, HashMap.class);
        assertTrue(result.containsKey("internships"));
    }

    // ★ Test 2: Invalid Name (e.g., null or empty or too long)
    @Test
    public void testInvalidName() throws Exception {
        // Provide invalid name (null).
        when(request.getParameter("name")).thenReturn(null);
        when(request.getParameter("description")).thenReturn("Valid description");

        // Simulate a company user.
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(101);
        when(session.getAttribute("currentUser")).thenReturn(company);

        uploadNewInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String output = responseWriter.toString();
        assertTrue(output.contains("Invalid name"));
    }

    // ★ Test 3: Invalid Description (e.g., null or too long)
    @Test
    public void testInvalidDescription() throws Exception {
        // Provide a valid name but invalid description.
        when(request.getParameter("name")).thenReturn("Valid Name");
        when(request.getParameter("description")).thenReturn("");

        // Simulate a company user.
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(101);
        when(session.getAttribute("currentUser")).thenReturn(company);

        uploadNewInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String output = responseWriter.toString();
        assertTrue(output.contains("Invalid description"));
    }

    // ★ Test 4: Upload Attempt by a Student (unauthorized)
    @Test
    public void testUploadByStudent() throws Exception {
        // Provide valid parameters.
        when(request.getParameter("name")).thenReturn("Valid Internship");
        when(request.getParameter("description")).thenReturn("Valid description");

        // Simulate a student user.
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(202);
        when(session.getAttribute("currentUser")).thenReturn(student);

        uploadNewInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        String output = responseWriter.toString();
        assertTrue(output.contains("No internship can be upload by a student"));
    }

    // ★ Test 5: SQLException Thrown When Uploading the Internship
    @Test
    public void testSQLExceptionInUploadNewInternship() throws Exception {
        // Provide valid parameters.
        String validName = "Internship Name";
        String validDescription = "Valid description";
        when(request.getParameter("name")).thenReturn(validName);
        when(request.getParameter("description")).thenReturn(validDescription);

        // Simulate a company user.
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(101);
        when(session.getAttribute("currentUser")).thenReturn(company);

        // Simulate an exception when uploading the internship.
        doThrow(new SQLException("DB upload error"))
                .when(internshipDAO).uploadNewInternship(validName, validDescription, 101);

        uploadNewInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String output = responseWriter.toString();
        assertTrue(output.contains("DB upload error"));
    }

    // ★ Test 6: SQLException Thrown When Retrieving Company Internships
    @Test
    public void testSQLExceptionInGetCompanyInternships() throws Exception {
        // Provide valid parameters.
        String validName = "Internship Name";
        String validDescription = "Valid description";
        when(request.getParameter("name")).thenReturn(validName);
        when(request.getParameter("description")).thenReturn(validDescription);

        // Simulate a company user.
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(101);
        when(session.getAttribute("currentUser")).thenReturn(company);

        // Simulate no exception during upload.
        doNothing().when(internshipDAO).uploadNewInternship(validName, validDescription, 101);

        // Simulate an exception when retrieving internships.
        when(internshipDAO.getCompanyInternships(101)).thenThrow(new SQLException("DB retrieval error"));

        uploadNewInternshipServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String output = responseWriter.toString();
        assertTrue(output.contains("DB retrieval error"));
    }
}
