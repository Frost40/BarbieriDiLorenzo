package polimi.SE2.SC.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import polimi.SE2.SC.DAOs.CompanyDAO;
import polimi.SE2.SC.DAOs.StudentDAO;
import polimi.SE2.SC.beans.Company;
import polimi.SE2.SC.beans.Student;
import polimi.SE2.SC.utils.ConnectionHandler;
import polimi.SE2.SC.utils.ServiceMessage;

@WebServlet("/SignUpManager")
@MultipartConfig
public class SignUpManager extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public SignUpManager() {
        super();
    }

    @Override
    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init() throws ServletException {
        ServletContext servletContext = getServletContext();
        this.connection = ConnectionHandler.getConnection(servletContext);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //Gets the parameters of the request and verifies if they are in the correct format (length and syntax)
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String repeatedPassword = request.getParameter("repeat_pwd");
        String userType = request.getParameter("userType");
        String name = request.getParameter("name");

        String surname = null;
        String nationality = null;
        String cityOfBirth = null;

        String mainOffice = null;
        String offices = null;

        ServiceMessage returnedServiceMessage = null;
        StudentDAO studentDAO = null;
        CompanyDAO companyDAO = null;


        if (userType.equals("student")) {
            surname = request.getParameter("surname");
            nationality = request.getParameter("nationality");
            cityOfBirth = request.getParameter("cityOfBirth");

            studentDAO = new StudentDAO(connection);
            returnedServiceMessage = readyForRegistration(email, password, repeatedPassword, name, surname, nationality, cityOfBirth, studentDAO);
        }
        else {
            mainOffice = request.getParameter("mainOffice");
            offices = request.getParameter("offices");

            companyDAO = new CompanyDAO(connection);
            returnedServiceMessage = readyForRegistration(email, password, repeatedPassword, name, mainOffice, offices, companyDAO);
        }

        switch(returnedServiceMessage.getResult()) {
            case "error" -> {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println(returnedServiceMessage.getText());
                return;
            }
            case "warning" -> {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().println(returnedServiceMessage.getText());
                return;

            }
            case "unauthorized" -> {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().println(returnedServiceMessage.getText());
                return;
            }
            case "success" -> {
                //Adding user's data to database and returning user's id automatically generated
                int userId = 0;
                try {
                    // At this point the user is ready to be registered
                    if (userType.equals("student")) {
                        userId = studentDAO.registerStudent(email, password,name, surname, nationality, cityOfBirth);
                    }
                    else {
                        userId = companyDAO.registerCompany(email, password, name, mainOffice, offices);
                    }

                // If an error occurred during the process the user is redirected to errorPage
                } catch (SQLException e) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().println(e.getMessage());
                    return;
                }

                //In case the system failed to retrieve user's id it redirects to errorPage
                if (userId == 0) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().println("An error occurred while retrieving user's id");
                    return;
                }

                response.setStatus(HttpServletResponse.SC_OK);
            }
        }
    }

    private ServiceMessage readyForRegistration(String email, String password, String repeatedPassword, String name, String surname, String nationality, String cityOfBirth, StudentDAO studentDAO) throws ServletException, IOException {
        ServiceMessage serviceMessageToReturn = new ServiceMessage();

        //Verifying that there are no null parameters
        if(email == null || password == null || repeatedPassword == null || name == null || surname == null || nationality == null || cityOfBirth == null) {
            serviceMessageToReturn.setResult("warning");
            serviceMessageToReturn.setText("Some data is missing from the sign up module!");
            return serviceMessageToReturn;
        }

        //Checking if the inserted email is actually a valid email address and assuring that its length is <= 100
        if (!isAnEmail(email) || email.length() > 100 || email.length() <= 0) {
            serviceMessageToReturn.setResult("warning");
            serviceMessageToReturn.setText("The email address is NOT valid!");
            return serviceMessageToReturn;
        }

        //Checking if the inserted name string has correct length (1-45)
        if (name.length() <= 0 || name.length() > 45) {
            serviceMessageToReturn.setResult("warning");
            serviceMessageToReturn.setText("Invalid name (a valid name has more than one character, less than 45 and has no special character)!");
            return serviceMessageToReturn;
        }

        //Checking if the inserted surname string has correct length (1-45)
        if (surname.length() <= 0 || surname.length() > 45) {
            serviceMessageToReturn.setResult("warning");
            serviceMessageToReturn.setText("Invalid surname (a valid surname has more than one character, less than 45 and has no special character)!");
            return serviceMessageToReturn;
        }
        //Checking if the inserted nationality string has correct length (1-45)
        if (nationality.length() <= 0 || nationality.length() > 45) {
            serviceMessageToReturn.setResult("warning");
            serviceMessageToReturn.setText("Invalid nationality (a valid nationality has more than one character, less than 45 and has no special character)!");
            return serviceMessageToReturn;
        }

        //Checking if the inserted cityOfBirth string has correct length (1-45)
        if (cityOfBirth.length() <= 0 || cityOfBirth.length() > 45) {
            serviceMessageToReturn.setResult("warning");
            serviceMessageToReturn.setText("Invalid cityOfBirth (a valid cityOfBirth has more than one character, less than 45 and has no special character)!");
            return serviceMessageToReturn;
        }

        //Checking if the inserted strings (PASSWORD and REPEAT_PWD) have the correct length (1-10) and equal
        if (password.length() <= 0 || password.length() > 10) {
            serviceMessageToReturn.setResult("warning");
            serviceMessageToReturn.setText("Invalid password (a valid password has more than one character and less than 10)!");
            return serviceMessageToReturn;
        }

        if (!password.equals(repeatedPassword)) {
            serviceMessageToReturn.setResult("warning");
            serviceMessageToReturn.setText("Password and repeat password field not equal!");
            return serviceMessageToReturn;
        }

        //Checking uniqueness of email
        Student user = null;
        try {
            user = studentDAO.getStudentByEmail(email);

            //If an error occurred during the process the user is redirected to errorPage
        } catch (SQLException e) {
            serviceMessageToReturn.setResult("error");
            serviceMessageToReturn.setText(e.getMessage());
            return serviceMessageToReturn;
        }

        if(user != null) {
            serviceMessageToReturn.setResult("unauthorized");
            serviceMessageToReturn.setText("Email already in use!");
            return serviceMessageToReturn;
        }

        serviceMessageToReturn.setResult("success");
        return serviceMessageToReturn;

    }

    private ServiceMessage readyForRegistration(String email, String password, String repeatedPassword, String name, String mainOffice, String offices, CompanyDAO companyDAO) throws ServletException, IOException {
        ServiceMessage serviceMessageToReturn = new ServiceMessage();

        //Verifying that there are no null parameters
        if(email == null || password == null || repeatedPassword == null || name == null || mainOffice == null || offices == null) {
            serviceMessageToReturn.setResult("warning");
            serviceMessageToReturn.setText("Some data is missing from the sign up module!");
            return serviceMessageToReturn;
        }

        //Checking if the inserted email is actually a valid email address and assuring that its length is <= 100
        if (!isAnEmail(email) || email.length() > 100 || email.length() <= 0) {
            serviceMessageToReturn.setResult("warning");
            serviceMessageToReturn.setText("The email address is NOT valid!");
            return serviceMessageToReturn;
        }

        //Checking if the inserted name string has correct length (1-45)
        if (name.length() <= 0 || name.length() > 45) {
            serviceMessageToReturn.setResult("warning");
            serviceMessageToReturn.setText("Invalid name (a valid name has more than one character, less than 45 and has no special character)!");
            return serviceMessageToReturn;
        }

        //Checking if the inserted mainOffice string has correct length (1-45)
        if (mainOffice.length() <= 0 || mainOffice.length() > 45) {
            serviceMessageToReturn.setResult("warning");
            serviceMessageToReturn.setText("Invalid mainOffice (a valid mainOffice has more than one character, less than 45 and has no special character)!");
            return serviceMessageToReturn;
        }

        //Checking if the inserted offices string has correct length (1-255)
        if (offices.length() <= 0 || offices.length() > 255) {
            serviceMessageToReturn.setResult("warning");
            serviceMessageToReturn.setText("Invalid offices (a valid offices has more than one character, less than 255 and has no special character)!");
            return serviceMessageToReturn;
        }

        //Checking if the inserted strings (PASSWORD and REPEAT_PWD) have the correct length (1-10) and equal
        if (password.length() <= 0 || password.length() > 10) {
            serviceMessageToReturn.setResult("warning");
            serviceMessageToReturn.setText("Invalid password (a valid password has more than one character and less than 10)!");
            return serviceMessageToReturn;
        }

        if (!password.equals(repeatedPassword)) {
            serviceMessageToReturn.setResult("warning");
            serviceMessageToReturn.setText("Password and repeat password field not equal!");
            return serviceMessageToReturn;
        }

        //Checking uniqueness of email
        Company user = null;
        try {
            user = companyDAO.getCompanyByEmail(email);

            //If an error occurred during the process the user is redirected to errorPage
        } catch (SQLException e) {
            serviceMessageToReturn.setResult("error");
            serviceMessageToReturn.setText(e.getMessage());
            return serviceMessageToReturn;
        }

        if(user != null) {
            serviceMessageToReturn.setResult("unauthorized");
            serviceMessageToReturn.setText("Email already in use!");
            return serviceMessageToReturn;
        }

        serviceMessageToReturn.setResult("success");
        return serviceMessageToReturn;

    }

    private boolean isAnEmail(String emailAddress) {
        String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

        Pattern pattern = Pattern.compile(EMAIL_REGEX);
        Matcher matcher = pattern.matcher(emailAddress);
        return matcher.matches();
    }

    public static boolean containsSpecialCharacters(String input) {
        String specialCharactersPattern = "[^a-zA-Z0-9]";

        return input != null && input.matches(".*" + specialCharactersPattern + ".*");
    }

}
