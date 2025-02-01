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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

import polimi.SE2.SC.DAOs.*;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Message;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SendMessageTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;

    @Mock
    private MessageDAO messageDAO;
    @Mock
    private InternshipDAO internshipDAO;
    @Mock
    private StudentDAO studentDAO;
    @Mock
    private Connection testConnection;

    private SendMessage sendMessageServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create a spy for the SendMessage servlet
        sendMessageServlet = spy(new SendMessage());

        // Use TestUtils to create a mocked ServletConfig and ServletContext
        ServletConfig servletConfig = testUtils.setUp();
        sendMessageServlet.init(servletConfig);

        // Inject the mocked connection into the private 'connection' field via reflection
        Field connectionField = SendMessage.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(sendMessageServlet, testConnection);

        // Override the factory methods to return our mocks
        doReturn(messageDAO).when(sendMessageServlet).createMessageDAO();
        doReturn(internshipDAO).when(sendMessageServlet).createInternshipDAO();
        doReturn(studentDAO).when(sendMessageServlet).createStudentDAO();

        // Prepare a StringWriter to capture servlet output
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // Mock session retrieval
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Student sends a valid message to an existing Internship
    // -------------------------------------------------------------------------
    @Test
    public void testStudentSendsValidMessage() throws Exception {
        // Student user in session
        Student studentSender = mock(Student.class);
        when(studentSender.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentSender);

        // Request parameters
        when(request.getParameter("message")).thenReturn("Hello, I'd like to join!");
        when(request.getParameter("receiverId")).thenReturn("200"); // Internship ID

        // The internship is valid
        Internship internship = new Internship();
        internship.setId(200);
        internship.setName("Internship #200");
        internship.setDescription("Test internship");
        doReturn(internship).when(internshipDAO).getInternshipById(200);

        // Stub insertMessage() and setChatToRead() to do nothing
        doNothing().when(messageDAO).insertMessage(anyString(), eq(false), eq(100), eq(200));
        doNothing().when(messageDAO).setChatToRead(eq(100), eq(200), eq(true));

        // Return an updated chat
        ArrayList<Message> updatedChat = new ArrayList<>();
        Message msg1 = new Message();
        msg1.setId(1);
        msg1.setMessage("Hello, I'd like to join!");
        msg1.setFromInternship(false);
        msg1.setSenderId(100);
        msg1.setReceiverId(200);
        updatedChat.add(msg1);
        doReturn(updatedChat).when(messageDAO).getAllChatMessages(100, 200);

        // Execute
        sendMessageServlet.doPost(request, response);

        // Verify success
        verify(response).setStatus(HttpServletResponse.SC_OK);

        // Parse the JSON response
        String output = responseWriter.toString();
        Map<String, Object> result = new Gson().fromJson(output, new TypeToken<HashMap<String,Object>>(){}.getType());
        assertTrue(result.containsKey("chat"), "Response should contain 'chat'");

        // Check the chat array
        ArrayList<Map<String, Object>> chatJson = (ArrayList<Map<String, Object>>) result.get("chat");
        assertEquals(1, chatJson.size(), "Chat should have 1 message");
        assertEquals("Hello, I'd like to join!", chatJson.get(0).get("message"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Message is invalid (empty or too long)
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidMessageLength() throws Exception {
        // Student user
        Student studentSender = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentSender);

        // Provide an empty message
        when(request.getParameter("message")).thenReturn("");
        when(request.getParameter("receiverId")).thenReturn("200");

        sendMessageServlet.doPost(request, response);

        // Expect BAD_REQUEST
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("Invalid message"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Missing or null receiverId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingReceiverId() throws Exception {
        // Student user
        Student studentSender = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentSender);

        when(request.getParameter("message")).thenReturn("Hello!");
        when(request.getParameter("receiverId")).thenReturn(null); // missing

        sendMessageServlet.doPost(request, response);

        // Expect BAD_REQUEST
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("Null receiver id!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: Student tries to send message, but the internship does not exist
    // -------------------------------------------------------------------------
    @Test
    public void testStudentReceiverNotExist() throws Exception {
        Student studentSender = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentSender);

        when(request.getParameter("message")).thenReturn("Hi there!");
        when(request.getParameter("receiverId")).thenReturn("999"); // Suppose internship 999 doesn't exist

        // The internshipDAO returns null
        doReturn(null).when(internshipDAO).getInternshipById(999);

        sendMessageServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("Either the account you are interacting with does not exists"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: Company tries to send message, but no senderId param provided
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyMissingSenderId() throws Exception {
        // Company user in session
        Company companySender = mock(Company.class);
        when(companySender.getId()).thenReturn(10);
        when(session.getAttribute("currentUser")).thenReturn(companySender);

        // Request
        when(request.getParameter("message")).thenReturn("Hello from internship #500");
        when(request.getParameter("receiverId")).thenReturn("300"); // The student's ID
        when(request.getParameter("senderId")).thenReturn(null);    // Missing param

        sendMessageServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("Null receiver id!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: Company tries to send message from an internship it doesn't own
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyUnownedInternship() throws Exception {
        // Company user
        Company companySender = mock(Company.class);
        when(companySender.getId()).thenReturn(10);
        when(session.getAttribute("currentUser")).thenReturn(companySender);

        // Request
        when(request.getParameter("message")).thenReturn("Hi Student");
        when(request.getParameter("receiverId")).thenReturn("300");   // Suppose student #300
        when(request.getParameter("senderId")).thenReturn("500");    // internship #500

        // The internship #500 does exist, but belongs to someone else
        Internship internship500 = new Internship();
        internship500.setId(500);
        doReturn(internship500).when(internshipDAO).getInternshipById(500);

        // But the company's own internships do NOT include #500
        ArrayList<Internship> companyInternships = new ArrayList<>();
        // e.g. The company only owns internship #501
        Internship internship501 = new Internship();
        internship501.setId(501);
        companyInternships.add(internship501);
        doReturn(companyInternships).when(internshipDAO).getCompanyInternships(10);

        sendMessageServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        String output = responseWriter.toString();
        assertTrue(output.contains("not the company responsible for the given internship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: Company tries to start a brand-new chat w/ a student (no existing chat)
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyStartsNewChatUnauthorized() throws Exception {
        // Company user
        Company companySender = mock(Company.class);
        when(companySender.getId()).thenReturn(10);
        when(session.getAttribute("currentUser")).thenReturn(companySender);

        when(request.getParameter("message")).thenReturn("First message from the internship!");
        when(request.getParameter("receiverId")).thenReturn("300");
        when(request.getParameter("senderId")).thenReturn("500");

        // The internship #500 belongs to the company
        Internship internship500 = new Internship();
        internship500.setId(500);
        doReturn(internship500).when(internshipDAO).getInternshipById(500);

        ArrayList<Internship> companyInternships = new ArrayList<>();
        companyInternships.add(internship500);
        doReturn(companyInternships).when(internshipDAO).getCompanyInternships(10);

        // The student #300 does exist
        Student student300 = new Student();
        student300.setId(300);
        doReturn(student300).when(studentDAO).getStudentById(300);

        // But there's NO existing chat (the DAO returns null or empty)
        doReturn(null).when(messageDAO).getAllChatMessages(300, 10);

        sendMessageServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("Only students are authorized to initiate"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 8: Successful Company → Student message (existing chat)
    // -------------------------------------------------------------------------
    @Test
    public void testCompanySendsValidMessage() throws Exception {
        // Company user
        Company companySender = mock(Company.class);
        when(companySender.getId()).thenReturn(10);
        when(session.getAttribute("currentUser")).thenReturn(companySender);

        when(request.getParameter("message")).thenReturn("Follow-up message from internship #500");
        when(request.getParameter("receiverId")).thenReturn("300");
        when(request.getParameter("senderId")).thenReturn("500");

        // The internship #500 belongs to the company
        Internship internship500 = new Internship();
        internship500.setId(500);
        doReturn(internship500).when(internshipDAO).getInternshipById(500);

        ArrayList<Internship> companyInternships = new ArrayList<>();
        companyInternships.add(internship500);
        doReturn(companyInternships).when(internshipDAO).getCompanyInternships(10);

        // Student #300 exists
        Student student300 = new Student();
        student300.setId(300);
        doReturn(student300).when(studentDAO).getStudentById(300);

        // There's an existing chat
        ArrayList<Message> existingChat = new ArrayList<>();
        Message oldMessage = new Message();
        oldMessage.setId(1);
        oldMessage.setSenderId(300); // Student
        oldMessage.setReceiverId(10); // Company (?), or maybe 500 if we interpret differently
        oldMessage.setFromInternship(false);
        oldMessage.setMessage("Hello from the student!");
        existingChat.add(oldMessage);
        doReturn(existingChat).when(messageDAO).getAllChatMessages(300, 10);

        // Insert new message
        doNothing().when(messageDAO).insertMessage(anyString(), eq(true), eq(500), eq(300));
        doNothing().when(messageDAO).setChatToRead(eq(300), eq(500), eq(false));

        // Return updated chat
        ArrayList<Message> updatedChat = new ArrayList<>(existingChat);
        Message newMsg = new Message();
        newMsg.setId(2);
        newMsg.setSenderId(500);
        newMsg.setReceiverId(300);
        newMsg.setFromInternship(true);
        newMsg.setMessage("Follow-up message from internship #500");
        updatedChat.add(newMsg);

        doReturn(updatedChat).when(messageDAO).getAllChatMessages(300, 10);

        sendMessageServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);

        // Parse output
        String output = responseWriter.toString();
        Map<String, Object> result = new Gson().fromJson(output, new TypeToken<HashMap<String,Object>>(){}.getType());
        assertTrue(result.containsKey("chat"));

        ArrayList<Map<String, Object>> chatJson = (ArrayList<Map<String, Object>>) result.get("chat");
        assertEquals(2, chatJson.size(), "Updated chat should have 2 messages");
        assertEquals("Follow-up message from internship #500", chatJson.get(1).get("message"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: SQL Exception occurs (e.g. during insertMessage)
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionDuringInsert() throws Exception {
        // Student user
        Student studentSender = mock(Student.class);
        when(studentSender.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentSender);

        when(request.getParameter("message")).thenReturn("Hello internship!");
        when(request.getParameter("receiverId")).thenReturn("200");

        Internship internship = new Internship();
        internship.setId(200);
        doReturn(internship).when(internshipDAO).getInternshipById(200);

        // Simulate exception on insertMessage
        doThrow(new SQLException("DB Insert Error")).when(messageDAO).insertMessage(anyString(), anyBoolean(), anyInt(), anyInt());

        sendMessageServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String output = responseWriter.toString();
        assertTrue(output.contains("DB Insert Error"));
    }
}
