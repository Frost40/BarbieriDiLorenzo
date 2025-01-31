function MyInternshipsPage(user, pageOrchestrator) {
    this.pageOrchestrator = pageOrchestrator;

    // Getting references of the HTML elements
    let myInternshipPageDiv = document.getElementById('my-internship-page-div');
    let studentListDiv;
    let internshipListColumn = document.getElementById('internship-list-column');
    let studentListColumn = document.getElementById('student-list-column');
    let informationSpace = document.getElementById('information-space');
    let internshipDescriptionDiv = document.getElementById("internship-description");
    let feedbacksDiv = document.getElementById("feedbacks");
    let otherDataDiv = document.getElementById('other-data');
    let questionnairesDiv = document.getElementById('questionnairesDiv');
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
    let applicationStatus;

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
                if (internshipData.studentHasInternship && internshipData.studentHasInternship.id) {
                    internshipItem.addEventListener("click", function () {
                        applicationStatus = internshipData.studentHasInternship.applicationStatus;
                        openInformationPage(internshipData.studentHasInternship.id);
                    });
                } else {
                    console.error("No valid studentHasInternship found for internshipId:", internshipId);
                }
            } else if (isCompany) {
                internshipItem.addEventListener("click", function () {
                    manageStudentListColumn(true);
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
                applicationStatus = studentEntry.studentHasInternship.applicationStatus;
                openInformationPage(studentEntry.studentHasInternship.id);
            });

            studentListDiv.appendChild(studentItem);
        });
    }

    function openInformationPage(studentHasInternshipId) {
        console.log(studentHasInternshipId);
        console.log(applicationStatus);

        var url = "OpenMyInternshipInformationSpace?studentHasInternshipId=" + encodeURIComponent(studentHasInternshipId);

        makeCall("GET", url, null, function (x) {
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

                        populateCommonElements(studentHasInternshipId);

                        switch (applicationStatus) {
                            case "evaluation":
                                populateForEvaluation(studentHasInternshipId);
                                break;

                            case "selectionProcess":
                                populateForSelectionProcess(studentHasInternshipId);
                                break;

                            case "ongoing":
                                populateForOngoing();
                                break;

                            case "ended":
                                populateForEnded();
                                break;

                            case "rejected":
                                populateForRejected();
                                break;
                        }

                        break;

                    case 400: // bad request
                        showErrorAlert(message);
                        break;

                    case 401: // unauthorized
                        showErrorAlert(message);
                        break;

                    case 500: // server error
                        showErrorAlert(message);
                        break;

                    default:
                        pageOrchestrator.showPage("home page");
                        break;
                }
            }
        });
    }

    function populateCommonElements(studentHasInternshipId) {
        internshipDescriptionDiv.innerHTML = ""; // Clear previous content
        feedbacksDiv.innerHTML = ""; // Clear previous feedbacks

        // Find the internship associated with the given studentHasInternshipId
        let internshipData = null;
        internshipsMap.forEach((data, internshipId) => {
            if (isStudent) {
                if (data.studentHasInternship.id === studentHasInternshipId) {
                    internshipData = data;
                }
            } else {
                data.students.forEach(studentEntry => {
                    if (studentEntry.studentHasInternship.id === studentHasInternshipId) {
                        internshipData = data;
                    }
                });
            }
        });

        if (!internshipData) return; // If no internship found, exit

        // Populate Internship Description
        let internship = internshipData.internship;
        internshipDescriptionDiv.innerHTML = `
            <h2>${internship.name}</h2>
            <p><strong>Description:</strong> ${internship.description}</p>
            <p><strong>Rating:</strong> ${internship.rating} ⭐</p>
        `;

        // Populate Feedbacks Section
        if (internship.feedbacks && internship.feedbacks.length > 0) {
            internship.feedbacks.forEach(feedback => {
                let feedbackItem = document.createElement("div");
                feedbackItem.className = "feedback-item";
                feedbackItem.innerHTML = `
                <p><strong>Rating:</strong> ${feedback.rating} ⭐</p>
                <p><strong>Comment:</strong> ${feedback.text}</p>
            `;
                feedbacksDiv.appendChild(feedbackItem);
            });
        } else {
            feedbacksDiv.innerHTML = "<p>No feedbacks available.</p>";
        }
    }

    function populateForEvaluation(studentHasInternshipId, isStudent) {
        const studentInfoDiv = document.createElement('div');
        studentInfoDiv.id = 'student-info';

        if (isStudent) {
            // Se isStudent è true, mostra il messaggio di valutazione
            studentInfoDiv.style.textAlign = 'center'; // Centra il testo nel div
            studentInfoDiv.innerHTML = `
            <p>Your application is being evaluated by the company</p>
        `;
        } else {
            // Aggiungi le informazioni dello studente al div
            studentInfoDiv.innerHTML = `
            <h2>Informazioni Studente</h2>
            <p><strong>Nome:</strong> ${student.name}</p>
            <p><strong>Cognome:</strong> ${student.surname}</p>
            <p><strong>Email:</strong> ${student.email}</p>
            <p><strong>Nazionalità:</strong> ${student.nationality}</p>
            <p><strong>Città di Nascita:</strong> ${student.cityOfBirth}</p>
            <a href="${cv.file}" download="cv_${student.name}_${student.surname}.pdf">Scarica CV</a>
        `;

            // Crea un link per il download
            const link = document.createElement('a');
            link.href = `data:application/pdf;base64,${cv.file}`; // Imposta l'URL del link
            link.download = 'cv_studente.pdf'; // Nome del file da scaricare
            document.body.appendChild(link); // Aggiungi il link al DOM

            studentInfoDiv.appendChild(link);

            // Crea i bottoni "Accept" e "Reject"
            const acceptButton = document.createElement('button');
            acceptButton.textContent = 'Accept';
            acceptButton.className = 'action-button accept'; // Assegna una classe CSS
            acceptButton.addEventListener('click', function() {
                console.log('Accepted:', student.name);
                manageStudentApplication(studentHasInternshipId, true);
            });

            const rejectButton = document.createElement('button');
            rejectButton.textContent = 'Reject';
            rejectButton.className = 'action-button reject'; // Assegna una classe CSS
            rejectButton.addEventListener('click', function() {
                console.log('Rejected:', student.name);
                manageStudentApplication(studentHasInternshipId, false);
            });

            // Aggiungi i bottoni al div delle informazioni dello studente
            studentInfoDiv.appendChild(acceptButton);
            studentInfoDiv.appendChild(rejectButton);
        }

        // Aggiungi il nuovo div all'elemento 'other-data'
        otherDataDiv.appendChild(studentInfoDiv);
    }

    function populateForSelectionProcess(studentHasInternshipId) {
        populateQuestionnaires(studentHasInternshipId);
        populateInterviews(studentHasInternshipId);
    }

    function populateQuestionnaires(studentHasInternshipId) {
        // Create the first div for questionnaires

        // Check if the div already exists
        if (!questionnairesDiv) {
            questionnairesDiv = document.createElement('div');
            questionnairesDiv.id = 'questionnaires-div';
            questionnairesDiv.className = 'data-container'; // Assign a class name
        }

        // Check if questionnaires are empty
        if (questionnaires.length === 0) {
            const emptyMessage = document.createElement('p');
            emptyMessage.textContent = "No questionnaires available.";
            emptyMessage.style.textAlign = 'center'; // Center the message
            questionnairesDiv.appendChild(emptyMessage);
        } else {
            // Populate the first div with data from the questionnaires variable
            questionnaires.forEach((questionnaire, index) => {
                const questionnaireItem = document.createElement('p');
                questionnaireItem.textContent = `Questionnaire ${index + 1}: ${questionnaire}`; // Sequential numbering
                questionnairesDiv.appendChild(questionnaireItem);

                // Create buttons for students
                if (isStudent) {
                    const downloadButton = createButton('Download', '📥', () => {
                        console.log(`Downloading questionnaire ${index + 1}`);
                        // Implement download logic here
                    });
                    const uploadButton = createButton('Upload', '📤', () => {
                        console.log(`Uploading questionnaire ${index + 1}`);
                        // Implement upload logic here
                    });
                    questionnairesDiv.appendChild(downloadButton);
                    questionnairesDiv.appendChild(uploadButton);
                } else {
                    // Create buttons for non-students
                    const approveButton = createButton('Approve', '✅', () => {
                        console.log(`Approving questionnaire ${index + 1}`);
                        // Implement approve logic here
                    });
                    const rejectButton = createButton('Reject', '❌', () => {
                        console.log(`Rejecting questionnaire ${index + 1}`);
                        // Implement reject logic here
                    });
                    questionnairesDiv.appendChild(approveButton);
                    questionnairesDiv.appendChild(rejectButton);
                }
            });
        }

        // Add upload questionnaire button for non-students
        if (isCompany) {
            // Create the form element
            const uploadForm = document.createElement('form');
            uploadForm.id = 'uploadQuestionnaireForm'; // Set an ID for the form
            uploadForm.method = 'POST'; // Set the method
            uploadForm.enctype = 'multipart/form-data'; // Set enctype for file uploads

            // Create a hidden input for studentHasInternshipId
            const studentInput = document.createElement('input');
            studentInput.type = 'hidden';
            studentInput.name = 'studentHasInternshipId';
            studentInput.value = studentHasInternshipId; // Set the value to the ID
            uploadForm.appendChild(studentInput);

            // Create a file input element
            const fileInput = document.createElement('input');
            fileInput.type = 'file';
            fileInput.name = 'questionnaire'; // Backend must recognize this field
            fileInput.accept = '.pdf, .doc, .docx'; // Accept specific file types (optional)
            fileInput.required = true; // Make it required
            fileInput.style.display = 'none'; // Hide the file input
            uploadForm.appendChild(fileInput);

            // Create the upload button
            const uploadButton = createButton('Upload Questionnaire', '📤', () => {
                // Trigger the file input click
                fileInput.click();
            });

            // Handle the file selection
            fileInput.addEventListener('change', (event) => {
                const file = event.target.files[0]; // Get the selected file
                if (file) {
                    console.log('Selected file:', file.name);
                    // Call the uploadQuestionnaire function with the form as an argument
                    uploadQuestionnaire(uploadForm, studentHasInternshipId);
                } else {
                    console.log('No file selected.');
                }
            });

            // Append the button to the questionnairesDiv
            questionnairesDiv.appendChild(uploadButton);
            // Append the form to the questionnairesDiv (hidden)
            questionnairesDiv.appendChild(uploadForm);
        }

        // Assuming 'otherDataDiv' is already defined and refers to the existing div
        // Append both divs to the existing other-data div
        otherDataDiv.appendChild(questionnairesDiv);
    }

    function populateInterviews(studentHasInternshipId) {
        // Create the second div for interviews scheduled
        const interviewsDiv = document.createElement('div');
        interviewsDiv.id = 'interviews-div';
        interviewsDiv.className = 'data-container'; // Assign the same class name

        // Check if interviewsScheduled are empty
        if (interviewsScheduled.length === 0) {
            const emptyMessage = document.createElement('p');
            emptyMessage.textContent = "No interviews scheduled.";
            emptyMessage.style.textAlign = 'center'; // Center the message
            interviewsDiv.appendChild(emptyMessage);
        } else {
            // Populate the second div with data from the interviewsScheduled variable
            interviewsScheduled.forEach(interview => {
                const interviewItem = document.createElement('p');
                interviewItem.textContent = `Interview Scheduled: ${interview.date}`; // Accessing the date property
                interviewsDiv.appendChild(interviewItem);

                // Create reject button for each interview
                const rejectButton = createButton('Reject', '❌', () => {
                    console.log(`Rejecting interview scheduled on ${interview.date}`);
                    // Implement reject logic here
                });
                interviewsDiv.appendChild(rejectButton);
            });
        }

        // Add select and send date and time button for non-students
        if (isCompany) {
            const selectDateButton = document.createElement('button');
            selectDateButton.textContent = 'Select and Send Date/Time 📅';
            selectDateButton.className = 'action-button'; // Add a class for styling
            selectDateButton.addEventListener('click', () => {
                // Create a date-time input
                const dateTimeInput = document.createElement('input');
                dateTimeInput.type = 'datetime-local';
                dateTimeInput.style.marginTop = '10px'; // Add some margin for better spacing

                // Create a send button
                const sendButton = createButton('Send', '📤', () => {
                    const selectedDateTime = dateTimeInput.value;
                    console.log(`Sending date/time: ${selectedDateTime}`);
                    scheduleInterview(studentHasInternshipId, selectedDateTime);
                });

                // Append the input and send button to the interviewsDiv
                interviewsDiv.appendChild(dateTimeInput);
                interviewsDiv.appendChild(sendButton);
            });
            interviewsDiv.appendChild(selectDateButton);
        }

        // Assuming 'otherDataDiv' is already defined and refers to the existing div
        // Append both divs to the existing other-data div
        otherDataDiv.appendChild(interviewsDiv);
    }

    function populateForOngoing() {

    }

    function populateForEnded() {

    }

    function populateForRejected() {

    }

    function manageStudentApplication(studentHasInternshipId, decision) {
        let tempFormEvaluation = document.createElement('form');

        // Create hidden input for message
        const messageInput = document.createElement('input');
        messageInput.type = 'hidden';
        messageInput.name = 'studentHasInternshipId';
        messageInput.value = studentHasInternshipId;
        tempFormEvaluation.appendChild(messageInput);

        // Create hidden input for internshipId
        const internshipInput = document.createElement('input');
        internshipInput.type = 'hidden';
        internshipInput.name = 'evaluation';
        internshipInput.value = decision;
        tempFormEvaluation.appendChild(internshipInput);

        makeCall("POST", "ManageStudentApplication", tempFormEvaluation, function (x) {
            if (x.readyState === XMLHttpRequest.DONE) {
                var message = x.responseText;
                switch (x.status) {
                    case 200:
                        // Code to execute in case of successful callBack
                        removeDiv('student-info');
                        feedbacksDiv.innerHTML = '';
                        internshipDescriptionDiv.innerHTML = '';
                        pageOrchestrator.showPage('my internships page')

                        break;

                    case 400: // bad request
                        showErrorAlert(message);
                        break;

                    case 401: // unauthorized
                        showErrorAlert(message);
                        break;

                    case 500: // server error
                        showErrorAlert(message);
                        break;

                    default:
                        pageOrchestrator.showPage("home page");
                        break;
                }
            }
        });
    }

    function uploadQuestionnaire(form, studentHasInternshipId) {

        makeCall("POST", "UploadQuestionnaire", form, function (x) {
            if (x.readyState === XMLHttpRequest.DONE) {
                var message = x.responseText;
                switch (x.status) {
                    case 200:
                        var jsonObject = JSON.parse(message);

                        // Code to execute in case of successful callBack
                        questionnaires = jsonObject.questionnaires;
                        clearDiv('questionnaires-div');
                        populateQuestionnaires(studentHasInternshipId);

                        break;

                    case 400: // bad request
                        showErrorAlert(message);
                        break;

                    case 401: // unauthorized
                        showErrorAlert(message);
                        break;

                    case 500: // server error
                        showErrorAlert(message);
                        break;

                    default:
                        pageOrchestrator.showPage("home page");
                        break;
                }
            }
        });
    }

    function scheduleInterview(studentHasInternshipId, interviewTimestamp) {
        let tempFormScheduleInterview = document.createElement('form');

        // Create hidden input for message
        const messageInput = document.createElement('input');
        messageInput.type = 'hidden';
        messageInput.name = 'studentHasInternshipId';
        messageInput.value = studentHasInternshipId;
        tempFormScheduleInterview.appendChild(messageInput);

        // Create hidden input for internshipId
        const internshipInput = document.createElement('input');
        internshipInput.type = 'hidden';
        internshipInput.name = 'date';
        internshipInput.value = interviewTimestamp;
        tempFormScheduleInterview.appendChild(internshipInput);

        makeCall("POST", "ScheduleInterview", tempFormScheduleInterview, function (x) {
            if (x.readyState === XMLHttpRequest.DONE) {
                var message = x.responseText;
                switch (x.status) {
                    case 200:
                        var jsonObject = JSON.parse(message);

                        // Code to execute in case of successful callBack
                        interviewsScheduled = jsonObject.interviewSchedules;
                        clearDiv('interviews-div');
                        populateInterviews(studentHasInternshipId);

                        break;

                    case 400: // bad request
                        showErrorAlert(message);
                        break;

                    case 401: // unauthorized
                        showErrorAlert(message);
                        break;

                    case 500: // server error
                        showErrorAlert(message);
                        break;

                    default:
                        pageOrchestrator.showPage("home page");
                        break;
                }
            }
        });
    }


    function removeDiv(divId) {
        const div = document.getElementById(divId);

        if (div) {
            div.remove();
            console.log(`Il div con id "${divId}" è stato rimosso.`);
        } else {
            console.log(`Il div con id "${divId}" non esiste.`);
        }
    }

    function clearDiv(divId) {
        const div = document.getElementById(divId);

        if (div) {
            div.innerHTML = '';
        } else {
            console.log(`Il div con id "${divId}" non esiste.`);
        }
    }

    // Function to hide the page
    this.hide = function() {
        manageStudentListColumn(false)
        myInternshipPageDiv.style.display = "none";
    };
}