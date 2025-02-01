package polimi.SE2.SC.controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import polimi.SE2.SC.DAOs.CvDAO;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Cv;
import polimi.SE2.SC.beans.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UploadNewCvTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Mock
    private Part filePart;
    @Mock
    private CvDAO cvDAO;
    @Mock
    private Connection testConnection; // our dummy/mocked connection

    // We will create the spy manually.
    private UploadNewCv uploadNewCvServlet;

    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create a spy manually.
        uploadNewCvServlet = spy(new UploadNewCv());

        // Use TestUtils to create a mocked ServletConfig and ServletContext.
        ServletConfig servletConfig = testUtils.setUp();

        // Initialize the servlet. In init(), UploadNewCv calls:
        //    this.connection = ConnectionHandler.getConnection(getServletContext());
        // We want to override that connection with our testConnection.
        uploadNewCvServlet.init(servletConfig);

        // Use reflection to set the private 'connection' field to our mocked connection.
        Field connectionField = UploadNewCv.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(uploadNewCvServlet, testConnection);

        // Override the factory method to return our mocked CvDAO.
        doReturn(cvDAO).when(uploadNewCvServlet).createCvDAO();

        // Prepare a StringWriter to capture servlet output.
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // When request.getSession() is called, return our mocked session.
        when(request.getSession()).thenReturn(session);
    }

    // ★ Test 1: Successful CV Upload (student with no existing CV)
    @Test
    public void testSuccessfulUpload() throws Exception {
        // Provide valid parameters
        when(request.getParameter("experiences")).thenReturn("Experience1,Experience2");
        when(request.getParameter("programmingSkills")).thenReturn("Java,Python");
        when(request.getParameter("otherSkills")).thenReturn("Communication,Teamwork");
        when(request.getPart("cv")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");
        byte[] dummyFile = "dummy pdf content".getBytes();
        when(filePart.getInputStream()).thenReturn(new ByteArrayInputStream(dummyFile));

        // Simulate a logged-in student (User that is not an instance of Company)
        User student = mock(User.class);
        when(student.getId()).thenReturn(1);
        when(session.getAttribute("currentUser")).thenReturn(student);

        // Ensure no CV exists yet for this student.
        when(cvDAO.getStudentCv(1)).thenReturn(null);

        // Call doPost() to simulate the file upload.
        uploadNewCvServlet.doPost(request, response);

        // Expect a 200 OK status
        verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    // ★ Test 2: Invalid Experiences Parameter (e.g., null)
    @Test
    public void testInvalidExperiences() throws Exception {
        when(request.getParameter("experiences")).thenReturn(null);
        when(request.getParameter("programmingSkills")).thenReturn("Java,Python");
        when(request.getParameter("otherSkills")).thenReturn("Communication");
        when(request.getPart("cv")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");
        when(filePart.getInputStream()).thenReturn(new ByteArrayInputStream("dummy".getBytes()));

        User student = mock(User.class);
        when(student.getId()).thenReturn(1);
        when(session.getAttribute("currentUser")).thenReturn(student);

        uploadNewCvServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("Invalid experiences set!"));
    }

    // ★ Test 3: Upload Attempt by a Company (not allowed)
    @Test
    public void testUploadByCompany() throws Exception {
        when(request.getParameter("experiences")).thenReturn("Some experience");
        when(request.getParameter("programmingSkills")).thenReturn("Some skills");
        when(request.getParameter("otherSkills")).thenReturn("Other skills");
        when(request.getPart("cv")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");
        when(filePart.getInputStream()).thenReturn(new ByteArrayInputStream("dummy".getBytes()));

        // Simulate a company as the current user.
        Company company = mock(Company.class);
        when(session.getAttribute("currentUser")).thenReturn(company);

        uploadNewCvServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("No CV is associated to a company"));
    }

    // ★ Test 4: Student Already Has a CV Uploaded
    @Test
    public void testAlreadyExistingCv() throws Exception {
        when(request.getParameter("experiences")).thenReturn("Exp");
        when(request.getParameter("programmingSkills")).thenReturn("Prog");
        when(request.getParameter("otherSkills")).thenReturn("Other");
        when(request.getPart("cv")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");
        when(filePart.getInputStream()).thenReturn(new ByteArrayInputStream("dummy".getBytes()));

        User student = mock(User.class);
        when(student.getId()).thenReturn(1);
        when(session.getAttribute("currentUser")).thenReturn(student);

        // Simulate that a CV already exists for this student.
        Cv dummyCv = new Cv();
        when(cvDAO.getStudentCv(1)).thenReturn(dummyCv);

        uploadNewCvServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("You already have a CV saved"));
    }

    // ★ Test 5: Invalid File Format (unsupported content type)
    @Test
    public void testInvalidFileFormat() throws Exception {
        when(request.getParameter("experiences")).thenReturn("Exp");
        when(request.getParameter("programmingSkills")).thenReturn("Prog");
        when(request.getParameter("otherSkills")).thenReturn("Other");
        when(request.getPart("cv")).thenReturn(filePart);
        // Return an unsupported file type
        when(filePart.getContentType()).thenReturn("image/jpeg");

        User student = mock(User.class);
        when(student.getId()).thenReturn(1);
        when(session.getAttribute("currentUser")).thenReturn(student);

        uploadNewCvServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("File format not permitted"));
    }

    // ★ Test 6: SQLException Thrown When Retrieving the Student CV
    @Test
    public void testSQLExceptionInGetStudentCv() throws Exception {
        when(request.getParameter("experiences")).thenReturn("Exp");
        when(request.getParameter("programmingSkills")).thenReturn("Prog");
        when(request.getParameter("otherSkills")).thenReturn("Other");
        when(request.getPart("cv")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");
        when(filePart.getInputStream()).thenReturn(new ByteArrayInputStream("dummy".getBytes()));

        User student = mock(User.class);
        when(student.getId()).thenReturn(1);
        when(session.getAttribute("currentUser")).thenReturn(student);

        // Simulate a SQLException when checking for an existing CV.
        when(cvDAO.getStudentCv(1)).thenThrow(new SQLException("DB error"));

        uploadNewCvServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("DB error"));
    }

    // ★ Test 7: SQLException Thrown During the File Upload Process
    @Test
    public void testSQLExceptionInUploadNewCv() throws Exception {
        when(request.getParameter("experiences")).thenReturn("Exp");
        when(request.getParameter("programmingSkills")).thenReturn("Prog");
        when(request.getParameter("otherSkills")).thenReturn("Other");
        when(request.getPart("cv")).thenReturn(filePart);
        when(filePart.getContentType()).thenReturn("application/pdf");
        when(filePart.getInputStream()).thenReturn(new ByteArrayInputStream("dummy".getBytes()));

        User student = mock(User.class);
        when(student.getId()).thenReturn(1);
        when(session.getAttribute("currentUser")).thenReturn(student);

        // Ensure no existing CV exists.
        when(cvDAO.getStudentCv(1)).thenReturn(null);
        // Simulate an exception when uploading the file.
        doThrow(new SQLException("Upload error"))
                .when(cvDAO).uploadNewCv(any(), anyString(), anyString(), anyString(), anyString(), eq(1));

        uploadNewCvServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("Upload error"));
    }
}

