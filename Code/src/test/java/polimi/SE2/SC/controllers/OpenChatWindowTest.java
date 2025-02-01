package polimi.SE2.SC.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

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
import com.google.gson.reflect.TypeToken;

import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.MessageDAO;
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Message;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OpenChatWindowTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;

    @Mock private MessageDAO messageDAO;
    @Mock private InternshipDAO internshipDAO;
    @Mock private Connection testConnection; // our mocked DB connection

    private OpenChatWindow openChatWindowServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create a spy of OpenChatWindow so we can override createMessageDAO() and createInternshipDAO().
        openChatWindowServlet = spy(new OpenChatWindow());

        // Use TestUtils to create a mocked ServletConfig (and ServletContext)
        ServletConfig servletConfig = testUtils.setUp();
        openChatWindowServlet.init(servletConfig);

        // Inject the mocked connection via reflection
        Field connectionField = OpenChatWindow.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(openChatWindowServlet, testConnection);

        // Override factory methods to return our mocks
        doReturn(messageDAO).when(openChatWindowServlet).createMessageDAO();
        doReturn(internshipDAO).when(openChatWindowServlet).createInternshipDAO();

        // Prepare a StringWriter to capture the servlet's output
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // When request.getSession() is called, return our mocked session
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Student user with valid otherAccountId => Should get existing chat
    // -------------------------------------------------------------------------
    @Test
    public void testStudentOpensChatSuccess() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // Provide otherAccountId
        when(request.getParameter("otherAccountId")).thenReturn("200");

        // messageDAO returns a non-null chat
        ArrayList<Message> existingChat = new ArrayList<>();
        Message msg = new Message();
        msg.setId(1);
        msg.setMessage("Hello");
        existingChat.add(msg);

        doReturn(existingChat).when(messageDAO).getAllChatMessages(100, 200);

        // Execute the servlet's doGet()
        openChatWindowServlet.doGet(request, response);

        // Expect SC_OK
        verify(response).setStatus(HttpServletResponse.SC_OK);

        // Parse JSON output
        String output = responseWriter.toString();
        assertFalse(output.isEmpty(), "Should contain JSON chat data");
        // Check that "chat" is present
        assertTrue(output.contains("chat"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Missing otherAccountId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingOtherAccountId() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // Provide null otherAccountId
        when(request.getParameter("otherAccountId")).thenReturn(null);

        openChatWindowServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("The id for the account you are trying to chat with is null"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Non-numeric otherAccountId
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidOtherAccountIdFormat() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // Provide "abc" => parse fails
        when(request.getParameter("otherAccountId")).thenReturn("abc");

        openChatWindowServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: Company user with missing internshipId
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyMissingInternshipId() throws Exception {
        // Company user
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // Provide otherAccountId but no internshipId
        when(request.getParameter("otherAccountId")).thenReturn("300");
        when(request.getParameter("internshipId")).thenReturn(null);

        openChatWindowServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("The id for the internship used to chat with the student is null!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: Company tries to use an internship it doesn't own
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyNotOwningInternship() throws Exception {
        // Company user
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("otherAccountId")).thenReturn("300");
        when(request.getParameter("internshipId")).thenReturn("999");

        // The DB says the company #50 only owns internship #1000
        ArrayList<Internship> ownedByCompany = new ArrayList<>();
        Internship i1000 = new Internship();
        i1000.setId(1000);
        ownedByCompany.add(i1000);

        doReturn(ownedByCompany).when(internshipDAO).getCompanyInternships(50);

        openChatWindowServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("You do not posses the internship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: SQLException retrieving company's internships
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetCompanyInternships() throws Exception {
        // Company user
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("otherAccountId")).thenReturn("300");
        when(request.getParameter("internshipId")).thenReturn("999");

        doThrow(new SQLException("DB error retrieving internships"))
                .when(internshipDAO).getCompanyInternships(50);

        openChatWindowServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String out = responseWriter.toString();
        assertTrue(out.contains("DB error retrieving internships"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: Chat does not exist (messageDAO returns null)
    // -------------------------------------------------------------------------
    @Test
    public void testNullChat() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // Provide otherAccountId
        when(request.getParameter("otherAccountId")).thenReturn("200");

        // DB returns null => chat not found
        doReturn(null).when(messageDAO).getAllChatMessages(100, 200);

        openChatWindowServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        String out = responseWriter.toString();
        assertTrue(out.contains("It is impossible to start a conversation from this page"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 8: SQLException retrieving chat
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetChat() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("otherAccountId")).thenReturn("200");

        // Throw exception
        doThrow(new SQLException("DB error retrieving chat"))
                .when(messageDAO).getAllChatMessages(100, 200);

        openChatWindowServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String out = responseWriter.toString();
        assertTrue(out.contains("DB error retrieving chat"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: Company user success => existing chat
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyOpensChatSuccess() throws Exception {
        // Company user
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("otherAccountId")).thenReturn("300");
        when(request.getParameter("internshipId")).thenReturn("999");

        // The company #50 owns internship #999
        ArrayList<Internship> owned = new ArrayList<>();
        Internship i999 = new Internship();
        i999.setId(999);
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(50);

        // The DB returns a valid chat
        ArrayList<Message> existingChat = new ArrayList<>();
        Message m1 = new Message();
        m1.setId(1);
        m1.setMessage("Hello from the internship!");
        existingChat.add(m1);

        doReturn(existingChat).when(messageDAO).getAllChatMessages(999, 300);

        openChatWindowServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);

        // Check JSON
        String output = responseWriter.toString();
        assertTrue(output.contains("chat"));
    }
}
