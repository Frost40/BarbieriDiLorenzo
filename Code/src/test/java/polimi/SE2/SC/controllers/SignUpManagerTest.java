package polimi.SE2.SC.controllers;

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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SignUpManagerTest {

    @Mock private StudentDAO studentDAO;
    @Mock private CompanyDAO companyDAO;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();
    private SignUpManager signUpManager;

    @Spy @InjectMocks private SignUpManager spySignUpManager;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        // Set up the servlet config using TestUtils
        ServletConfig servletConfig = testUtils.setUp();
        // Create the real instance of SignUpManager and initialize it
        signUpManager = new SignUpManager();
        signUpManager.init(servletConfig);
        // Spy on the real instance
        spySignUpManager = spy(signUpManager);
        // Override factory methods to return mocks
        doReturn(studentDAO).when(spySignUpManager).createStudentDAO();
        doReturn(companyDAO).when(spySignUpManager).createCompanyDAO();

        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    // ✅ Test 1: Successful Student Registration
    @Test
    public void testStudentRegistrationSuccess() throws Exception {
        when(request.getParameter("userType")).thenReturn("student");
        when(request.getParameter("email")).thenReturn("student@example.com");
        when(request.getParameter("password")).thenReturn("123");
        when(request.getParameter("repeat_pwd")).thenReturn("123");
        when(request.getParameter("name")).thenReturn("John");
        when(request.getParameter("surname")).thenReturn("Doe");
        when(request.getParameter("nationality")).thenReturn("English");
        when(request.getParameter("cityOfBirth")).thenReturn("London");

        when(studentDAO.getStudentByEmail("student@example.com")).thenReturn(null); // Email is unique
        when(studentDAO.registerStudent(anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(1);

        spySignUpManager.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    // ✅ Test 2: Successful Company Registration
    @Test
    public void testCompanyRegistrationSuccess() throws Exception {
        when(request.getParameter("userType")).thenReturn("company");
        when(request.getParameter("email")).thenReturn("company@example.com");
        when(request.getParameter("password")).thenReturn("securepass");
        when(request.getParameter("repeat_pwd")).thenReturn("securepass");
        when(request.getParameter("name")).thenReturn("Tech Corp");
        when(request.getParameter("mainOffice")).thenReturn("Main Office");
        when(request.getParameter("offices")).thenReturn("Office1,Office2");

        when(companyDAO.getCompanyByEmail("company@example.com")).thenReturn(null); // Email is unique
        when(companyDAO.registerCompany(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(2);

        spySignUpManager.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    // ✅ Test 3: Duplicate Email for Student
    @Test
    public void testStudentDuplicateEmail() throws Exception {
        when(request.getParameter("userType")).thenReturn("student");
        when(request.getParameter("email")).thenReturn("student@example.com");
        when(studentDAO.getStudentByEmail("student@example.com")).thenReturn(new Student());

        spySignUpManager.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    // ✅ Test 4: Duplicate Email for Company
    @Test
    public void testCompanyDuplicateEmail() throws Exception {
        when(request.getParameter("userType")).thenReturn("company");
        when(request.getParameter("email")).thenReturn("company@example.com");
        when(companyDAO.getCompanyByEmail("company@example.com")).thenReturn(new Company());

        spySignUpManager.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    // ✅ Test 5: Invalid Email Format
    @Test
    public void testInvalidEmailFormat() throws Exception {
        when(request.getParameter("userType")).thenReturn("company");
        when(request.getParameter("email")).thenReturn("invalid-email");

        spySignUpManager.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    // ✅ Test 6: Mismatched Passwords
    @Test
    public void testMismatchedPasswords() throws Exception {
        when(request.getParameter("userType")).thenReturn("company");
        when(request.getParameter("password")).thenReturn("123");
        when(request.getParameter("repeat_pwd")).thenReturn("321");

        spySignUpManager.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    // ✅ Test 7: Invalid Password Length
    @Test
    public void testInvalidPasswordLength() throws Exception {
        when(request.getParameter("userType")).thenReturn("company");
        when(request.getParameter("password")).thenReturn("");

        spySignUpManager.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    // ✅ Test 8: Missing Required Fields
    @Test
    public void testMissingFields() throws Exception {
        when(request.getParameter("userType")).thenReturn("company");
        when(request.getParameter("email")).thenReturn(null);
        when(request.getParameter("password")).thenReturn(null);

        spySignUpManager.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    // ✅ Test 9: Database Error Handling
    @Test
    public void testDatabaseError() throws Exception {
        when(request.getParameter("userType")).thenReturn("student");
        when(request.getParameter("email")).thenReturn("student@example.com");
        when(request.getParameter("password")).thenReturn("123");
        when(request.getParameter("repeat_pwd")).thenReturn("123");
        when(request.getParameter("name")).thenReturn("John");
        when(request.getParameter("surname")).thenReturn("Doe");
        when(request.getParameter("nationality")).thenReturn("English");
        when(request.getParameter("cityOfBirth")).thenReturn("London");

        when(studentDAO.getStudentByEmail("student@example.com")).thenThrow(new SQLException("Database error"));

        spySignUpManager.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
