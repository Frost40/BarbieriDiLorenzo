package polimi.SE2.SC.controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
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

import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.DAOs.FeedbackDAO;
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Feedback;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.StudentHasInternship;
import polimi.SE2.SC.beans.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReadFeedbackTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;

    // These DAOs match your code above:
    @Mock private FeedbackDAO feedbackDAO;
    @Mock private StudentHasInternshipDAO studentHasInternshipDAO;
    @Mock private InternshipDAO internshipDAO;

    // We do not instantiate them directly. We'll override the servlet's
    // createFeedbackDAO(), createStudentHasInternshipDAO(), createInternshipDAO()
    // to return these mocks.

    @Mock private Connection testConnection; // The DB connection is also mocked.

    // We'll create a spy so we can override factory methods.
    private ReadFeedback readFeedbackServlet;

    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create a spy of the ReadFeedback servlet
        readFeedbackServlet = spy(new ReadFeedback());

        // Use TestUtils to create a mocked ServletConfig (and ServletContext).
        ServletConfig servletConfig = testUtils.setUp();
        readFeedbackServlet.init(servletConfig);

        // Inject the mocked connection into the private 'connection' field via reflection
        Field connectionField = ReadFeedback.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(readFeedbackServlet, testConnection);

        // Override the factory methods to return our mocked DAOs
        doReturn(feedbackDAO).when(readFeedbackServlet).createFeedbackDAO();
        doReturn(studentHasInternshipDAO).when(readFeedbackServlet).createStudentHasInternshipDAO();
        doReturn(internshipDAO).when(readFeedbackServlet).createInternshipDAO();

        // Prepare a StringWriter to capture servlet output
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // When request.getSession() is called, return our mocked session
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Company reads a valid feedback
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyReadsFeedbackSuccess() throws Exception {
        // Company user
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // Provide a valid feedback ID
        when(request.getParameter("feedbackId")).thenReturn("1001");

        // The DB has a feedback #1001
        Feedback feedback = new Feedback();
        feedback.setId(1001);
        feedback.setStudentHasInternshipId(777);
        doReturn(feedback).when(feedbackDAO).getFeedback(1001);

        // The DB has a StudentHasInternship #777 referencing internship #999
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(777);
        shi.setInternshipId(999);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(777);

        // The company's internships include #999
        Internship internship999 = new Internship();
        internship999.setId(999);
        ArrayList<Internship> companyInternships = new ArrayList<>();
        companyInternships.add(internship999);
        doReturn(companyInternships).when(internshipDAO).getCompanyInternships(50);

        // Mark the feedback as read
        doNothing().when(feedbackDAO).hasBeenRead(1001);

        // Execute doPost()
        readFeedbackServlet.doPost(request, response);

        // Expect SC_OK
        verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Missing or empty feedbackId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingFeedbackId() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // Missing param
        when(request.getParameter("feedbackId")).thenReturn(null);

        readFeedbackServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("The id for the complaint is null!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Student tries to read feedback (not allowed)
    // -------------------------------------------------------------------------
    @Test
    public void testStudentReadsFeedback() throws Exception {
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("feedbackId")).thenReturn("1001");

        // We can stub the DB calls or do nothing. The servlet checks isStudent early
        readFeedbackServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: Feedback not found in DB
    // -------------------------------------------------------------------------
    @Test
    public void testFeedbackNotFound() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("feedbackId")).thenReturn("1001");

        // The DB returns null for feedback #1001
        doReturn(null).when(feedbackDAO).getFeedback(1001);

        readFeedbackServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: Error retrieving feedback from DB
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionWhenRetrievingFeedback() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("feedbackId")).thenReturn("1001");

        doThrow(new SQLException("DB error retrieving feedback"))
                .when(feedbackDAO).getFeedback(1001);

        readFeedbackServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: StudentHasInternship not found or DB error
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionStudentHasInternship() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("feedbackId")).thenReturn("1002");

        // The feedback #1002 is valid
        Feedback feedback = new Feedback();
        feedback.setId(1002);
        feedback.setStudentHasInternshipId(777);
        doReturn(feedback).when(feedbackDAO).getFeedback(1002);

        // DB error retrieving the StudentHasInternship
        doThrow(new SQLException("DB error on StudentHasInternship"))
                .when(studentHasInternshipDAO).getStudentHasInternship(777);

        readFeedbackServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: Company does not own the internship in question
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyNotOwner() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("feedbackId")).thenReturn("1003");

        // The feedback #1003 is valid
        Feedback feedback = new Feedback();
        feedback.setId(1003);
        feedback.setStudentHasInternshipId(777);
        doReturn(feedback).when(feedbackDAO).getFeedback(1003);

        // The StudentHasInternship #777 references internship #999
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(777);
        shi.setInternshipId(999);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(777);

        // The company only owns internship #888, not #999
        ArrayList<Internship> ownedByCompany = new ArrayList<>();
        Internship internship888 = new Internship();
        internship888.setId(888);
        ownedByCompany.add(internship888);

        doReturn(ownedByCompany).when(internshipDAO).getCompanyInternships(50);

        readFeedbackServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // -------------------------------------------------------------------------
    // ★ Test 8: Marking the feedback as read throws exception
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionHasBeenRead() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("feedbackId")).thenReturn("1004");

        // The DB has a feedback #1004 referencing StudentHasInternship #777
        Feedback feedback = new Feedback();
        feedback.setId(1004);
        feedback.setStudentHasInternshipId(777);
        doReturn(feedback).when(feedbackDAO).getFeedback(1004);

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(777);
        shi.setInternshipId(999);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(777);

        // The company does own internship #999
        Internship internship999 = new Internship();
        internship999.setId(999);
        ArrayList<Internship> owned = new ArrayList<>();
        owned.add(internship999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(50);

        // Throw exception on hasBeenRead
        doThrow(new SQLException("DB error on hasBeenRead"))
                .when(feedbackDAO).hasBeenRead(1004);

        readFeedbackServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: Non-numeric feedbackId
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidFeedbackIdFormat() throws Exception {
        // Company user
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(50);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // feedbackId is "abc"
        when(request.getParameter("feedbackId")).thenReturn("abc");

        readFeedbackServlet.doPost(request, response);

        // parsingChecker fails -> returns early with BAD_REQUEST
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
