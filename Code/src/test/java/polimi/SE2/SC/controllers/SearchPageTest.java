package polimi.SE2.SC.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
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

import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SearchPageTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;

    @Mock private InternshipDAO internshipDAO;
    @Mock private Connection testConnection;

    private SearchPageManager searchPageManagerServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Spy the servlet so we can override DAO creation
        searchPageManagerServlet = spy(new SearchPageManager());

        // Use TestUtils to provide a mocked ServletConfig, ServletContext, etc.
        ServletConfig servletConfig = testUtils.setUp();
        searchPageManagerServlet.init(servletConfig);

        // Inject the mocked connection via reflection
        Field connectionField = SearchPageManager.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(searchPageManagerServlet, testConnection);

        // Override createInternshipDAO() to return our mocked internshipDAO
        doReturn(internshipDAO).when(searchPageManagerServlet).createInternshipDAO();

        // Prepare a StringWriter to capture servlet output
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // When request.getSession() is called, return our mocked session
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Student user => success => returns all internships
    // -------------------------------------------------------------------------
    @Test
    public void testStudentSeesAllInternshipsInDepth() throws Exception {
        // 1. Mock a Student user
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // 2. Suppose the DB has some internships
        ArrayList<Internship> allInternships = new ArrayList<>();
        Internship i1 = new Internship();
        i1.setId(1);
        i1.setName("Internship A");
        i1.setDescription("A great internship");
        i1.setRating(4.5);
        i1.setCompanyId(123);
        allInternships.add(i1);

        Internship i2 = new Internship();
        i2.setId(2);
        i2.setName("Internship B");
        i2.setDescription("Another great internship");
        i2.setRating(3.7);
        i2.setCompanyId(456);
        allInternships.add(i2);

        // 3. Stub the DAO call
        doReturn(allInternships).when(internshipDAO).getAllInternships();

        // 4. Execute the GET request
        searchPageManagerServlet.doGet(request, response);

        // 5. Expect HTTP 200 OK
        verify(response).setStatus(HttpServletResponse.SC_OK);

        // 6. Capture and parse the JSON output
        String output = responseWriter.toString();
        assertFalse(output.isEmpty(), "Response should contain JSON data");

        // 6a. Deserialize JSON into a Map<String, List<Internship>>
        TypeToken<Map<String, ArrayList<Internship>>> token =
                new TypeToken<Map<String, ArrayList<Internship>>>() {};
        Map<String, ArrayList<Internship>> parsedJson = new Gson().fromJson(output, token.getType());

        // 7. Verify the key "allInternships" exists
        assertTrue(parsedJson.containsKey("allInternships"), "JSON should contain 'allInternships' key");

        // 8. Extract the list of internships
        ArrayList<Internship> returnedInternships = parsedJson.get("allInternships");
        assertNotNull(returnedInternships, "allInternships should not be null");
        assertEquals(2, returnedInternships.size(), "Should return 2 internships");

        // 9. Check each internship’s fields in detail
        Internship returnedI1 = returnedInternships.get(0);
        assertEquals(1, returnedI1.getId());
        assertEquals("Internship A", returnedI1.getName());
        assertEquals("A great internship", returnedI1.getDescription());
        assertEquals(4.5, returnedI1.getRating());
        assertEquals(123, returnedI1.getCompanyId());

        Internship returnedI2 = returnedInternships.get(1);
        assertEquals(2, returnedI2.getId());
        assertEquals("Internship B", returnedI2.getName());
        assertEquals("Another great internship", returnedI2.getDescription());
        assertEquals(3.7, returnedI2.getRating());
        assertEquals(456, returnedI2.getCompanyId());
    }


    // -------------------------------------------------------------------------
    // ★ Test 2: Company user => unauthorized
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyUserUnauthorized() throws Exception {
        // Mock a Company user
        Company companyUser = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // Call doGet
        searchPageManagerServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        String output = responseWriter.toString();
        assertTrue(output.contains("Company accounts are not authorize to view the Search Page"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: SQLException retrieving internships
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetAllInternships() throws Exception {
        // Mock a Student user
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        doThrow(new SQLException("DB error retrieving internships"))
                .when(internshipDAO).getAllInternships();

        // Execute
        searchPageManagerServlet.doGet(request, response);

        // Expect internal server error
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String out = responseWriter.toString();
        assertTrue(out.contains("DB error retrieving internships"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: Edge case => currentUser is null (no session user)
    // -------------------------------------------------------------------------
    @Test
    public void testNoUserInSession() throws Exception {
        when(session.getAttribute("currentUser")).thenReturn(null);

        // The servlet doesn't have an explicit check for user==null
        // It only checks for 'instanceof Company'
        // So we treat user==null as a Student for now, meaning it won't be unauthorized.

        ArrayList<Internship> allInternships = new ArrayList<>();
        doReturn(allInternships).when(internshipDAO).getAllInternships();

        searchPageManagerServlet.doGet(request, response);

        // The code doesn't handle a null user specially. So it will proceed
        // => SC_OK
        verify(response).setStatus(HttpServletResponse.SC_OK);
    }
}
