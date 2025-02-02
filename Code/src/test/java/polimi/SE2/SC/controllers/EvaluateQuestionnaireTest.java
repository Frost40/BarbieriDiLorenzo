
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
import java.util.Map;
import java.util.Optional;

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

import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.QuestionnaireDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Internship;
import polimi.SE2.SC.beans.Questionnaire;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.StudentHasInternship;
import polimi.SE2.SC.beans.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)

public class EvaluateQuestionnaireTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private Connection testConnection;

    // The DAOs we'll mock
    @Mock private QuestionnaireDAO questionnaireDAO;
    @Mock private StudentHasInternshipDAO studentHasInternshipDAO;
    @Mock private InternshipDAO internshipDAO;

    private EvaluateQuestionnaire evaluateQuestionnaireServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Spy on the servlet
        evaluateQuestionnaireServlet = spy(new EvaluateQuestionnaire());

        // Use TestUtils for the servlet config/context
        ServletConfig servletConfig = testUtils.setUp();
        evaluateQuestionnaireServlet.init(servletConfig);

        // Inject the mocked connection via reflection
        Field connectionField = EvaluateQuestionnaire.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(evaluateQuestionnaireServlet, testConnection);

        // Override DAO creation
        doReturn(questionnaireDAO).when(evaluateQuestionnaireServlet).createQuestionnaireDAO();
        doReturn(studentHasInternshipDAO).when(evaluateQuestionnaireServlet).createStudentHasInternshipDAO();
        doReturn(internshipDAO).when(evaluateQuestionnaireServlet).createInternshipDAO();

        // Prepare response writer
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // By default, request.getSession() => mocked session
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Company successfully evaluates a completed questionnaire
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyEvaluatesQuestionnaireSuccess() throws Exception {
        // Company user
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // Valid parameters
        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("true");

        // DB has a valid questionnaire #999 referencing studentHasInternship=555
        Questionnaire q = new Questionnaire();
        q.setId(999);
        q.setStudentHasInternshipId(555);
        q.setEvaluation(null);    // not evaluated yet
        q.setCompleted(true);     // is completed by the student
        doReturn(q).when(questionnaireDAO).getQuestionnaire(999);

        // DB has a valid StudentHasInternship #555 referencing internship=777
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(777);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company #200 owns internship #777
        Internship i777 = new Internship();
        i777.setId(777);
        ArrayList<Internship> owned = new ArrayList<>();
        owned.add(i777);
        doReturn(owned).when(internshipDAO).getCompanyInternships(200);

        // Stub setCompanyDecision
        doNothing().when(questionnaireDAO).setCompanyDecision(999, true);

        // Execute doPost
        evaluateQuestionnaireServlet.doPost(request, response);

        // Expect SC_OK
        verify(response).setStatus(HttpServletResponse.SC_OK);
        // The servlet does not return JSON, just sets SC_OK
        String out = responseWriter.toString();
        assertTrue(out.isEmpty(), "Should not contain error text on success");
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Missing studentHasInternshipId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingStudentHasInternshipId() throws Exception {
        Company companyUser = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // We skip providing studentHasInternshipId
        when(request.getParameter("studentHasInternshipId")).thenReturn(null);
        when(request.getParameter("questionnaireId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("true");

        evaluateQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("The id for the student-internship is null!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Missing questionnaireId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingQuestionnaireId() throws Exception {
        Company companyUser = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        // Skip questionnaireId
        when(request.getParameter("questionnaireId")).thenReturn(null);
        when(request.getParameter("evaluation")).thenReturn("false");

        evaluateQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("The id for the questionnaire is null!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: Invalid evaluation param (not 'true' or 'false')
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidEvaluationParam() throws Exception {
        Company companyUser = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("yes"); // invalid

        evaluateQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("Invalid evaluation parameter"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: Non-numeric questionnaireId
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidQuestionnaireId() throws Exception {
        Company companyUser = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("abc");
        when(request.getParameter("evaluation")).thenReturn("true");

        evaluateQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: Questionnaire not found
    // -------------------------------------------------------------------------
    @Test
    public void testQuestionnaireNotFound() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("false");

        doReturn(null).when(questionnaireDAO).getQuestionnaire(999);

        evaluateQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("No questionnaire found"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: Mismatch between questionnaire's studentHasInternshipId and param
    // -------------------------------------------------------------------------
    @Test
    public void testMismatchStudentHasInternship() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("true");

        Questionnaire q = new Questionnaire();
        q.setId(999);
        q.setStudentHasInternshipId(12345); // mismatch
        doReturn(q).when(questionnaireDAO).getQuestionnaire(999);

        evaluateQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("does not match the one found for the questionnaire"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 8: StudentHasInternship not found in DB
    // -------------------------------------------------------------------------
    @Test
    public void testNoStudentHasInternship() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("true");

        // The DB has a questionnaire referencing #555
        Questionnaire q = new Questionnaire();
        q.setId(999);
        q.setStudentHasInternshipId(555);
        q.setCompleted(true);
        doReturn(q).when(questionnaireDAO).getQuestionnaire(999);

        // But getStudentHasInternship(555) returns null
        doReturn(null).when(studentHasInternshipDAO).getStudentHasInternship(555);

        evaluateQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // Actually, the code tries to handle that scenario?
        // The code tries to retrieve from DB, no special check if it's null => "studentHasInternship is X"
        // Actually the code checks if (studentHasInternship==null)? => "The code does not do SC_INTERNAL_SERVER_ERROR but let's see:
        // code: StudentHasInternship studentHasInternship = ....
        // if studentHasInternship == null => ??? Actually we might get an NPE or not
        // Wait the code is:
        //    if (studentHasInternship == null) {
        //        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        //        response.getWriter().println("There is no ongoing internship ...");
        //        return;
        //    }
        // So it returns SC_UNAUTHORIZED
        // So let's fix that
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: The user is a student (not allowed)
    // -------------------------------------------------------------------------
    @Test
    public void testUserIsStudent() throws Exception {
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("true");

        evaluateQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // -------------------------------------------------------------------------
    // ★ Test 10: Company does not own the internship
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyNotOwningInternship() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("false");

        // DB has a valid questionnaire referencing #555
        Questionnaire q = new Questionnaire();
        q.setId(999);
        q.setStudentHasInternshipId(555);
        q.setCompleted(true);
        doReturn(q).when(questionnaireDAO).getQuestionnaire(999);

        // StudentHasInternship references internship #777
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(777);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company #200 owns internship #999, not #777
        Internship i999 = new Internship();
        i999.setId(999);
        ArrayList<Internship> owned = new ArrayList<>();
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(200);

        evaluateQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("you are not the company responsible"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 11: Wrong applicationStatus -> not selectionProcess
    // -------------------------------------------------------------------------
    @Test
    public void testWrongApplicationStatus() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("true");

        Questionnaire q = new Questionnaire();
        q.setId(999);
        q.setStudentHasInternshipId(555);
        q.setCompleted(true);
        q.setEvaluation(null);
        doReturn(q).when(questionnaireDAO).getQuestionnaire(999);

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(777);
        shi.setApplicationStatus("ongoing"); // not selectionProcess
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company does own #777 though
        Internship i777 = new Internship();
        i777.setId(777);
        ArrayList<Internship> owned = new ArrayList<>();
        owned.add(i777);
        doReturn(owned).when(internshipDAO).getCompanyInternships(200);

        evaluateQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("not the correct time to evaluate an questionnaire"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 12: Questionnaire has already been evaluated
    // -------------------------------------------------------------------------
    @Test
    public void testAlreadyEvaluated() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("true");

        Questionnaire q = new Questionnaire();
        q.setId(999);
        q.setStudentHasInternshipId(555);
        q.setCompleted(true);
        q.setEvaluation(true); // already evaluated
        doReturn(q).when(questionnaireDAO).getQuestionnaire(999);

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(777);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        Internship i777 = new Internship();
        i777.setId(777);
        ArrayList<Internship> owned = new ArrayList<>();
        owned.add(i777);
        doReturn(owned).when(internshipDAO).getCompanyInternships(200);

        evaluateQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("This questionnaire has already been evaluated"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 13: Questionnaire not completed yet
    // -------------------------------------------------------------------------
    @Test
    public void testQuestionnaireNotCompleted() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("false");

        // Found the questionnaire, but it's not completed
        Questionnaire q = new Questionnaire();
        q.setId(999);
        q.setStudentHasInternshipId(555);
        q.setCompleted(false);
        q.setEvaluation(null);
        doReturn(q).when(questionnaireDAO).getQuestionnaire(999);

        // StudentHasInternship data is correct
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(777);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company does own #777
        Internship i777 = new Internship();
        i777.setId(777);
        ArrayList<Internship> owned = new ArrayList<>();
        owned.add(i777);
        doReturn(owned).when(internshipDAO).getCompanyInternships(200);

        evaluateQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("This questionnaire has not been completed by the student yet"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 14: SQLException setting the company decision
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionSetCompanyDecision() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getParameter("questionnaireId")).thenReturn("999");
        when(request.getParameter("evaluation")).thenReturn("true");

        // DB has a valid questionnaire
        Questionnaire q = new Questionnaire();
        q.setId(999);
        q.setStudentHasInternshipId(555);
        q.setEvaluation(null);
        q.setCompleted(true);
        doReturn(q).when(questionnaireDAO).getQuestionnaire(999);

        // DB has a valid StudentHasInternship #555 with applicationStatus="selectionProcess"
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(777);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company #200 owns internship #777
        Internship i777 = new Internship();
        i777.setId(777);
        ArrayList<Internship> owned = new ArrayList<>();
        owned.add(i777);
        doReturn(owned).when(internshipDAO).getCompanyInternships(200);

        // But an SQLException is thrown when we attempt setCompanyDecision
        doThrow(new SQLException("DB error setting evaluation"))
                .when(questionnaireDAO).setCompanyDecision(999, true);

        evaluateQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error setting evaluation"));
    }
}




