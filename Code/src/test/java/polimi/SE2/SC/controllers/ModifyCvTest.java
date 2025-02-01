package polimi.SE2.SC.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
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

import polimi.SE2.SC.DAOs.CvDAO;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Cv;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.beans.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ModifyCvTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private Part filePart;
    @Mock private CvDAO cvDAO;
    @Mock private Connection testConnection;

    // We'll manually create the spy.
    private ModifyCv modifyCvServlet;

    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create a spy of the servlet so we can override createCvDAO().
        modifyCvServlet = spy(new ModifyCv());

        // Use TestUtils to mock ServletConfig and ServletContext.
        ServletConfig servletConfig = testUtils.setUp();
        modifyCvServlet.init(servletConfig);

        // Inject the mocked connection into the servlet's private 'connection' field.
        Field connectionField = ModifyCv.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(modifyCvServlet, testConnection);

        // Override the factory method to return our mocked CvDAO.
        doReturn(cvDAO).when(modifyCvServlet).createCvDAO();

        // Prepare a StringWriter to capture servlet output.
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // When request.getSession() is called, return our mocked session.
        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Successful CV Modification by a Student
    // -------------------------------------------------------------------------
    @Test
    public void testStudentModifiesCvSuccess() throws Exception {
        // Student user
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(101);
        when(session.getAttribute("currentUser")).thenReturn(student);

        // Request parameters
        when(request.getParameter("cvId")).thenReturn("500");  // The student's existing CV id
        when(request.getParameter("experiences")).thenReturn("Exp1,Exp2");
        when(request.getParameter("programmingSkills")).thenReturn("Java,Python");
        when(request.getParameter("otherSkills")).thenReturn("Teamwork,Communication");

        // Mock filePart (the new CV file)
        when(request.getPart("cv")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");
        when(filePart.getInputStream()).thenReturn(new ByteArrayInputStream("dummy pdf data".getBytes()));

        // The student's existing CV in DB
        Cv existingCv = new Cv();
        existingCv.setId(500);
        existingCv.setStudentId(101);
        existingCv.setExperiences(new ArrayList<>());
        existingCv.setProgrammingSkills(new ArrayList<>());
        existingCv.setOtherSkills(new ArrayList<>());

        // Return this CV from getStudentCv()
        doReturn(existingCv).when(cvDAO).getStudentCv(101);

        // Stub the updateCv() method
        doNothing().when(cvDAO).updateCv(eq(500), any(), eq("pdf"), anyString(), anyString(), anyString());

        // Execute the servlet
        modifyCvServlet.doPost(request, response);

        // Verify success
        verify(response).setStatus(HttpServletResponse.SC_OK);

        // Parse JSON output
        String jsonOutput = responseWriter.toString();
        Map<String, Object> result = new Gson().fromJson(jsonOutput, new TypeToken<HashMap<String, Object>>(){}.getType());
        assertTrue(result.containsKey("modifiedCv"), "JSON should contain 'modifiedCv'");

        // Ensure experiences, programmingSkills, otherSkills are updated
        Map<String, Object> cvJson = (Map<String, Object>) result.get("modifiedCv");
        // The server updates the Cv object in memory with the new splitted arrays.
        assertEquals(2.0, ((ArrayList<?>) cvJson.get("experiences")).size(), "Should have 2 experiences");
        assertEquals(2.0, ((ArrayList<?>) cvJson.get("programmingSkills")).size(), "Should have 2 programming skills");
        assertEquals(2.0, ((ArrayList<?>) cvJson.get("otherSkills")).size(), "Should have 2 other skills");
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Invalid Experiences Parameter
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidExperiences() throws Exception {
        Student student = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(student);

        // Provide experiences as an empty string
        when(request.getParameter("experiences")).thenReturn("");
        when(request.getParameter("programmingSkills")).thenReturn("Java");
        when(request.getParameter("otherSkills")).thenReturn("Communication");
        when(request.getParameter("cvId")).thenReturn("500");
        when(request.getPart("cv")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");

        modifyCvServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("Invalid experiences set"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Company tries to modify a CV (Unauthorized)
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyModifiesCv() throws Exception {
        Company company = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(company);

        // Provide valid parameters
        when(request.getParameter("cvId")).thenReturn("500");
        when(request.getParameter("experiences")).thenReturn("Exp");
        when(request.getParameter("programmingSkills")).thenReturn("Prog");
        when(request.getParameter("otherSkills")).thenReturn("Other");
        when(request.getPart("cv")).thenReturn(filePart);

        modifyCvServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("No cv is associated to a company"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: Missing cvId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingCvId() throws Exception {
        Student student = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(student);

        when(request.getParameter("cvId")).thenReturn(null); // missing
        when(request.getParameter("experiences")).thenReturn("Exp");
        when(request.getParameter("programmingSkills")).thenReturn("Prog");
        when(request.getParameter("otherSkills")).thenReturn("Other");
        when(request.getPart("cv")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");

        modifyCvServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("The id for the cv is null!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: Student tries to modify CV, but no CV found in DB
    // -------------------------------------------------------------------------
    @Test
    public void testNoCvInDb() throws Exception {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(111);
        when(session.getAttribute("currentUser")).thenReturn(student);

        when(request.getParameter("cvId")).thenReturn("500");
        when(request.getParameter("experiences")).thenReturn("Exp");
        when(request.getParameter("programmingSkills")).thenReturn("Prog");
        when(request.getParameter("otherSkills")).thenReturn("Other");
        when(request.getPart("cv")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");

        // The DAO returns null for getStudentCv()
        doReturn(null).when(cvDAO).getStudentCv(111);

        modifyCvServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("The cv you are trying to modify does not exists"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: Student tries to modify someone else's CV
    // -------------------------------------------------------------------------
    @Test
    public void testWrongOwner() throws Exception {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(111);
        when(session.getAttribute("currentUser")).thenReturn(student);

        when(request.getParameter("cvId")).thenReturn("999");
        when(request.getParameter("experiences")).thenReturn("Exp");
        when(request.getParameter("programmingSkills")).thenReturn("Prog");
        when(request.getParameter("otherSkills")).thenReturn("Other");
        when(request.getPart("cv")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");

        // The DB does have a CV, but it belongs to another student
        Cv otherStudentCv = new Cv();
        otherStudentCv.setId(999);
        otherStudentCv.setStudentId(222); // different user
        doReturn(otherStudentCv).when(cvDAO).getStudentCv(111);

        modifyCvServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        String output = responseWriter.toString();
        assertTrue(output.contains("You can only modify your own Cv"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: Invalid file content type
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidFileType() throws Exception {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(111);
        when(session.getAttribute("currentUser")).thenReturn(student);

        when(request.getParameter("cvId")).thenReturn("999");
        when(request.getParameter("experiences")).thenReturn("Exp");
        when(request.getParameter("programmingSkills")).thenReturn("Prog");
        when(request.getParameter("otherSkills")).thenReturn("Other");
        when(request.getPart("cv")).thenReturn(filePart);
        // Provide an unsupported file type
        when(filePart.getContentType()).thenReturn("image/png");

        // Suppose the student's CV is found
        Cv existingCv = new Cv();
        existingCv.setId(999);
        existingCv.setStudentId(111);
        doReturn(existingCv).when(cvDAO).getStudentCv(111);

        modifyCvServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("File format not permitted!"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 8: SQL Exception during updateCv
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionInUpdate() throws Exception {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(101);
        when(session.getAttribute("currentUser")).thenReturn(student);

        when(request.getParameter("cvId")).thenReturn("500");
        when(request.getParameter("experiences")).thenReturn("Exp");
        when(request.getParameter("programmingSkills")).thenReturn("Prog");
        when(request.getParameter("otherSkills")).thenReturn("Other");
        when(request.getPart("cv")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");
        when(filePart.getInputStream()).thenReturn(new ByteArrayInputStream("dummy pdf content".getBytes()));

        // The student's CV in DB
        Cv cv = new Cv();
        cv.setId(500);
        cv.setStudentId(101);
        doReturn(cv).when(cvDAO).getStudentCv(101);

        doThrow(new SQLException("DB update error"))
                .when(cvDAO).updateCv(eq(500), any(), eq("pdf"), anyString(), anyString(), anyString());

        modifyCvServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB update error"));
    }
}
