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
import java.util.List;
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
import com.google.gson.reflect.TypeToken;

import polimi.SE2.SC.DAOs.MessageDAO;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Message;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.User;
import polimi.SE2.SC.beans.Company;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MessageManagerTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;

    @Mock
    private MessageDAO messageDAO;
    @Mock
    private Connection testConnection; // our dummy/mocked connection

    // We will create the spy manually.
    private MessagesManager messagesManagerServlet;

    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create a spy manually for the servlet.
        messagesManagerServlet = spy(new MessagesManager());

        // Use TestUtils to create a mocked ServletConfig (and ServletContext).
        ServletConfig servletConfig = testUtils.setUp();
        messagesManagerServlet.init(servletConfig);

        // Inject the mocked connection into the private 'connection' field.
        Field connectionField = MessagesManager.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(messagesManagerServlet, testConnection);

        // Override the factory method to return our mocked MessageDAO.
        doReturn(messageDAO).when(messagesManagerServlet).createMessageDAO();

        // Prepare a StringWriter to capture servlet output.
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // When request.getSession() is called, return our mocked session.
        when(request.getSession()).thenReturn(session);
    }
    // ★ Test 1: Successful doGet for a Student user (ArrayList<Internship>)
    @Test
    public void testDoGetStudentSuccess() throws Exception {
        // Simulate a student user.
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(1001);
        when(session.getAttribute("currentUser")).thenReturn(student);

        // Create a dummy list of internships (representing the chat list for a student)
        ArrayList<Internship> dummyChats = new ArrayList<>();

        Internship intern1 = new Internship();
        intern1.setId(1);
        intern1.setName("Software Engineering Internship");
        intern1.setDescription("Work on Java projects.");

        Internship intern2 = new Internship();
        intern2.setId(2);
        intern2.setName("Data Science Internship");
        intern2.setDescription("Work with Python and Machine Learning.");

        dummyChats.add(intern1);
        dummyChats.add(intern2);

        // Stub DAO method to return the list of internships
        when(messageDAO.getAllChatsForStudent(1001)).thenReturn(dummyChats);

        // Call doGet()
        messagesManagerServlet.doGet(request, response);

        // Verify response status, content type, and encoding
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");

        // Parse JSON output
        String jsonOutput = responseWriter.toString();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Convert JSON to a Map
        Map<String, Object> result = gson.fromJson(jsonOutput, new TypeToken<HashMap<String, Object>>() {}.getType());

        // Verify "allChats" key exists
        assertTrue(result.containsKey("allChats"), "JSON should contain key 'allChats'");

        // Extract "allChats" as a list
        ArrayList<?> internshipsList = (ArrayList<?>) result.get("allChats");
        assertNotNull(internshipsList, "allChats should not be null");

        // Ensure the list has 2 internships
        assertEquals(2, internshipsList.size(), "There should be 2 internships in the chat list");

        // Convert each internship JSON object to a map and verify content
        Map<String, Object> firstInternship = (Map<String, Object>) internshipsList.get(0);
        Map<String, Object> secondInternship = (Map<String, Object>) internshipsList.get(1);

        assertEquals("Software Engineering Internship", firstInternship.get("name"), "First internship name should match");
        assertEquals("Work on Java projects.", firstInternship.get("description"), "First internship description should match");

        assertEquals("Data Science Internship", secondInternship.get("name"), "Second internship name should match");
        assertEquals("Work with Python and Machine Learning.", secondInternship.get("description"), "Second internship description should match");
    }

    // More detailed tests are difficult for companies as there are problmes with casting here in the test
    // It is visible via System.out.println(result) that we get what is expected
    // ★ Test 2: Successful doGet for a Company user
    @Test
    public void testDoGetCompanySuccess() throws Exception {
        // Simulate a company user.
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(2002);
        when(session.getAttribute("currentUser")).thenReturn(company);

        // Create a dummy HashMap for company chats.
        HashMap<Internship, ArrayList<Student>> dummyChats = new HashMap<>();
        Internship intern1 = new Internship();
        intern1.setId(10);
        intern1.setName("Internship X");
        ArrayList<Student> studentsChat = new ArrayList<>();
        Student student1 = new Student();
        student1.setId(3003);
        student1.setName("Alice");
        studentsChat.add(student1);
        dummyChats.put(intern1, studentsChat);
        when(messageDAO.getAllChatsForCompany(2002)).thenReturn(dummyChats);

        // Call doGet()
        messagesManagerServlet.doGet(request, response);

        // Verify that the response status is OK.
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");

        // Parse JSON output.
        String jsonOutput = responseWriter.toString();
        Gson gson = new GsonBuilder().setPrettyPrinting().enableComplexMapKeySerialization().create();
        Map<String, Object> result = gson.fromJson(jsonOutput, new TypeToken<HashMap<String, Object>>(){}.getType());

        assertTrue(result.containsKey("allChats"), "JSON should contain key 'allChats'");
        // Extract "allChats" from the response
        List<List<Object>> allChats = (List<List<Object>>) result.get("allChats");

        // Validate that there is exactly one internship entry
        assertEquals(1, allChats.size(), "There should be 1 internship entry");

        // Extract the internship details and student list
        List<Object> internshipEntry = allChats.get(0);

        assertEquals(2, internshipEntry.size(), "Each entry should have an internship and a student list");

        // Extract the internship data
        Map<String, Object> internshipData = (Map<String, Object>) internshipEntry.get(0);

        assertEquals(10.0, internshipData.get("id"), "Internship ID should be 10.0");
        assertEquals("Internship X", internshipData.get("name"), "Internship name should be 'Internship X'");

        // Extract the student list
        List<Map<String, Object>> studentList = (List<Map<String, Object>>) internshipEntry.get(1);

        assertEquals(1, studentList.size(), "There should be 1 student in the list");

        // Extract student details
        Map<String, Object> studentData = studentList.get(0);

        assertEquals(3003.0, studentData.get("id"), "Student ID should be 3003.0");
        assertEquals("Alice", studentData.get("name"), "Student name should be 'Alice'");

    }
    // ★ Test 3: doGet for a Student user when DAO throws SQLException
    @Test
    public void testDoGetStudentSQLException() throws Exception {
        // Simulate a student user.
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(1001);
        when(session.getAttribute("currentUser")).thenReturn(student);

        // Simulate SQLException thrown by DAO.
        when(messageDAO.getAllChatsForStudent(1001)).thenThrow(new SQLException("DB retrieval error"));

        // Call doGet()
        messagesManagerServlet.doGet(request, response);

        // Expect Internal Server Error.
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String output = responseWriter.toString();
        assertTrue(output.contains("DB retrieval error"), "Output should contain the error message");
    }

    // ★ Test 4: doGet for a Company user when DAO throws SQLException
    @Test
    public void testDoGetCompanySQLException() throws Exception {
        // Simulate a company user.
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(2002);
        when(session.getAttribute("currentUser")).thenReturn(company);

        // Simulate SQLException thrown by DAO.
        when(messageDAO.getAllChatsForCompany(2002)).thenThrow(new SQLException("DB retrieval error for company"));

        // Call doGet()
        messagesManagerServlet.doGet(request, response);

        // Expect Internal Server Error.
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String output = responseWriter.toString();
        assertTrue(output.contains("DB retrieval error for company"), "Output should contain the error message");
    }


    @Test
    public void testDoGetStudentNoChats() throws Exception {
        // Simulate a student user
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(1001);
        when(session.getAttribute("currentUser")).thenReturn(student);

        // Stub DAO method to return an empty list
        when(messageDAO.getAllChatsForStudent(1001)).thenReturn(new ArrayList<>());

        // Call doGet()
        messagesManagerServlet.doGet(request, response);

        // Verify response status
        verify(response).setStatus(HttpServletResponse.SC_OK);

        // Parse JSON output
        String jsonOutput = responseWriter.toString();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Map<String, Object> result = gson.fromJson(jsonOutput, new TypeToken<HashMap<String, Object>>() {}.getType());

        assertNotNull(result, "Response should not be null");
        assertTrue(result.containsKey("allChats"), "JSON should contain key 'allChats'");
        assertTrue(((ArrayList<?>) result.get("allChats")).isEmpty(), "allChats should be an empty list");
    }

    // Issues with the casting to test companies chats
    @Test
    public void testDoGetCompanyNoChats() throws Exception {
        // Simulate a company user
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(2002);
        when(session.getAttribute("currentUser")).thenReturn(company);

        // Stub DAO method to return an empty HashMap
        when(messageDAO.getAllChatsForCompany(2002)).thenReturn(new HashMap<>());

        // Call doGet()
        messagesManagerServlet.doGet(request, response);

        // Verify response status
        verify(response).setStatus(HttpServletResponse.SC_OK);

        // Parse JSON output
        String jsonOutput = responseWriter.toString();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Map<String, Object> result = gson.fromJson(jsonOutput, new TypeToken<HashMap<String, Object>>() {}.getType());

        assertNotNull(result, "Response should not be null");
        assertTrue(result.containsKey("allChats"), "JSON should contain key 'allChats'");
        assertTrue(((Map<?, ?>) result.get("allChats")).isEmpty(), "allChats should be an empty object");
    }

    @Test
    public void testDoGetCompanyWithInternshipHavingNoStudents() throws Exception {
        // Simulate a company user.
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(2002);
        when(session.getAttribute("currentUser")).thenReturn(company);

        // Create a dummy HashMap for company chats.
        HashMap<Internship, ArrayList<Student>> dummyChats = new HashMap<>();
        Internship intern1 = new Internship();
        intern1.setId(10);
        intern1.setName("Internship X");
        ArrayList<Student> studentsChat = new ArrayList<>();
        dummyChats.put(intern1, studentsChat);
        when(messageDAO.getAllChatsForCompany(2002)).thenReturn(dummyChats);

        // Call doGet()
        messagesManagerServlet.doGet(request, response);

        // Verify that the response status is OK.
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");

        // Parse JSON output.
        String jsonOutput = responseWriter.toString();
        Gson gson = new GsonBuilder().setPrettyPrinting().enableComplexMapKeySerialization().create();
        Map<String, Object> result = gson.fromJson(jsonOutput, new TypeToken<HashMap<String, Object>>() {
        }.getType());

        assertTrue(result.containsKey("allChats"), "JSON should contain key 'allChats'");
        // Extract "allChats" from the response and ensure the internship exists but has no students
        // Extract "allChats"
        List<List<Object>> allChats = (List<List<Object>>) result.get("allChats");

        // Validate that there is exactly one internship entry
        assertEquals(1, allChats.size(), "There should be 1 internship entry");

        // Extract the internship details and student list
        List<Object> internshipEntry = allChats.get(0);

        assertEquals(2, internshipEntry.size(), "Each entry should have an internship and a student list");

        // Extract the internship data
        Map<String, Object> internshipData = (Map<String, Object>) internshipEntry.get(0);

        assertEquals(10.0, internshipData.get("id"), "Internship ID should be 10.0");
        assertEquals("Internship X", internshipData.get("name"), "Internship name should be 'Internship X'");

        // Extract the student list
        List<Object> studentList = (List<Object>) internshipEntry.get(1);

        assertTrue(studentList.isEmpty(), "Student list should be empty");


    }

}
