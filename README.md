# Students&Companies (S&C) Platform

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

## Installation Instructions

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

## License
This project is licensed under the MIT License. See the LICENSE file for details.
