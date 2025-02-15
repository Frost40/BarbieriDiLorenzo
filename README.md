# Students&Companies (S&C) Platform

## Table of Contents
1. [Project Overview](#project-overview)
2. [Team](#team)
3. [Repository Structure](#repository-structure)
4. [Installation Instructions](#installation-instructions)
   - [Prerequisites](#prerequisites)
   - [Importing the Project into IntelliJ](#1-importing-the-project-into-intellij)
   - [Configuring Apache Tomcat in IntelliJ](#2-configuring-apache-tomcat-in-intellij)
   - [Setting Up the MySQL Database](#3-setting-up-the-mysql-database)
   - [Importing the Database Schema](#4-importing-the-database-schema)
   - [Updating Database Configuration in the Project](#5-updating-database-configuration-in-the-project)
   - [Running the Project](#6-running-the-project)
5. [Features Implemented](#features-implemented)
6. [Testing](#testing)
   - [Servlet Testing](#1-servlet-testing)
   - [DAO Testing](#2-dao-testing)
   - [Test Cases](#3-test-cases)
   - [Testing Tools](#4-testing-tools)
   - [Test Coverage](#5-test-coverage)
   - [Acceptance Testing](#6-acceptance-testing)
7. [Documentation](#documentation)
8. [Evaluation](#evaluation)
9. [License](#license)

## Project Overview

The **Students&Companies (S&C)** platform is a web application designed to match university students looking for internships with companies offering them. The platform facilitates the matching process based on students' skills, experiences, and attitudes, as well as the projects and terms offered by companies. The system also includes features for recommendation, selection processes, and monitoring of internships.

This project is part of the **Software Engineering 2** course for the academic year 2024-2025. The project is divided into two main assignments:
1. **Requirement Analysis and Specification Document (RASD)**
2. **Design Document (DD)**
3. **Implementation and Test Deliverable (ITD)**
4. **Acceptance Test Deliverable (ATD)**

This repository contains the code, documentation, and resources related to the implementation of the S&C platform.


## Team

- Flavio Di Lorenzo (flavio.dilorenzo@mail.polimi.it) (GitHub: [@Frost40](https://github.com/Frost40))
- Alessabdro Barbieri (alessandrogwyn.barbieri@mail.polimi.it) (GitHub: [@AleGB29](https://github.com/AleGB29))

## Repository Structure

The repository is organized as follows:
```
S&C-Platform/
├── Alloy/                          # Directory containing Alloy models and related files
├── Code/                           # Additional code or scripts (if any)
│   ├── src/                        # Source code directory
│   │   ├── main/                   # Main source code
│   │   │   ├── java/               # Java source code
│   │   │   │   └── polimi/SE2/SC/  # Package structure for the S&C platform
│   │   │   └── webapp/             # Web application resources (e.g., HTML, CSS, JS)
│   │   └── test/                   # Test source code
│   │       └── java/               # Java test code
│   │           └── polimi/SE2/SC/  # Package structure for the S&C platform tests
│   ├── S&C.sql                     # SQL file containing the database schema and initial data
│   └── pom.xml                     # Maven configuration file for project dependencies and build settings
├── idea/                           # IntelliJ IDEA project configuration files
├── DeliveryFolder/                 # Contains final deliverables (RASD, DD, ITD, ATD)
├── RASD/                           # Working directory for RASD (if separate from DeliveryFolder)
├── DD/                             # Working directory for DD (if separate from DeliveryFolder)
└── ITD/                            # Working directory for ITD (if separate from DeliveryFolder)
```

---

## Installation Instructions
This section provides detailed instructions for setting up and running the **Students&Companies (S&C)** platform locally. The project is implemented as a Java web application and requires the following software to be installed:

### Prerequisites

Before starting, ensure that the following software is installed on your system:

- **IntelliJ IDEA** (Ultimate edition)
- **Apache Tomcat** (Version 9 or later) – We used version 9.0.98
- **MySQL Workbench** (Version 8 or later)


### 1. Importing the Project into IntelliJ
1. **Clone the repository**:
   ```
         git clone https://github.com/Frost40/BarbieriDILorenzo.git
   ```
2. Open IntelliJ IDEA and go to File -> Open.

3. Select the project root folder and click OK to open it.

4. Configure the project as a Maven module:
      - Go to File -> Project Structure -> Modules.
      - Click the + button and select Import Module.
      - Navigate to the Code folder in the project directory and follow the import instructions.
      - Click Create, then Apply, and OK.

5. Enable Maven in the project:
      - Right-click on the pom.xml file in the project root directory.
      - Select Add as Maven Project to ensure all dependencies are downloaded.

### 2. Configuring Apache Tomcat in IntelliJ
1. Go to Run -> Edit Configurations.
2. Click + and select Tomcat Server -> Local.
3. Under the Application Server section, click Configure... and select the installed Tomcat directory.      
4. Navigate to the Deployment tab and click + -> Artifact.      
5. Select Code: war exploded and apply the changes.      
6. Under the Server tab, make sure the port is set correctly (default is 8080).      
7. Click Apply and OK.

### 3. Setting Up the MySQL Database
1. Open MySQL Workbench.

2. Create a new database connection by clicking the + icon next to MySQL Connections.

3. Enter the following details (adjust if your setup differs):
      - Connection Name: S&C_DB (or any name you prefer)
      - Hostname: localhost
      - Port: 3306
      - Username: root (or another MySQL username, if configured differently)
      - Password: Enter your MySQL root password.

4. Test the connection by clicking Test Connection.
      - If the test is successful, click OK to save the connection.
      - If it fails, double-check your hostname, port, username, and password.

### 4. Importing the Database Schema
1. Open MySQL Workbench and select the S&C_DB (or whichever connection you created).

2. Click the folder icon in the query tab (or go to File > Open SQL Script).

3. Locate the provided SQL script in your project repository and select it. The script is found in the Code folder and is called S&C.sql.

4. Review the script:
      - By default, the script creates a database named your_database_name.
      - If you want a different name, edit these lines in the script:
      ```
            CREATE DATABASE IF NOT EXISTS `your_database_name`;
            USE `your_database_name`;
      ```
      - Replace your_database_name with your chosen database name.

5. Execute the script by clicking the lightning bolt icon (the “Execute” button). MySQL Workbench will run all statements, creating the database and its tables.

6. Verify the tables:
      - In the Schemas panel on the left, locate the newly created database (you may need to refresh the panel).
      - Expand it to ensure you see all 10 tables: company, complaint, cv, feedback, internship, interviewschedule, message, questionnaire, student, and studentsinternship.

### 5. Updating Database Configuration in the Project
To ensure the application can access the database, update the following files with your MySQL credentials:

1. Modify web.xml
      1. Locate web.xml in src/main/webapp/WEB-INF/.
      2. Find the database connection parameters and update them:
      ```
            <context-param>
                <param-name>dbUrl</param-name>
                <param-value>jdbc:mysql://127.0.0.1:3306/StudentCompany?serverTimezone=UTC</param-value>
            </context-param>
            <context-param>
                <param-name>dbUser</param-name>
                <param-value>root</param-value>
            </context-param>
            <context-param>
                <param-name>dbPassword</param-name>
                <param-value>your_db_password</param-value>
            </context-param>
            <context-param>
                <param-name>dbDriver</param-name>
                <param-value>com.mysql.cj.jdbc.Driver</param-value>
            </context-param>
      ```
2. Modify TestUtils.java (for testing)
If you intend to run tests that require database interaction:
      1. Locate TestUtils.java in src/test/java/polimi/SE2/SC/.
      2. Update the MySQL connection settings in this file to match your local database configuration.

### 6. Running the Project
1. Start Apache Tomcat:
      - Go to Run -> Edit Configurations.
      - Select Tomcat Server -> Local and click Run.

2. Verify the deployment:
      - Open a browser and navigate to:

      ```
            http://localhost:8080/Code_war_exploded/
      ```

---

## Features Implemented
As a group of two students, we focused on implementing the following features:

1. Student and Company Registration:
      - Students and companies can create accounts and provide necessary information (e.g., CVs for students, internship details for companies).

2. Internship Search and Application:
      - Students can search for internships based on keywords, skills, and other criteria.
      - Students can apply for internships directly through the platform.

3. Recommendation System:
     - The platform provides recommendations to students based on their profiles and to companies based on their internship requirements.

4. Selection Process Management:
      - Companies can manage the selection process, including scheduling interviews and collecting feedback from students.

5. Monitoring and Feedback:
      - Students and companies can provide feedback on the internship process, and the platform collects statistics for future improvements.

## Testing

We thoroughly tested the S&C platform to ensure its functionality, reliability, and robustness. Our testing strategy focused on **testing all possible returns of each servlet**, which allowed us to indirectly test all methods of the DAOs (Data Access Objects). Below is an overview of our testing approach:

### 1. **Servlet Testing**
   - **Objective**: Test all possible return scenarios for each servlet to ensure proper handling of requests and responses.
   - **Approach**:
     - For each servlet, we simulated various input scenarios (e.g., valid inputs, invalid inputs, edge cases).
     - We verified that the servlet returned the correct HTTP status codes (e.g., `200 OK`, `400 Bad Request`, `500 Internal Server Error`) and responses for each scenario.
     - This approach ensured that all methods in the DAOs were indirectly tested, as the servlets rely on the DAOs for data access and manipulation.
   - **Examples**:
     - Testing user registration with valid and invalid inputs.
     - Testing internship search with different filters (e.g., by skill, location, or company).
     - Testing the recommendation system with various student and company profiles.

### 2. **DAO Testing**
   - **Objective**: Ensure that all DAO methods function correctly and interact with the database as expected.
   - **Approach**:
     - Since the DAOs were indirectly tested through the servlets, we focused on verifying that the DAOs returned the correct data or exceptions for each scenario.
     - We also tested database interactions, such as CRUD operations (Create, Read, Update, Delete), to ensure data consistency and integrity.
   - **Examples**:
     - Testing the `StudentDAO` for student registration, login, and profile updates.
     - Testing the `InternshipDAO` for adding, updating, and retrieving internship details.

### 3. **Test Cases**
   - We created a comprehensive set of test cases to cover all possible scenarios for each servlet and DAO method.
   - Each test case included:
     - **Input**: The data or request sent to the servlet.
     - **Expected Output**: The expected response or behavior.
     - **Actual Output**: The actual response or behavior observed during testing.
     - **Result**: Whether the test passed or failed.

### 4. **Testing Tools**
   - We used **JUnit** for unit testing and **Postman** for API testing to simulate HTTP requests and verify responses.
   - For database testing, we used **MySQL Workbench** to manually verify data consistency and integrity.

### 5. **Test Coverage**
   - By testing all possible returns of each servlet, we achieved **full coverage of the DAO methods**, as the servlets rely on the DAOs for all data-related operations.
   - We also ensured that edge cases and error scenarios were thoroughly tested to handle unexpected inputs gracefully.

### 6. **Acceptance Testing**:
   - We performed acceptance testing on another group's prototype and documented the results in the ATD.

## Documentation
The following documents are included in this repository:

1. RASD: Requirement Analysis and Specification Document.

2. DD: Design Document.

3. ITD: Implementation and Test Deliverable.

4. ATD: Acceptance Test Deliverable.

You can find these documents in the DeliveryFolder directory.

## Evaluation

The project was evaluated by **Professor Elisabetta Di Nitto** as part of the **Software Engineering 2** course. After a thorough review of the deliverables, including the **Requirement Analysis and Specification Document (RASD)**, **Design Document (DD)**, **Implementation and Test Deliverable (ITD)**, and **Acceptance Test Deliverable (ATD)**, our team received the following evaluation:
- **Score**: **30L/30** (Full marks **cum laude**)


This achievement reflects the hard work and dedication of the team throughout the project lifecycle.

## License
This project is licensed under the MIT License. See the LICENSE file for details.
