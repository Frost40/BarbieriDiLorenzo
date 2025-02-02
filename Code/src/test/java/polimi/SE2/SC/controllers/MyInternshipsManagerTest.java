package polimi.SE2.SC.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.StudentHasInternship;
import polimi.SE2.SC.beans.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MyInternshipsManagerTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Mock
    private Connection testConnection;

    @Mock
    private StudentHasInternshipDAO studentHasInternshipDAO;

    private MyInternshipsManager myInternshipsManagerServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Spy the servlet
        myInternshipsManagerServlet = spy(new MyInternshipsManager());

        // Use TestUtils for mocked ServletConfig/Context
        ServletConfig servletConfig = testUtils.setUp();
        myInternshipsManagerServlet.init(servletConfig);

        // Inject the mocked connection via reflection
        Field connField = MyInternshipsManager.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connField.set(myInternshipsManagerServlet, testConnection);

        // Override DAO creation
        doReturn(studentHasInternshipDAO).when(myInternshipsManagerServlet).createStudentHasInternshipDAO();

        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // Default: session is mocked
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Student user => getAllStudentInternships => success
    // -------------------------------------------------------------------------
    @Test
    public void testStudentSuccess() throws Exception {
        // Student
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // Suppose DB returns some HashMap<Internship, StudentHasInternship>
        HashMap<Internship, StudentHasInternship> studentInternships = new HashMap<>();

        Internship i1 = new Internship();
        i1.setId(999);
        StudentHasInternship shi1 = new StudentHasInternship();
        shi1.setId(555);
        studentInternships.put(i1, shi1);

        doReturn(studentInternships).when(studentHasInternshipDAO).getAllStudentInternships(100);

        myInternshipsManagerServlet.doGet(request, response);

        // Expect SC_OK
        verify(response).setStatus(HttpServletResponse.SC_OK);
        String out = responseWriter.toString();
        assertTrue(out.contains("\"data\""), "JSON should contain 'data'");
        // We expect a flattened structure in "data"
        assertTrue(out.contains("\"internship\""), "Should contain 'internship'");
        assertTrue(out.contains("\"studentHasInternship\""), "Should contain 'studentHasInternship'");
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Company user => getCompanyOngoingInternships => success
    // -------------------------------------------------------------------------
    @Test
    public void testCompanySuccess() throws Exception {
        // Company
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // DB returns HashMap<Internship, HashMap<Student, StudentHasInternship>>
        HashMap<Internship, HashMap<Student, StudentHasInternship>> companyData = new HashMap<>();

        Internship i2 = new Internship();
        i2.setId(888);
        HashMap<Student, StudentHasInternship> innerMap = new HashMap<>();

        Student st = new Student();
        st.setId(555);
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(666);

        innerMap.put(st, shi);
        companyData.put(i2, innerMap);

        doReturn(companyData).when(studentHasInternshipDAO).getCompanyOngoingInternships(200);

        myInternshipsManagerServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String out = responseWriter.toString();
        // We expect 'data' => array => each object has "internship" + "students"
        assertTrue(out.contains("\"data\""));
        assertTrue(out.contains("\"internship\""));
        assertTrue(out.contains("\"students\""));
        assertTrue(out.contains("\"student\""));
        assertTrue(out.contains("\"studentHasInternship\""));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: DB error for Student => SC_INTERNAL_SERVER_ERROR
    // -------------------------------------------------------------------------
    @Test
    public void testStudentDbError() throws Exception {
        // Student
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // DB throws exception
        doThrow(new SQLException("DB error getAllStudentInternships"))
                .when(studentHasInternshipDAO).getAllStudentInternships(100);

        myInternshipsManagerServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error getAllStudentInternships"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: DB error for Company => SC_INTERNAL_SERVER_ERROR
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyDbError() throws Exception {
        // Company
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        doThrow(new SQLException("DB error getCompanyOngoingInternships"))
                .when(studentHasInternshipDAO).getCompanyOngoingInternships(200);

        myInternshipsManagerServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error getCompanyOngoingInternships"));
    }

}