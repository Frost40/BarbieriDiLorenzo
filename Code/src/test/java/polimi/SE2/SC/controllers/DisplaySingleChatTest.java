package polimi.SE2.SC.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

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
import polimi.SE2.SC.DAOs.MessageDAO;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Message;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DisplaySingleChatTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;

    @Mock private InternshipDAO internshipDAO;
    @Mock private MessageDAO messageDAO;
    @Mock private Connection testConnection;

    private DisplaySingleChat displaySingleChatServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create a spy for DisplaySingleChat
        displaySingleChatServlet = spy(new DisplaySingleChat());

        // Use TestUtils to get a mocked ServletConfig/Context
        ServletConfig servletConfig = testUtils.setUp();
        displaySingleChatServlet.init(servletConfig);

        // Inject mocked connection via reflection
        Field connectionField = DisplaySingleChat.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(displaySingleChatServlet, testConnection);

        // Override the factory methods to return mocks
        doReturn(internshipDAO).when(displaySingleChatServlet).createInternshipDAO();
        doReturn(messageDAO).when(displaySingleChatServlet).createMessageDAO();

        // Prepare a StringWriter for capturing output
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // When getSession() is called, return the mocked session
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Student sees an existing chat with an internship
    // -------------------------------------------------------------------------
    @Test
    public void testStudentDisplaysChatSuccess() throws Exception {
        // Mock user as a Student
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // Provide valid internshipId
        when(request.getParameter("internshipId")).thenReturn("500");

        // DB has an internship #500
        Internship internship = new Internship();
        internship.setId(500);
        doReturn(internship).when(internshipDAO).getInternshipById(500);

        // DB returns chat messages
        ArrayList<Message> existingChat = new ArrayList<>();
        Message msg1 = new Message();
        msg1.setId(1);
        msg1.setMessage("Hello from your internship!");
        existingChat.add(msg1);
        doReturn(existingChat).when(messageDAO).getAllChatMessages(100, 500);

        // Execute doGet
        displaySingleChatServlet.doGet(request, response);

        // Verify success
        verify(response).setStatus(HttpServletResponse.SC_OK);

        // JSON check
        String output = responseWriter.toString();
        assertTrue(output.contains("\"chat\""));
        assertTrue(output.contains("Hello from your internship!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Missing internshipId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingInternshipId() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // internshipId param is missing
        when(request.getParameter("internshipId")).thenReturn(null);

        displaySingleChatServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String out = responseWriter.toString();
        assertTrue(out.contains("Null complaint id!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Non-numeric internshipId
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidInternshipIdFormat() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // internshipId = "abc"
        when(request.getParameter("internshipId")).thenReturn("abc");

        displaySingleChatServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: Internship not found in DB
    // -------------------------------------------------------------------------
    @Test
    public void testInternshipNotFound() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("internshipId")).thenReturn("500");

        // DB returns null
        doReturn(null).when(internshipDAO).getInternshipById(500);

        displaySingleChatServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("The internship you are trying to chat with does not exists"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: SQLException retrieving internship
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetInternship() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("internshipId")).thenReturn("500");

        doThrow(new SQLException("DB error retrieving internship"))
                .when(internshipDAO).getInternshipById(500);

        displaySingleChatServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error retrieving internship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: SQLException retrieving chat
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetChat() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("internshipId")).thenReturn("500");

        // DB has internship #500
        Internship internship = new Internship();
        internship.setId(500);
        doReturn(internship).when(internshipDAO).getInternshipById(500);

        // Throw exception on getAllChatMessages
        doThrow(new SQLException("DB error retrieving chat"))
                .when(messageDAO).getAllChatMessages(100, 500);

        displaySingleChatServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error retrieving chat"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: Student sees an empty chat (or null)
    // -------------------------------------------------------------------------
    @Test
    public void testEmptyChat() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("internshipId")).thenReturn("500");

        // DB has internship #500
        Internship internship = new Internship();
        internship.setId(500);
        doReturn(internship).when(internshipDAO).getInternshipById(500);

        // DB returns an empty list or possibly null
        ArrayList<Message> emptyChat = new ArrayList<>();
        doReturn(emptyChat).when(messageDAO).getAllChatMessages(100, 500);

        displaySingleChatServlet.doGet(request, response);

        // We see in the code that it doesn't do a special check for empty chat
        // => it still returns 200 with the empty chat array in JSON
        verify(response).setStatus(HttpServletResponse.SC_OK);

        String out = responseWriter.toString();
        assertTrue(out.contains("\"chat\":[]"), "Should contain an empty chat array in JSON");
    }
}
