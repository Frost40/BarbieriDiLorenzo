package polimi.SE2.SC.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import polimi.SE2.SC.DAOs.CompanyDAO;
import polimi.SE2.SC.DAOs.StudentDAO;
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.TestUtils;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LoginManagerTest {

    @Mock private StudentDAO studentDAO;
    @Mock private CompanyDAO companyDAO;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private Connection connection; // For completeness, though our servlet uses its own connection

    // Create a real instance of LoginManager and then spy on it
    private LoginManager loginManager;
    @Spy @InjectMocks private LoginManager spyLoginManager;

    private StringWriter responseWriter;
    private Gson gson;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        // Set up the servlet config using TestUtils
        ServletConfig servletConfig = testUtils.setUp();
        // Create the real instance of LoginManager and initialize it
        loginManager = new LoginManager();
        loginManager.init(servletConfig);
        // Create a spy on the real instance so that we can override DAO creation
        spyLoginManager = spy(loginManager);
        // Override factory methods to return our mocks
        doReturn(studentDAO).when(spyLoginManager).createStudentDAO();
        doReturn(companyDAO).when(spyLoginManager).createCompanyDAO();

        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        when(request.getSession()).thenReturn(session);

        gson = new GsonBuilder().create();
    }

    // Test 1: Successful Student Login
    @Test
    public void testStudentLoginSuccess() throws Exception {
        when(request.getParameter("id")).thenReturn("student@example.com");
        when(request.getParameter("password")).thenReturn("123");

        Student expectedStudent = new Student();
        expectedStudent.setId(1);
        expectedStudent.setEmail("student@example.com");
        expectedStudent.setPassword("123");
        expectedStudent.setName("John Doe");
        // You can also set surname, nationality, cityOfBirth if needed

        when(studentDAO.findStudentWithCredentials("student@example.com", "123"))
                .thenReturn(expectedStudent);

        spyLoginManager.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String jsonResponse = responseWriter.toString().trim();
        Student actual = gson.fromJson(jsonResponse, Student.class);
        assertEquals(expectedStudent.getId(), actual.getId());
        assertEquals(expectedStudent.getEmail(), actual.getEmail());
        assertEquals(expectedStudent.getPassword(), actual.getPassword());
        assertEquals(expectedStudent.getName(), actual.getName());
    }

    // Test 2: Successful Company Login
    @Test
    public void testCompanyLoginSuccess() throws Exception {
        when(request.getParameter("id")).thenReturn("company@example.com");
        when(request.getParameter("password")).thenReturn("securepass");

        Company expectedCompany = new Company();
        expectedCompany.setId(6); // Adjust this if necessary based on test data
        expectedCompany.setEmail("company@example.com");
        expectedCompany.setPassword("securepass");
        expectedCompany.setName("Tech Corp");
        expectedCompany.setMainOffice("Main Office");
        expectedCompany.setOffices(new java.util.ArrayList<>());
        expectedCompany.getOffices().add("Office1");
        expectedCompany.getOffices().add("Office2");

        // Simulate student authentication failure so that company DAO is used
        when(studentDAO.findStudentWithCredentials("company@example.com", "securepass"))
                .thenReturn(null);
        when(companyDAO.findCompanyWithCredential("company@example.com", "securepass"))
                .thenReturn(expectedCompany);

        spyLoginManager.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String jsonResponse = responseWriter.toString().trim();
        Company actual = gson.fromJson(jsonResponse, Company.class);
        assertEquals(expectedCompany.getId(), actual.getId());
        assertEquals(expectedCompany.getEmail(), actual.getEmail());
        assertEquals(expectedCompany.getPassword(), actual.getPassword());
        assertEquals(expectedCompany.getName(), actual.getName());
        assertEquals(expectedCompany.getMainOffice(), actual.getMainOffice());
        assertArrayEquals(expectedCompany.getOffices().toArray(), actual.getOffices().toArray());
    }

    // Test 3: Invalid Email Format
    @Test
    public void testInvalidEmailFormat() throws Exception {
        when(request.getParameter("id")).thenReturn("invalid-email");
        when(request.getParameter("password")).thenReturn("password");

        spyLoginManager.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String output = responseWriter.toString();
        assertTrue(output.contains("Invalid email address!"));
    }

    // Test 4: Missing Credentials
    @Test
    public void testMissingCredentials() throws Exception {
        when(request.getParameter("id")).thenReturn(null);
        when(request.getParameter("password")).thenReturn(null);

        spyLoginManager.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String output = responseWriter.toString();
        assertTrue(output.contains("Some data is missing from the login module!"));
    }

    // Test 5: Incorrect Email/Password
    @Test
    public void testIncorrectCredentials() throws Exception {
        when(request.getParameter("id")).thenReturn("wrong@example.com");
        when(request.getParameter("password")).thenReturn("wrongpass");

        when(studentDAO.findStudentWithCredentials("wrong@example.com", "wrongpass")).thenReturn(null);
        when(companyDAO.findCompanyWithCredential("wrong@example.com", "wrongpass")).thenReturn(null);

        spyLoginManager.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        String output = responseWriter.toString();
        assertTrue(output.contains("Email or password are incorrect"));
    }

    // Test 6: Database Error Simulation
    @Test
    public void testDatabaseError() throws Exception {
        when(request.getParameter("id")).thenReturn("student@example.com");
        when(request.getParameter("password")).thenReturn("123");

        when(studentDAO.findStudentWithCredentials("student@example.com", "123"))
                .thenThrow(new SQLException("Database error"));

        spyLoginManager.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String output = responseWriter.toString();
        assertTrue(output.contains("Database error"));
    }

    // Extended Test Cases

    // Test 7: Password Too Long (> 10 characters)
    @Test
    public void testPasswordTooLong() throws Exception {
        when(request.getParameter("id")).thenReturn("student@example.com");
        when(request.getParameter("password")).thenReturn("thispasswordistoolong");

        spyLoginManager.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String output = responseWriter.toString();
        assertTrue(output.contains("Invalid password"));
    }

    // Test 8: Empty Password
    @Test
    public void testEmptyPassword() throws Exception {
        when(request.getParameter("id")).thenReturn("student@example.com");
        when(request.getParameter("password")).thenReturn("");

        spyLoginManager.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String output = responseWriter.toString();
        assertTrue(output.contains("Invalid password"));
    }

    // Test 9: Email Too Long (> 100 characters)
    @Test
    public void testEmailTooLong() throws Exception {
        String longEmail = "a".repeat(101) + "@example.com";
        when(request.getParameter("id")).thenReturn(longEmail);
        when(request.getParameter("password")).thenReturn("password123");

        spyLoginManager.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String output = responseWriter.toString();
        assertTrue(output.contains("Invalid email address!"));
    }

    // Test 10: Empty Email
    @Test
    public void testEmptyEmail() throws Exception {
        when(request.getParameter("id")).thenReturn("");
        when(request.getParameter("password")).thenReturn("password123");

        spyLoginManager.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String output = responseWriter.toString();
        assertTrue(output.contains("Invalid email address!"));
    }
}
