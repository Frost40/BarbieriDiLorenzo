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

import com.google.gson.Gson;
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
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Message;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReadMessageTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;

    @Mock private MessageDAO messageDAO;
    @Mock private InternshipDAO internshipDAO;
    @Mock private Connection testConnection; // mocked DB connection

    private ReadMessage readMessageServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create a spy for the ReadMessage servlet
        readMessageServlet = spy(new ReadMessage());

        // Use TestUtils to create a mocked ServletConfig (and ServletContext)
        ServletConfig servletConfig = testUtils.setUp();
        readMessageServlet.init(servletConfig);

        // Inject the mocked connection into the private 'connection' field via reflection
        Field connectionField = ReadMessage.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(readMessageServlet, testConnection);

        // Override the factory methods to return our mocks
        doReturn(messageDAO).when(readMessageServlet).createMessageDAO();
        doReturn(internshipDAO).when(readMessageServlet).createInternshipDAO();

        // Prepare a StringWriter to capture servlet output
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // Mock session retrieval
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Student successfully reads their own message
    // -------------------------------------------------------------------------
    @Test
    public void testStudentReadsOwnMessage() throws Exception {
        // Simulate a Student user
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(student);

        // Provide valid messageId
        when(request.getParameter("messageId")).thenReturn("999");

        // The message with ID=999 exists, and its receiver is the student #100
        Message msg = new Message();
        msg.setId(999);
        msg.setReceiverId(100);
        doReturn(msg).when(messageDAO).getMessage(999);

        // Stub hasBeenRead() to do nothing
        doNothing().when(messageDAO).hasBeenRead(999, true);

        // Execute the servlet
        readMessageServlet.doPost(request, response);

        // Verify the servlet sets HTTP 200 OK
        verify(response).setStatus(HttpServletResponse.SC_OK);

        // No JSON is returned, so just check there's no error in the output
        assertTrue(responseWriter.toString().isEmpty(), "Should not have error text");
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Student tries to read a message that does not exist
    // -------------------------------------------------------------------------
    @Test
    public void testStudentMessageNotExists() throws Exception {
        // Student user
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(student);

        // The message ID is valid, but doesn't exist in DB
        when(request.getParameter("messageId")).thenReturn("999");
        doReturn(null).when(messageDAO).getMessage(999);

        readMessageServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("The message you are trying to close does not exists"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Student tries to read a message not addressed to them
    // -------------------------------------------------------------------------
    @Test
    public void testStudentNotReceiver() throws Exception {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(student);

        when(request.getParameter("messageId")).thenReturn("999");

        // This message is addressed to receiverId=200, not the student #100
        Message msg = new Message();
        msg.setId(999);
        msg.setReceiverId(200);
        doReturn(msg).when(messageDAO).getMessage(999);

        readMessageServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("You are not the receiver"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: Company reads an internship's message - success
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyReadsInternshipMessage() throws Exception {
        // Company user
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(company);

        // The message belongs to internship ID=999, which belongs to the company
        when(request.getParameter("messageId")).thenReturn("1111");

        Message msg = new Message();
        msg.setId(1111);
        msg.setReceiverId(999); // The message is addressed to internship #999

        // The company does own internship #999
        Internship intern999 = new Internship();
        intern999.setId(999);
        ArrayList<Internship> companyInternships = new ArrayList<>();
        companyInternships.add(intern999);

        doReturn(msg).when(messageDAO).getMessage(1111);
        doReturn(companyInternships).when(internshipDAO).getCompanyInternships(50);

        // Stub hasBeenRead
        doNothing().when(messageDAO).hasBeenRead(1111, false);

        readMessageServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertTrue(responseWriter.toString().isEmpty(), "No error text expected");
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: Company tries to read a message from an internship it doesn't own
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyUnauthorizedInternship() throws Exception {
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(company);

        when(request.getParameter("messageId")).thenReturn("123");

        // The message's receiver is internship #888, not owned by this company
        Message msg = new Message();
        msg.setId(123);
        msg.setReceiverId(888);
        doReturn(msg).when(messageDAO).getMessage(123);

        // The company's internships are #777, but not #888
        ArrayList<Internship> owned = new ArrayList<>();
        Internship i777 = new Internship(); i777.setId(777);
        owned.add(i777);
        doReturn(owned).when(internshipDAO).getCompanyInternships(50);

        readMessageServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        String output = responseWriter.toString();
        assertTrue(output.contains("you are not the company responsible"),
                "Should say the company is not authorized");
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: Missing or empty messageId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingMessageId() throws Exception {
        // Student user
        Student student = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(student);

        // messageId param missing
        when(request.getParameter("messageId")).thenReturn(null);

        readMessageServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: SQL Exception thrown retrieving the message
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetMessage() throws Exception {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(student);

        when(request.getParameter("messageId")).thenReturn("999");

        doThrow(new SQLException("DB getMessage error"))
                .when(messageDAO).getMessage(999);

        readMessageServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB getMessage error"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 8: SQL Exception thrown marking the message as read
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionHasBeenRead() throws Exception {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(student);

        when(request.getParameter("messageId")).thenReturn("999");

        // The message is valid and belongs to the student
        Message msg = new Message();
        msg.setId(999);
        msg.setReceiverId(100);
        doReturn(msg).when(messageDAO).getMessage(999);

        // But hasBeenRead throws an exception
        doThrow(new SQLException("DB hasBeenRead error"))
                .when(messageDAO).hasBeenRead(999, true);

        readMessageServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB hasBeenRead error"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: messageId parse error (non-numeric)
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidMessageIdFormat() throws Exception {
        // Student user
        Student student = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(student);

        // Suppose "messageId" is "abc" → parse fails
        when(request.getParameter("messageId")).thenReturn("abc");

        readMessageServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
