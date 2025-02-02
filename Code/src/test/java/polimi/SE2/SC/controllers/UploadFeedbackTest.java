package polimi.SE2.SC.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

import polimi.SE2.SC.DAOs.FeedbackDAO;
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UploadFeedbackTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private Connection testConnection;

    @Mock private StudentHasInternshipDAO studentHasInternshipDAO;
    @Mock private FeedbackDAO feedbackDAO;
    @Mock private InternshipDAO internshipDAO;

    private UploadFeedback uploadFeedbackServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Spy the servlet
        uploadFeedbackServlet = spy(new UploadFeedback());

        ServletConfig servletConfig = testUtils.setUp();
        uploadFeedbackServlet.init(servletConfig);

        // Inject mock connection
        Field connField = UploadFeedback.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connField.set(uploadFeedbackServlet, testConnection);

        // Override DAO creation
        doReturn(studentHasInternshipDAO).when(uploadFeedbackServlet).createStudentHasInternshipDAO();
        doReturn(feedbackDAO).when(uploadFeedbackServlet).createFeedbackDAO();
        doReturn(internshipDAO).when(uploadFeedbackServlet).createInternshipDAO();

        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // Default session
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Student successfully uploads feedback for an ended internship
    // -------------------------------------------------------------------------
    @Test
    public void testStudentUploadsFeedbackSuccess() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // Valid params
        when(request.getParameter("text")).thenReturn("Great experience!");
        when(request.getParameter("rating")).thenReturn("8");
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        // The DB has StudentHasInternship #555 => ended
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(100);
        shi.setApplicationStatus("ended");
        shi.setInternshipId(999);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The user has not provided feedback yet
        doReturn(new ArrayList<Feedback>()).when(feedbackDAO).getFeedbacksGivenByStudent(100);

        // Upload feedback
        doNothing().when(feedbackDAO).uploadFeedback("Great experience!", 8, 555);

        // Then we return a feedback list for the internship
        ArrayList<Feedback> feedbacks = new ArrayList<>();
        Feedback f1 = new Feedback();
        f1.setId(10);
        f1.setRating(8);
        feedbacks.add(f1);
        doReturn(feedbacks).when(feedbackDAO).getInternshipFeedbacks(999);

        // Updating rating => no op
        doNothing().when(internshipDAO).updateInternshipRating(999, 8.0);

        uploadFeedbackServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String out = responseWriter.toString();
        assertTrue(out.contains("\"feedbacks\""));
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Feedback text too long
    // -------------------------------------------------------------------------
    @Test
    public void testFeedbackTextTooLong() throws Exception {
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // Over 255 chars
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<300;i++) sb.append("A");
        when(request.getParameter("text")).thenReturn(sb.toString());
        when(request.getParameter("rating")).thenReturn("5");
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        uploadFeedbackServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("Invalid feedback"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Missing ongoingInternshipId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingOngoingInternshipId() throws Exception {
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("text")).thenReturn("Ok experience");
        when(request.getParameter("rating")).thenReturn("5");
        when(request.getParameter("ongoingInternshipId")).thenReturn(null);

        uploadFeedbackServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("No valid id for the ongoing internship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: Missing rating
    // -------------------------------------------------------------------------
    @Test
    public void testMissingRating() throws Exception {
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("text")).thenReturn("Ok experience");
        when(request.getParameter("rating")).thenReturn(null);
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        uploadFeedbackServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("The rating given for the internship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: Invalid rating range
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidRatingRange() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("text")).thenReturn("Ok experience");
        when(request.getParameter("rating")).thenReturn("15"); // invalid
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        uploadFeedbackServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("Invalid rating"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: StudentHasInternship not found
    // -------------------------------------------------------------------------
    @Test
    public void testStudentHasInternshipNotFound() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("text")).thenReturn("Ok experience");
        when(request.getParameter("rating")).thenReturn("8");
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        doReturn(null).when(studentHasInternshipDAO).getStudentHasInternship(555);

        uploadFeedbackServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("There is no ongoing internship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: Internship not ended => no feedback
    // -------------------------------------------------------------------------
    @Test
    public void testInternshipNotEnded() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("text")).thenReturn("Ok experience");
        when(request.getParameter("rating")).thenReturn("8");
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(100);
        shi.setApplicationStatus("ongoing"); // not ended
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        uploadFeedbackServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("only after the internship period has ended"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 8: User is not a student
    // -------------------------------------------------------------------------
    @Test
    public void testUserIsCompany() throws Exception {
        Company companyUser = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("text")).thenReturn("Ok experience");
        when(request.getParameter("rating")).thenReturn("8");
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        uploadFeedbackServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: Student mismatch
    // -------------------------------------------------------------------------
    @Test
    public void testStudentMismatch() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("text")).thenReturn("Ok experience");
        when(request.getParameter("rating")).thenReturn("8");
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(999); // mismatch
        shi.setApplicationStatus("ended");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        uploadFeedbackServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("not associated to the given internship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 10: Already gave feedback for this internship
    // -------------------------------------------------------------------------
    @Test
    public void testAlreadyGaveFeedback() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("text")).thenReturn("Ok experience");
        when(request.getParameter("rating")).thenReturn("8");
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(100);
        shi.setApplicationStatus("ended");
        shi.setInternshipId(999);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The user has feedback for the same studentHasInternship #555
        ArrayList<Feedback> existingFeedback = new ArrayList<>();
        Feedback f = new Feedback();
        f.setId(1);
        f.setStudentHasInternshipId(555);
        existingFeedback.add(f);

        doReturn(existingFeedback).when(feedbackDAO).getFeedbacksGivenByStudent(100);

        uploadFeedbackServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("only evaluate each internship experience once"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 11: SQL exception on uploadFeedback
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionUploadFeedback() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("text")).thenReturn("Ok experience");
        when(request.getParameter("rating")).thenReturn("8");
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(100);
        shi.setApplicationStatus("ended");
        shi.setInternshipId(999);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        doReturn(new ArrayList<Feedback>()).when(feedbackDAO).getFeedbacksGivenByStudent(100);

        doThrow(new SQLException("DB error uploading feedback"))
                .when(feedbackDAO).uploadFeedback("Ok experience", 8, 555);

        uploadFeedbackServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error uploading feedback"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 12: SQL exception retrieving feedbacks or updating rating
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionRetrievingFeedbacksOrUpdatingRating() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("text")).thenReturn("Ok experience");
        when(request.getParameter("rating")).thenReturn("8");
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(100);
        shi.setApplicationStatus("ended");
        shi.setInternshipId(999);
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        doReturn(new ArrayList<Feedback>()).when(feedbackDAO).getFeedbacksGivenByStudent(100);
        doNothing().when(feedbackDAO).uploadFeedback("Ok experience", 8, 555);

        // Throw an exception when getInternshipFeedbacks
        doThrow(new SQLException("DB error retrieving feedbacks"))
                .when(feedbackDAO).getInternshipFeedbacks(999);

        uploadFeedbackServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error retrieving feedbacks"));
    }
}
