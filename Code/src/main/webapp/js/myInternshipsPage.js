function MyInternshipsPage(user, pageOrchestrator) {
    this.pageOrchestrator = pageOrchestrator;

    // Getting references of the HTML elements
    let myInternshipPageDiv = document.getElementById('my-internship-page-div');
    let studentListDiv;
    let internshipListColumn = document.getElementById('internship-list-column');
    let studentListColumn = document.getElementById('student-list-column');
    let informationSpace = document.getElementById('information-space');
    let homePageButton = document.getElementById('home-page-btn');
    let searchPageButton = document.getElementById('search-page-btn');
    let logoutButton = document.getElementById('log-out-btn');

    // Variables of the page
    let internshipsData = null; // Will store the entire data object
    let currentInternship = null; // Will store the currently selected internship (for students)
    let currentCompanyInternship = null; // Will store the currently selected internship (for companies)
    let studentsList = null; // Will store the list of students for a company internship
    let internshipsMap;
    let isStudent;
    let isCompany;
    let questionnaires;
    let complaints;
    let interviewsScheduled;
    let feedbacks;
    let student;
    let cv;

    // Adding listeners
    homePageButton.addEventListener('click', function() {
        pageOrchestrator.showPage("home page", "placeHolder", "placeHolder", "placeHolder");
    });
    logoutButton.addEventListener('click', function() {
        pageOrchestrator.showPage("main page", "placeHolder", "placeHolder", "placeHolder");
    });

    this.open = function () {
        myInternshipPageDiv.style.display = "";
        // Construct query string
        var url = "MyInternshipsManager";
        makeCall("GET", url, null, function (x) {
            if (x.readyState === XMLHttpRequest.DONE) {
                var message = x.responseText;
                switch (x.status) {
                    case 200:
                        var jsonObject = JSON.parse(message);

                        // Code to execute in case of successful callBack
                        internshipsData = jsonObject.data; // Save the data in the variable

                        internshipsMap = new Map();

                        // Controlla se l'utente è uno studente o una company
                        isStudent = internshipsData.length > 0 && internshipsData[0].studentHasInternship;
                        isCompany = !isStudent;

                        internshipsData.forEach(entry => {
                            if (isStudent) {
                                // Mappa per studenti: internshipId -> { internship, studentHasInternship }
                                internshipsMap.set(entry.internship.id, {
                                    internship: entry.internship,
                                    studentHasInternship: entry.studentHasInternship
                                });
                            } else {
                                // Mappa per company: internshipId -> { internship, students: [{ student, studentHasInternship }] }
                                internshipsMap.set(entry.internship.id, {
                                    internship: entry.internship,
                                    students: entry.students.map(studentEntry => ({
                                        student: studentEntry.student,
                                        studentHasInternship: studentEntry.studentHasInternship
                                    }))
                                });
                            }
                        });
                        populateInternshipList();

                        console.log(internshipsData);
                        console.log(internshipsMap);
                        break;

                    case 400: // bad request
                        pageOrchestrator.showPage("home page");
                        showErrorAlert(message);
                        break;

                    case 401: // unauthorized
                        pageOrchestrator.showPage("home page");
                        showErrorAlert(message);
                        break;

                    case 500: // server error
                        pageOrchestrator.showPage("home page");
                        showErrorAlert(message);
                        break;

                    default:
                        pageOrchestrator.showPage("home page");
                        break;
                }
            }
        });
    }

    // Methods called internally from the js class
    // Funzione per creare/rimuovere dinamicamente student-list-column
    function manageStudentListColumn(isCompany) {
        const container = document.querySelector(".container");
        const existingStudentList = document.getElementById("student-list-column");

        if (isCompany) {
            if (!existingStudentList) {
                studentListDiv = document.createElement("div");
                studentListDiv.className = "student-list-column";
                studentListDiv.id = "student-list-column";
                container.insertBefore(studentListDiv, document.getElementById("information-space"));
            }
        } else {
            if (existingStudentList) {
                existingStudentList.remove();
            }
        }
    }

    function populateInternshipList() {
        internshipListColumn.innerHTML = ""; // Clear previous content

        internshipsMap.forEach((internshipData, internshipId) => {
            let internshipItem = document.createElement("div");
            internshipItem.textContent = internshipData.internship.name; // Display the internship name
            internshipItem.className = "internship-item"; // Add a CSS class for styling

            // Add click listener based on user type
            if (isStudent) {
                internshipItem.addEventListener("click", function () {
                    openInformationPage(internshipsData.studentHasInternship.id);
                });
            } else if (isCompany) {
                internshipItem.addEventListener("click", function () {
                    manageStudentListColumn(true)
                    populateStudentColumn(internshipId);
                });
            }

            internshipListColumn.appendChild(internshipItem);
        });
    }

    function populateStudentColumn(internshipId) {
        studentListDiv.innerHTML = ""; // Clear previous content

        let internshipData = internshipsMap.get(internshipId);
        if (!internshipData || !internshipData.students) return;

        internshipData.students.forEach(studentEntry => {
            let studentItem = document.createElement("div");
            studentItem.textContent = studentEntry.student.name; // Display student name
            studentItem.className = "student-item"; // Add a CSS class for styling

            // Add click listener to open student info page
            studentItem.addEventListener("click", function () {
                openInformationPage(studentEntry.studentHasInternship.id);
            });

            studentListDiv.appendChild(studentItem);
        });
    }

    function openInformationPage(studentHasInternshipId) {
        console.log(studentHasInternshipId);

        makeCall("POST", "MyInternshipsManager", null, function (x) {
            if (x.readyState === XMLHttpRequest.DONE) {
                var message = x.responseText;
                switch (x.status) {
                    case 200:
                        var jsonObject = JSON.parse(message);

                        // Code to execute in case of successful callBack
                        questionnaires = jsonObject.questionnaires || [];
                        complaints = jsonObject.complaints || [];
                        interviewsScheduled = jsonObject.interviewSchedules || [];
                        feedbacks = jsonObject.feedbacks || [];
                        student = jsonObject.student || null;
                        cv = jsonObject.cv || null;

                        console.log(questionnaires);
                        console.log(complaints);
                        console.log(interviewsScheduled);
                        console.log(feedbacks);
                        console.log(student);
                        console.log(cv);

                        break;

                    case 400: // bad request
                        pageOrchestrator.showPage("home page");
                        showErrorAlert(message);
                        break;

                    case 401: // unauthorized
                        pageOrchestrator.showPage("home page");
                        showErrorAlert(message);
                        break;

                    case 500: // server error
                        pageOrchestrator.showPage("home page");
                        showErrorAlert(message);
                        break;

                    default:
                        pageOrchestrator.showPage("home page");
                        break;
                }
            }
        });
    }

    // Function to hide the page
    this.hide = function() {
        manageStudentListColumn(false)
        myInternshipPageDiv.style.display = "none";
    };
}