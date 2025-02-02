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

import polimi.SE2.SC.DAOs.ComplaintDAO;
import polimi.SE2.SC.DAOs.InternshipDAO;
import polimi.SE2.SC.DAOs.StudentHasInternshipDAO;
import polimi.SE2.SC.TestUtils;
import polimi.SE2.SC.beans.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UploadComplaintTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private Connection testConnection;

    @Mock private StudentHasInternshipDAO studentHasInternshipDAO;
    @Mock private ComplaintDAO complaintDAO;
    @Mock private InternshipDAO internshipDAO;

    private UploadComplaint uploadComplaintServlet;
    private StringWriter responseWriter;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Spy the servlet
        uploadComplaintServlet = spy(new UploadComplaint());

        ServletConfig servletConfig = testUtils.setUp();
        uploadComplaintServlet.init(servletConfig);

        // Inject mock connection
        Field connField = UploadComplaint.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connField.set(uploadComplaintServlet, testConnection);

        // Override DAOs
        doReturn(studentHasInternshipDAO).when(uploadComplaintServlet).createStudentHasInternshipDAO();
        doReturn(complaintDAO).when(uploadComplaintServlet).createComplaintDAO();
        doReturn(internshipDAO).when(uploadComplaintServlet).createInternshipDAO();

        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        when(request.getSession()).thenReturn(session);
    }

    // -------------------------------------------------------------------------
    // ★ Test 1: Student uploads a valid complaint for an ongoing internship
    // -------------------------------------------------------------------------
    @Test
    public void testStudentUploadsComplaintSuccess() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // Provide valid param
        when(request.getParameter("complaint")).thenReturn("Issue with the internship tasks");
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        // The DB has StudentHasInternship #555 => ongoing, with the same student #100
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(100);
        shi.setInternshipId(999);
        shi.setApplicationStatus("ongoing");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // Upload complaint
        doNothing().when(complaintDAO).uploadComplaint("Issue with the internship tasks", 555, true);

        // Then, retrieving open complaints
        ArrayList<Complaint> complaints = new ArrayList<>();
        Complaint c1 = new Complaint();
        c1.setId(10);
        complaints.add(c1);
        doReturn(complaints).when(complaintDAO).getOngoingInternshipOpenComplaints(555);

        uploadComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertTrue(responseWriter.toString().contains("\"openComplaints\""));
    }

    // -------------------------------------------------------------------------
    // ★ Test 2: Complaint text invalid (too short, too long, or null)
    // -------------------------------------------------------------------------
    @Test
    public void testInvalidComplaintText() throws Exception {
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        // e.g. complaint is empty
        when(request.getParameter("complaint")).thenReturn("");
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        uploadComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("Invalid complaint"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 3: Missing ongoingInternshipId
    // -------------------------------------------------------------------------
    @Test
    public void testMissingOngoingInternshipId() throws Exception {
        Student studentUser = mock(Student.class);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaint")).thenReturn("Something is wrong");
        when(request.getParameter("ongoingInternshipId")).thenReturn(null);

        uploadComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("No valid id"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 4: StudentHasInternship not found
    // -------------------------------------------------------------------------
    @Test
    public void testStudentHasInternshipNotFound() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaint")).thenReturn("Issue text");
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        doReturn(null).when(studentHasInternshipDAO).getStudentHasInternship(555);

        uploadComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("There is no ongoing internship"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 5: Wrong time to upload => applicationStatus != 'ongoing'
    // -------------------------------------------------------------------------
    @Test
    public void testWrongTimeToUpload() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaint")).thenReturn("Issue text");
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(100);
        shi.setApplicationStatus("selectionProcess");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        uploadComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("not authorized to upload a complaint"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 6: Student mismatch
    // -------------------------------------------------------------------------
    @Test
    public void testStudentMismatch() throws Exception {
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(200);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaint")).thenReturn("Issue text");
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        // DB shows #555 => ongoing => studentId=999 => mismatch
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(999);
        shi.setApplicationStatus("ongoing");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        uploadComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("you are not the student associated"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 7: If user is a company
    // -------------------------------------------------------------------------
    @Test
    public void testUserIsCompany() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(100);  // Fix: Mock getId() explicitly
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("complaint")).thenReturn("Company complaint about intern");
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        // DB => #555 => ongoing => internshipId=999
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("ongoing");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company owns #999
        ArrayList<Internship> owned = new ArrayList<>();
        Internship i999 = new Internship();
        i999.setId(999);
        owned.add(i999);

        doReturn(owned).when(internshipDAO).getCompanyInternships(100); // Use stubbed value

        // DB allows complaint
        doNothing().when(complaintDAO).uploadComplaint("Company complaint about intern", 555, false);

        ArrayList<Complaint> openComplaints = new ArrayList<>();
        openComplaints.add(new Complaint());
        doReturn(openComplaints).when(complaintDAO).getOngoingInternshipOpenComplaints(555);

        uploadComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertTrue(responseWriter.toString().contains("\"openComplaints\""));
    }


    // -------------------------------------------------------------------------
    // ★ Test 8: Company does not own the internship
    // -------------------------------------------------------------------------
    @Test
    public void testCompanyNotOwner() throws Exception {
        Company companyUser = mock(Company.class);
        when(companyUser.getId()).thenReturn(300);
        when(session.getAttribute("currentUser")).thenReturn(companyUser);

        when(request.getParameter("complaint")).thenReturn("Issue text");
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        // DB => #555 => ongoing => internshipId=999
        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setInternshipId(999);
        shi.setApplicationStatus("ongoing");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        // The company #300 owns some other internship => mismatch
        ArrayList<Internship> owned = new ArrayList<>();
        Internship i888 = new Internship();
        i888.setId(888);
        owned.add(i888);
        doReturn(owned).when(internshipDAO).getCompanyInternships(300);

        uploadComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("you are not the company responsible"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 9: SQLException uploading complaint
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionUploadComplaint() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaint")).thenReturn("Issue text");
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(100);
        shi.setInternshipId(999);
        shi.setApplicationStatus("ongoing");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        doThrow(new SQLException("DB error uploading complaint"))
                .when(complaintDAO).uploadComplaint("Issue text", 555, true);

        uploadComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error uploading complaint"));
    }

    // -------------------------------------------------------------------------
    // ★ Test 10: SQLException retrieving open complaints
    // -------------------------------------------------------------------------
    @Test
    public void testSQLExceptionRetrievingOpenComplaints() throws Exception {
        // Student user
        Student studentUser = mock(Student.class);
        when(studentUser.getId()).thenReturn(100);
        when(session.getAttribute("currentUser")).thenReturn(studentUser);

        when(request.getParameter("complaint")).thenReturn("Issue text");
        when(request.getParameter("ongoingInternshipId")).thenReturn("555");

        StudentHasInternship shi = new StudentHasInternship();
        shi.setId(555);
        shi.setStudentId(100);
        shi.setInternshipId(999);
        shi.setApplicationStatus("ongoing");
        doReturn(shi).when(studentHasInternshipDAO).getStudentHasInternship(555);

        doNothing().when(complaintDAO).uploadComplaint("Issue text", 555, true);

        doThrow(new SQLException("DB error retrieving open complaints"))
                .when(complaintDAO).getOngoingInternshipOpenComplaints(555);

        uploadComplaintServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("DB error retrieving open complaints"));
    }
}
