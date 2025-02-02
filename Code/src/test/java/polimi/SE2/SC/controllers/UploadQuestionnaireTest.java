

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
import java.util.*;

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
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.*;
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.StudentHasInternship;
import polimi.SE2.SC.beans.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UploadQuestionnaireTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private Connection testConnection;

    @Mock private QuestionnaireDAO questionnaireDAO;
    @Mock private StudentHasInternshipDAO studentHasInternshipDAO;
    @Mock private InternshipDAO internshipDAO;

    @Mock private Part filePart;

    private UploadQuestionnaire uploadQuestionnaireServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create a spy of UploadQuestionnaire
        uploadQuestionnaireServlet = spy(new UploadQuestionnaire());

        // Use TestUtils to create a mocked ServletConfig and ServletContext
        ServletConfig servletConfig = testUtils.setUp();
        uploadQuestionnaireServlet.init(servletConfig);

        // Inject the mocked connection
        Field connectionField = UploadQuestionnaire.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(uploadQuestionnaireServlet, testConnection);

        // Override DAO creation methods
        doReturn(questionnaireDAO).when(uploadQuestionnaireServlet).createQuestionnaireDAO();
        doReturn(studentHasInternshipDAO).when(uploadQuestionnaireServlet).createStudentHasInternshipDAO();
        doReturn(internshipDAO).when(uploadQuestionnaireServlet).createInternshipDAO();

        // Prepare a StringWriter to capture the servlet output
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // By default, our session is the mocked one
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Company uploads a valid questionnaire (happy path)
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyUploadsQuestionnaireSuccess() throws Exception {
        // Mock a Company user
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // Provide a valid studentHasInternshipId
        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        // Mock the uploaded file part
        when(request.getPart("questionnaire")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");
        when(filePart.getInputStream()).thenReturn(new ByteArrayInputStream("PDF data".getBytes()));

        // The DB has a valid StudentHasInternship #555
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(777);  // belongs to some internship
        shi.setApplicationStatus("selectionProcess"); // correct time period
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company does own internship #777
        Internship internship777 = new Internship();
        internship777.setId(777);
        ArrayList<Internship> ownedByCompany = new ArrayList<>();
        ownedByCompany.add(internship777);
        doReturn(ownedByCompany).when(internshipDAO).getCompanyInternships(200);

        // Stub the upload call
        doNothing().when(questionnaireDAO).uploadQuestionnaire(any(), eq("pdf"), eq(555));

        // Then, we return a list of questionnaires after upload
        ArrayList<Questionnaire> questionnaires = new ArrayList<>();
        Questionnaire q1 = new Questionnaire();
        q1.setId(999);
        q1.setStudentHasInternshipId(555);
        q1.setCompleted(false);
        questionnaires.add(q1);
        doReturn(questionnaires).when(questionnaireDAO).getOngoingInternshipQuestionnaires(555);

        // Execute doPost
        uploadQuestionnaireServlet.doPost(request, response);

        // Expect SC_OK
        verify(response).setStatus(HttpServletResponse.SC_OK);

        // Check the JSON
        String output = responseWriter.toString();
        assertFalse(output.isEmpty());
        Map<String,Object> topLevel = new Gson().fromJson(output, new TypeToken<HashMap<String,Object>>(){}.getType());
        assertTrue(topLevel.containsKey("questionnaires"));
        ArrayList<Map<String,Object>> array = (ArrayList<Map<String,Object>>) topLevel.get("questionnaires");
        assertEquals(1, array.size());
        assertEquals(999.0, array.get(0).get("id"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Missing studentHasInternshipId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingStudentHasInternshipId() throws Exception {
        // Company user
        Company companyUser = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        // We don't provide the param
        when(request.getParameter("studentHasInternshipId")).thenReturn(null);

        uploadQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("No valid id for the ongoing internship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: No questionnaire file part
    // -------------------------------------------------------------------------
    @Test
    public void testNoQuestionnaireFilePart() throws Exception {
        Company companyUser = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");

        // Return null for request.getPart("questionnaire")
        when(request.getPart("questionnaire")).thenReturn(null);

        uploadQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("No questionnaire file has been uploaded!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: Student user tries to upload (not allowed)
    // -------------------------------------------------------------------------
    @Test
    public void testStudentUploading() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        // Possibly we have a filePart
        when(request.getPart("questionnaire")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");

        uploadQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: StudentHasInternship not found in DB
    // -------------------------------------------------------------------------
    @Test
    public void testStudentHasInternshipNotFound() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getPart("questionnaire")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");

        doReturn(null).when(studentHasInternshipDAO).getStudentHasInternship(555);

        uploadQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("There is no ongoing internship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: The company does not own the internship
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyNotOwningInternship() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getPart("questionnaire")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");

        // DB has StudentHasInternship #555 referencing internship #777
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(777);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company #200 only owns internship #999, not 777
        Internship i999 = new Internship();
        i999.setId(999);
        ArrayList<Internship> owned = new ArrayList<>();
        owned.add(i999);
        doReturn(owned).when(internshipDAO).getCompanyInternships(200);

        uploadQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("you are not the company responsible"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: Wrong time to upload a questionnaire (applicationStatus != selectionProcess)
    // -------------------------------------------------------------------------
    @Test
    public void testWrongTimeToUploadQuestionnaire() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getPart("questionnaire")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");

        // The internship is not in 'selectionProcess'
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(777);
        shi.setApplicationStatus("ongoing"); // not selectionProcess
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company does own internship #777
        Internship i777 = new Internship();
        i777.setId(777);
        ArrayList<Internship> owned = new ArrayList<>();
        owned.add(i777);
        doReturn(owned).when(internshipDAO).getCompanyInternships(200);

        uploadQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("You are not authorized to upload a questionnaire"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 8: Invalid file format
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidFileFormat() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getPart("questionnaire")).thenReturn(filePart);
        // e.g. image/png is invalid
        when(filePart.getContentType()).thenReturn("image/png");

        // The DB is set up so it would be correct if not for the file type
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

        uploadQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("File format not permitted!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: IOException / SQLException while uploading the questionnaire
    // -------------------------------------------------------------------------
    @Test
    public void testIOExceptionWhileUploading() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getPart("questionnaire")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");

        // DB is set up
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

        // But getInputStream() throws
        when(filePart.getInputStream()).thenThrow(new IOException("IO error reading the file"));

        uploadQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("IO error reading the file"));
    }

    @Test
    public void testSQLExceptionWhileUploading() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getPart("questionnaire")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");
        when(filePart.getInputStream()).thenReturn(new ByteArrayInputStream("PDF data".getBytes()));

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

        // But the DB fails
        doThrow(new SQLException("DB error uploading questionnaire"))
                .when(questionnaireDAO).uploadQuestionnaire(any(), eq("pdf"), eq(555));

        uploadQuestionnaireServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error uploading questionnaire"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 10: Final check => verifying "questionnaires" in JSON
    // -------------------------------------------------------------------------
    @Test
    public void testJsonResponseAfterUpload() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("studentHasInternshipId")).thenReturn("555");
        when(request.getPart("questionnaire")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");
        when(filePart.getInputStream()).thenReturn(new ByteArrayInputStream("Some pdf data".getBytes()));

        // Good scenario
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

        doNothing().when(questionnaireDAO).uploadQuestionnaire(any(), eq("pdf"), eq(555));

        // After upload, suppose we have 2 questionnaires in progress
        ArrayList<Questionnaire> qList = new ArrayList<>();
        Questionnaire q1 = new Questionnaire();
        q1.setId(1001);
        q1.setStudentHasInternshipId(555);
        q1.setFileType("pdf");
        q1.setCompleted(false);

        Questionnaire q2 = new Questionnaire();
        q2.setId(1002);
        q2.setStudentHasInternshipId(555);
        q2.setFileType("pdf");
        q2.setCompleted(false);

        qList.add(q1);
        qList.add(q2);

        doReturn(qList).when(questionnaireDAO).getOngoingInternshipQuestionnaires(555);

        // Execute
        uploadQuestionnaireServlet.doPost(request, response);

        // Check SC_OK
        verify(response).setStatus(HttpServletResponse.SC_OK);

        String output = responseWriter.toString();
        assertFalse(output.isEmpty());

        // Parse JSON
        Map<String,Object> topLevel = new Gson().fromJson(output, new TypeToken<HashMap<String,Object>>(){}.getType());
        assertTrue(topLevel.containsKey("questionnaires"));

        ArrayList<Map<String,Object>> qArr =
                (ArrayList<Map<String,Object>>) topLevel.get("questionnaires");
        assertEquals(2, qArr.size());
        assertEquals(1001.0, qArr.get(0).get("id"));
        assertEquals(1002.0, qArr.get(1).get("id"));
    }
}
