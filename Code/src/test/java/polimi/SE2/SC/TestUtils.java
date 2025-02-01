package polimi.SE2.SC;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {
    public ServletConfig setUp() {
        // Mock the ServletContext
        ServletContext servletContext = mock(ServletContext.class);

        // Mock the ServletConfig
        ServletConfig servletConfig = mock(ServletConfig.class);
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getInitParameter("dbUrl")).thenReturn("jdbc:mysql://127.0.0.1:3306/StudentCompany?serverTimezone=UTC");
        when(servletContext.getInitParameter("dbUser")).thenReturn("root");
        when(servletContext.getInitParameter("dbPassword")).thenReturn("password_2024");
        when(servletContext.getInitParameter("dbDriver")).thenReturn("com.mysql.cj.jdbc.Driver");

        return servletConfig;
    }
}