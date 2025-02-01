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
import polimi.SE2.SC.beans.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HomePageManagerTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private Connection testConnection;

    // Mocks for all DAOs used by the servlet
    @Mock private MessageDAO messageDAO;
    @Mock private CvDAO cvDAO;
    @Mock private StudentDAO studentDAO;
    @Mock private CompanyDAO companyDAO;
    @Mock private FeedbackDAO feedbackDAO;
    @Mock private ComplaintDAO complaintDAO;
    @Mock private InternshipDAO internshipDAO;

    private HomePageManager homePageManagerServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create a spy of HomePageManager
        homePageManagerServlet = spy(new HomePageManager());

        // Use TestUtils to create mocked ServletConfig / ServletContext
        ServletConfig servletConfig = testUtils.setUp();
        homePageManagerServlet.init(servletConfig);

        // Inject our mocked connection into the private 'connection' field
        Field connectionField = HomePageManager.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(homePageManagerServlet, testConnection);

        // Override all DAO factory methods to return mocks
        doReturn(messageDAO).when(homePageManagerServlet).createMessageDAO();
        doReturn(cvDAO).when(homePageManagerServlet).createCvDAO();
        doReturn(studentDAO).when(homePageManagerServlet).createStudentDAO();
        doReturn(companyDAO).when(homePageManagerServlet).createCompanyDAO();
        doReturn(feedbackDAO).when(homePageManagerServlet).createFeedbackDAO();
        doReturn(complaintDAO).when(homePageManagerServlet).createComplaintDAO();
        doReturn(internshipDAO).when(homePageManagerServlet).createInternshipDAO();

        // Prepare a StringWriter to capture servlet output
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // When request.getSession() is called, return our mocked session
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Student scenario -> all DAO calls for a student succeed
    // -------------------------------------------------------------------------
    @Test
    public void testStudentSuccess() throws Exception {
        // Mock a Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(10);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // Return a CV from cvDAO
        Cv cvMock = new Cv();
        cvMock.setId(111);
        cvMock.setStudentId(10);

        doReturn(cvMock).when(cvDAO).getStudentCv(10);

        // Return a Student object from studentDAO
        Student studentMock = new Student();
        studentMock.setId(10);
        studentMock.setName("Alice");
        doReturn(studentMock).when(studentDAO).getStudentById(10);

        // Return some unread messages from messageDAO
        ArrayList<Message> unreadMessages = new ArrayList<>();
        Message msg1 = new Message();
        msg1.setId(1);
        msg1.setMessage("Hello");
        unreadMessages.add(msg1);
        doReturn(unreadMessages).when(messageDAO).getNotReadMessages(10, true);

        // Return some unread complaints from complaintDAO
        ArrayList<Complaint> unreadComplaints = new ArrayList<>();
        Complaint c1 = new Complaint();
        c1.setId(101);
        c1.setText("A complaint text");
        unreadComplaints.add(c1);
        doReturn(unreadComplaints).when(complaintDAO).getNotReadComplaints(10, true);

        // For a student, we do NOT call internshipDAO or feedbackDAO in success path
        // so we can leave them not stubbed, or do nothing.

        // Execute doGet
        homePageManagerServlet.doGet(request, response);

        // Verify SC_OK
        verify(response).setStatus(HttpServletResponse.SC_OK);

        // Parse JSON
        String output = responseWriter.toString();
        assertFalse(output.isEmpty());
        // Convert JSON to Map
        Map<String, Object> result = new Gson().fromJson(output, new TypeToken<HashMap<String,Object>>(){}.getType());

        // Check top-level keys
        assertTrue(result.containsKey("cv"), "Should contain 'cv'");
        assertTrue(result.containsKey("student"), "Should contain 'student'");
        assertTrue(result.containsKey("unreadMessages"), "Should contain 'unreadMessages'");
        assertTrue(result.containsKey("unreadComplaints"), "Should contain 'unreadComplaints'");
        // Should NOT contain 'internships', 'company', or 'unreadFeedbacks' for a student
        assertFalse(result.containsKey("internships"), "Should not contain 'internships' for a student");
        assertFalse(result.containsKey("company"), "Should not contain 'company' for a student");
        assertFalse(result.containsKey("unreadFeedbacks"), "Should not contain 'unreadFeedbacks' for a student");
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Company scenario -> all DAO calls for a company succeed
    // -------------------------------------------------------------------------
    @Test
    public void testCompanySuccess() throws Exception {
        // Mock a Company user
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(20);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // Return company internships
        ArrayList<Internship> internships = new ArrayList<>();
        Internship i1 = new Internship();
        i1.setId(501);
        i1.setName("Company Internship #501");
        internships.add(i1);
        doReturn(internships).when(internshipDAO).getCompanyInternships(20);

        // Return the company from companyDAO
        Company companyMock = new Company();
        companyMock.setId(20);
        companyMock.setEmail("company@example.com");
        doReturn(companyMock).when(companyDAO).getCompanyById(20);

        // Return some unread feedback
        ArrayList<Feedback> unreadFeedbacks = new ArrayList<>();
        Feedback f1 = new Feedback();
        f1.setId(201);
        f1.setRating(5);
        unreadFeedbacks.add(f1);
        doReturn(unreadFeedbacks).when(feedbackDAO).getNotReadFeedbacks(20);

        // Return unread messages
        ArrayList<Message> unreadMessages = new ArrayList<>();
        Message msg = new Message();
        msg.setId(10);
        unreadMessages.add(msg);
        doReturn(unreadMessages).when(messageDAO).getNotReadMessages(20, false);

        // Return unread complaints
        ArrayList<Complaint> unreadComplaints = new ArrayList<>();
        Complaint c2 = new Complaint();
        c2.setId(202);
        unreadComplaints.add(c2);
        doReturn(unreadComplaints).when(complaintDAO).getNotReadComplaints(20, false);

        // Execute
        homePageManagerServlet.doGet(request, response);

        // Check SC_OK
        verify(response).setStatus(HttpServletResponse.SC_OK);

        // Parse JSON
        String output = responseWriter.toString();
        assertFalse(output.isEmpty());

        Map<String, Object> result = new Gson().fromJson(output, new TypeToken<HashMap<String,Object>>(){}.getType());

        // Company scenario => 'internships', 'company', 'unreadFeedbacks', 'unreadMessages', 'unreadComplaints'
        assertTrue(result.containsKey("internships"), "Should contain 'internships'");
        assertTrue(result.containsKey("company"), "Should contain 'company'");
        assertTrue(result.containsKey("unreadFeedbacks"), "Should contain 'unreadFeedbacks'");
        assertTrue(result.containsKey("unreadMessages"), "Should contain 'unreadMessages'");
        assertTrue(result.containsKey("unreadComplaints"), "Should contain 'unreadComplaints'");

        // Should NOT contain 'cv' or 'student'
        assertFalse(result.containsKey("cv"), "Should not contain 'cv' in a company scenario");
        assertFalse(result.containsKey("student"), "Should not contain 'student' in a company scenario");
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Student scenario -> SQL Exception retrieving CV
    // -------------------------------------------------------------------------
    @Test
    public void testStudentSQLExceptionCv() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(10);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        doThrow(new SQLException("DB error retrieving CV"))
                .when(cvDAO).getStudentCv(10);

        homePageManagerServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error retrieving CV"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: Student scenario -> SQL Exception retrieving Student
    // -------------------------------------------------------------------------
    @Test
    public void testStudentSQLExceptionStudent() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(10);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // CV is fine
        Cv cvMock = new Cv();
        cvMock.setId(1);
        cvMock.setStudentId(10);
        doReturn(cvMock).when(cvDAO).getStudentCv(10);

        doThrow(new SQLException("DB error retrieving Student"))
                .when(studentDAO).getStudentById(10);

        homePageManagerServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error retrieving Student"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: Company scenario -> SQL Exception retrieving Internships
    // -------------------------------------------------------------------------
    @Test
    public void testCompanySQLExceptionInternships() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(20);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        doThrow(new SQLException("DB error retrieving company internships"))
                .when(internshipDAO).getCompanyInternships(20);

        homePageManagerServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error retrieving company internships"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: Company scenario -> SQL Exception retrieving Company
    // -------------------------------------------------------------------------
    @Test
    public void testCompanySQLExceptionCompany() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(20);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // Suppose internships is fine
        ArrayList<Internship> compInternships = new ArrayList<>();
        doReturn(compInternships).when(internshipDAO).getCompanyInternships(20);

        doThrow(new SQLException("DB error retrieving Company"))
                .when(companyDAO).getCompanyById(20);

        homePageManagerServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error retrieving Company"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: SQL Exception retrieving unread messages for either type
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionUnreadMessages() throws Exception {
        // Let's use a Student user again
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(10);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // CV & Student calls are fine
        doReturn(new Cv()).when(cvDAO).getStudentCv(10);
        doReturn(new Student()).when(studentDAO).getStudentById(10);

        // But unreadMessages throws
        doThrow(new SQLException("DB error retrieving unread messages"))
                .when(messageDAO).getNotReadMessages(10, true);

        homePageManagerServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error retrieving unread messages"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 8: SQL Exception retrieving unread complaints
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionUnreadComplaints() throws Exception {
        // Use Company user
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(20);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // Company calls are fine so far
        doReturn(new ArrayList<Internship>()).when(internshipDAO).getCompanyInternships(20);
        doReturn(new Company()).when(companyDAO).getCompanyById(20);
        doReturn(new ArrayList<Feedback>()).when(feedbackDAO).getNotReadFeedbacks(20);

        // But unreadComplaints throws
        doThrow(new SQLException("DB error retrieving unread complaints"))
                .when(complaintDAO).getNotReadComplaints(20, false);

        homePageManagerServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error retrieving unread complaints"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: Detailed JSON checks for Company scenario
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyDetailedJson() throws Exception {
        // Company user
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(30);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // Stub: Company internships
        ArrayList<Internship> internships = new ArrayList<>();
        Internship i1 = new Internship();
        i1.setId(101);
        i1.setName("Company Internship #101");
        i1.setCompanyId(30);
        internships.add(i1);
        doReturn(internships).when(internshipDAO).getCompanyInternships(30);

        // Stub: Company details
        Company compObj = new Company();
        compObj.setId(30);
        compObj.setEmail("mycompany@example.com");
        doReturn(compObj).when(companyDAO).getCompanyById(30);

        // Stub: Unread feedback
        ArrayList<Feedback> unreadFeedbacks = new ArrayList<>();
        Feedback fb1 = new Feedback();
        fb1.setId(999);
        fb1.setRating(10);
        unreadFeedbacks.add(fb1);
        doReturn(unreadFeedbacks).when(feedbackDAO).getNotReadFeedbacks(30);

        // Stub: Unread messages
        ArrayList<Message> unreadMsgs = new ArrayList<>();
        Message msg = new Message();
        msg.setId(1);
        unreadMsgs.add(msg);
        doReturn(unreadMsgs).when(messageDAO).getNotReadMessages(30, false);

        // Stub: Unread complaints
        ArrayList<Complaint> unreadComplaints = new ArrayList<>();
        Complaint complaint = new Complaint();
        complaint.setId(888);
        unreadComplaints.add(complaint);
        doReturn(unreadComplaints).when(complaintDAO).getNotReadComplaints(30, false);

        // Execute doGet
        homePageManagerServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);

        // Parse JSON fully
        String output = responseWriter.toString();
        assertFalse(output.isEmpty());

        // We expect a structure with "internships", "company", "unreadFeedbacks", "unreadMessages", "unreadComplaints"
        // e.g. {
        //   internships: [ { id: 101, ... } ],
        //   company: { id: 30, email: "..." },
        //   unreadFeedbacks: [ { id:999, rating:10, ...} ],
        //   unreadMessages: [ { id:1, ...} ],
        //   unreadComplaints: [ { id:888, ...} ]
        // }

        // Let's parse as a Map
        Map<String, Object> topLevel = new Gson().fromJson(output, new TypeToken<HashMap<String,Object>>(){}.getType());

        assertTrue(topLevel.containsKey("internships"));
        assertTrue(topLevel.containsKey("company"));
        assertTrue(topLevel.containsKey("unreadFeedbacks"));
        assertTrue(topLevel.containsKey("unreadMessages"));
        assertTrue(topLevel.containsKey("unreadComplaints"));

        // Check we don't see "cv" or "student"
        assertFalse(topLevel.containsKey("cv"));
        assertFalse(topLevel.containsKey("student"));

        // If you like, parse each field in detail. We'll just do one example:
        // For "internships", parse it as a List of Map
        ArrayList<Map<String,Object>> internshipsJson = (ArrayList<Map<String,Object>>) topLevel.get("internships");
        assertEquals(1, internshipsJson.size());
        assertEquals(101.0, internshipsJson.get(0).get("id"));  // note: numeric fields become Double in a raw map parse
        assertEquals("Company Internship #101", internshipsJson.get(0).get("name"));
    }
}
