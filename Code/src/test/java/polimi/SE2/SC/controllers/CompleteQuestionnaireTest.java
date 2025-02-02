
package polimi.SE2.SC.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import javax.servlet.http.Part;

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

import polimi.SE2.SC.DAOs.QuestionnaireDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.Questionnaire;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.StudentHasInternship;
import polimi.SE2.SC.beans.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CompleteQuestionnaireTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private Connection testConnection;

    // The two DAOs we need
    @Mock private QuestionnaireDAO questionnaireDAO;
    @Mock private StudentHasInternshipDAO studentHasInternshipDAO;

    // We'll spy the servlet so we can override its factory methods
    private CompleteQuestionnaire completeQuestionnaireServlet;

    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    // We also need to mock the Part that represents the file upload
    @Mock private Part filePart;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create a spy of CompleteQuestionnaire
        completeQuestionnaireServlet = spy(new CompleteQuestionnaire());

        // Use TestUtils to get a mocked ServletConfig, etc.
        ServletConfig servletConfig = testUtils.setUp();
        completeQuestionnaireServlet.init(servletConfig);

        // Inject our mocked connection via reflection
        Field connectionField = CompleteQuestionnaire.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(completeQuestionnaireServlet, testConnection);

        // Override the DAO creation methods to return our mocks
        doReturn(questionnaireDAO).when(completeQuestionnaireServlet).createQuestionnaireDAO();
        doReturn(studentHasInternshipDAO).when(completeQuestionnaireServlet).createStudentHasInternshipDAO();

        // Set up the response writer
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // By default, request.getSession() returns our mocked session
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Student completes questionnaire successfully (happy path)
    // -------------------------------------------------------------------------
    @Test
    public void testStudentCompletesQuestionnaireSuccess() throws Exception {
        // Student user in session
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // Valid parameters
        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");

        // We'll mock a filePart with PDF content
        when(request.getPart("questionnaire")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");
        when(filePart.getInputStream()).thenReturn(new ByteArrayInputStream("PDF data".getBytes()));

        // The DB has a valid questionnaire #999 referencing studentHasInternshipId=555
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setId(999);
        questionnaire.setStudentHasInternshipId(555);
        questionnaire.setCompleted(false);

        doReturn(questionnaire).when(questionnaireDAO).getQuestionnaire(999);

        // The DB has a StudentHasInternship #555 referencing the same user #100
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(100);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // Stub the completion method
        doNothing().when(questionnaireDAO).completeQuestionnaire(eq(999), any(), eq("pdf"));

        // Then, after completion, we retrieve all ongoing questionnaires
        ArrayList<Questionnaire> questionnaires = new ArrayList<>();
        Questionnaire q1 = new Questionnaire();
        q1.setId(999);
        q1.setCompleted(true);
        questionnaires.add(q1);

        doReturn(questionnaires).when(questionnaireDAO).getOngoingInternshipQuestionnaires(555);

        // Execute doPost
        completeQuestionnaireServlet.doPost(request, response);

        // Expect SC_OK
        verify(response).setStatus(HttpServletResponse.SC_OK);

        // Check JSON output
        String output = responseWriter.toString();
        assertFalse(output.isEmpty());

        // We parse the top-level map
        Map<String, Object> topLevel =
                new Gson().fromJson(output, new TypeToken<HashMap<String, Object>>(){}.getType());
        assertTrue(topLevel.containsKey("questionnaires"), "Should contain 'questionnaires' key");

        // If you want to parse it as a list of Questionnaire, we can do so
        ArrayList<Map<String,Object>> questionnairesJson =
                (ArrayList<Map<String,Object>>) topLevel.get("questionnaires");
        assertEquals(1, questionnairesJson.size());
        assertEquals(999.0, questionnairesJson.get(0).get("id"));
        assertTrue((Boolean) questionnairesJson.get(0).get("completed"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Missing studentHasInternshipId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingStudentHasInternshipId() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // We do not provide studentHasInternshipId
        when(request.getParameter("studentHasInternshipId")).thenReturn(null);
        when(request.getParameter("questionnaireId")).thenReturn("999");

        completeQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("The id for the student-internship is null!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Missing questionnaireId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingQuestionnaireId() throws Exception {
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn(null);

        completeQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("The id for the questionnaire is null!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: Parsing error for questionnaireId
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidQuestionnaireIdFormat() throws Exception {
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("abc"); // invalid parse

        completeQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: No questionnaire found in DB
    // -------------------------------------------------------------------------
    @Test
    public void testNoQuestionnaireFound() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");

        // DB returns null
        doReturn(null).when(questionnaireDAO).getQuestionnaire(999);

        completeQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("No questionnaire found for the given id"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: Questionnaire's studentHasInternshipId != param
    // -------------------------------------------------------------------------
    @Test
    public void testMismatchStudentHasInternshipId() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");

        Questionnaire q = new Questionnaire();
        q.setId(999);
        q.setStudentHasInternshipId(9999); // mismatch
        q.setCompleted(false);

        doReturn(q).when(questionnaireDAO).getQuestionnaire(999);

        completeQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("does not match the one found for the questionnaire"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: Not a student user
    // -------------------------------------------------------------------------
    @Test
    public void testNotAStudent() throws Exception {
        // Suppose it's a Company or some other user type
        Company companyUser = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");

        completeQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // -------------------------------------------------------------------------
    // ★ Test 8: Student not matching the StudentHasInternship
    // -------------------------------------------------------------------------
    @Test
    public void testStudentNotOwnerOfInternship() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");

        // DB has a matching questionnaire
        Questionnaire q = new Questionnaire();
        q.setId(999);
        q.setStudentHasInternshipId(555);
        q.setCompleted(false);
        doReturn(q).when(questionnaireDAO).getQuestionnaire(999);

        // DB has a StudentHasInternship #555 with studentId=300 => mismatch
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(300);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // Execute
        completeQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("not a student associated to the given internship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: Wrong time to complete questionnaire (applicationStatus != selectionProcess)
    // -------------------------------------------------------------------------
    @Test
    public void testWrongTimeToCompleteQuestionnaire() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");

        // DB has a valid questionnaire
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setId(999);
        questionnaire.setStudentHasInternshipId(555);
        questionnaire.setCompleted(false);
        doReturn(questionnaire).when(questionnaireDAO).getQuestionnaire(999);

        // StudentHasInternship has status = "ongoing" instead of "selectionProcess"
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(100);
        shi.setApplicationStatus("ongoing");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        completeQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("This is not the correct time to complete a questionnaire"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 10: Questionnaire is already completed
    // -------------------------------------------------------------------------
    //TODO Check Complete Questionnaire Servlet
    @Test
    public void testAlreadyCompletedQuestionnaire() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");
        when(request.getPart("questionnaire")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");
        when(filePart.getInputStream()).thenReturn(new ByteArrayInputStream("PDF data".getBytes()));

        Questionnaire q = new Questionnaire();
        q.setId(999);
        q.setStudentHasInternshipId(555);
        q.setCompleted(true); // already completed
        doReturn(q).when(questionnaireDAO).getQuestionnaire(999);

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(100);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        completeQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // -------------------------------------------------------------------------
    // ★ Test 11: Invalid file format
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidFileFormat() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");

        // Return a valid, not-completed questionnaire
        Questionnaire q = new Questionnaire();
        q.setId(999);
        q.setStudentHasInternshipId(555);
        q.setCompleted(false);
        doReturn(q).when(questionnaireDAO).getQuestionnaire(999);

        // Return matching StudentHasInternship
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(100);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The file part is image/png => not permitted
        when(request.getPart("questionnaire")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("image/png");

        completeQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("File format not permitted!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 12: SQLException retrieving the questionnaire
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetQuestionnaire() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");

        doThrow(new SQLException("DB error retrieving questionnaire"))
                .when(questionnaireDAO).getQuestionnaire(999);

        completeQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error retrieving questionnaire"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 13: SQLException retrieving the StudentHasInternship
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionGetStudentHasInternship() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");

        // The questionnaire is found
        Questionnaire q = new Questionnaire();
        q.setId(999);
        q.setStudentHasInternshipId(555);
        q.setCompleted(false);
        doReturn(q).when(questionnaireDAO).getQuestionnaire(999);

        doThrow(new SQLException("DB error retrieving StudentHasInternship"))
                .when(studentHasInternshipDAO).getStudentHasInternship(555);

        completeQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error retrieving StudentHasInternship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 14: IOException / SQLException completing the questionnaire
    // -------------------------------------------------------------------------
    @Test
    public void testIOExceptionCompletingQuestionnaire() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");

        // Good questionnaire
        Questionnaire q = new Questionnaire();
        q.setId(999);
        q.setStudentHasInternshipId(555);
        q.setCompleted(false);
        doReturn(q).when(questionnaireDAO).getQuestionnaire(999);

        // Good StudentHasInternship
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(100);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // filePart is PDF
        when(request.getPart("questionnaire")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");

        // But an IOException is thrown when we call getInputStream()
        when(filePart.getInputStream()).thenThrow(new IOException("IO error reading file"));

        completeQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("IO error reading file"));
    }

    @Test
    public void testSQLExceptionCompletingQuestionnaire() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");

        Questionnaire q = new Questionnaire();
        q.setId(999);
        q.setStudentHasInternshipId(555);
        q.setCompleted(false);
        doReturn(q).when(questionnaireDAO).getQuestionnaire(999);

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(100);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        when(request.getPart("questionnaire")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");
        when(filePart.getInputStream()).thenReturn(new ByteArrayInputStream("pdf data".getBytes()));

        // Throw an SQLException in completeQuestionnaire
        doThrow(new SQLException("DB error completing questionnaire"))
                .when(questionnaireDAO).completeQuestionnaire(eq(999), any(), eq("pdf"));

        completeQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String out = responseWriter.toString();
        assertTrue(out.contains("DB error completing questionnaire"));
    }
}

