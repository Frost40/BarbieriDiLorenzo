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
    let questionnairesDiv = document.getElementById('questionnaires-div');
    let interviewsDiv = document.getElementById('interviews-div');
    let complaintsDiv = document.getElementById('complaints-div');
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
    searchPageButton.addEventListener('click', function() {
        pageOrchestrator.showPage("search page", "placeHolder", "placeHolder", "placeHolder");
    });
    logoutButton.addEventListener('click', function() {
        pageOrchestrator.showPage("main page", "placeHolder", "placeHolder", "placeHolder");
    });

    this.open = function () {
        myInternshipPageDiv.style.display = "";
        clearDiv('internship-description');
        clearDiv('feedbacks');
        clearDiv('other-data');
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
        clearDiv(otherDataDiv.id);
        removeDiv('end-message-div');
        removeDiv('questionnaires-div');
        removeDiv('complaints-div');

        questionnaires = [];
        complaints = [];
        interviewsScheduled = [];
        feedbacks = [];
        student = null;
        cv = null;

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
                                populateForOngoing(studentHasInternshipId);
                                break;

                            case "ended":
                                populateForEnded(studentHasInternshipId);
                                break;

                            case "rejected":
                                populateForRejected(studentHasInternshipId);
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
        let internship = internshipData.internship;

        // Populate Internship Description
        populateInternshipDescription(internship, studentHasInternshipId);

        // Populate Feedbacks Section
        populateFeedbacks(studentHasInternshipId);
    }

    function populateInternshipDescription(internship, studentHasInternshipId) {
        internshipDescriptionDiv.innerHTML = `
            <h2>${internship.name}</h2>
            <p><strong>Description:</strong> ${internship.description}</p>
            <p><strong>Rating:</strong> ${internship.rating} ⭐</p>
        `;

        if (isCompany) {
            // Create and append the Start or End button based on application status
            let actionButton;

            if (applicationStatus === "selectionProcess") {
                actionButton = createButton('btn btn-medium btn-gossamer', 'Start', '▶️', () => {
                    // Implement the logic for starting the internship
                    console.log(`Starting internship with ID: ${studentHasInternshipId}`);
                    startInternship(studentHasInternshipId);
                });

                let rejectButton = createButton('btn btn-medium btn-gossamer', 'Reject', '❌', () => {
                    // Implement the logic for starting the internship
                    console.log(`Rejecting internship with ID: ${studentHasInternshipId}`);
                    manageStudentApplication(studentHasInternshipId, false);
                });

                internshipDescriptionDiv.appendChild(rejectButton);
            } else if (applicationStatus === "ongoing") {
                actionButton = createButton('btn btn-medium btn-gossamer', 'End', '⏹️', () => {
                    // Implement the logic for ending the internship
                    console.log(`Ending internship with ID: ${studentHasInternshipId}`);
                    endInternship(studentHasInternshipId);
                });
            }

            // Append the action button to the internship description div if it exists
            if (actionButton) {
                internshipDescriptionDiv.appendChild(actionButton);
            }
        }
    }

    function populateFeedbacks(studentHasInternshipId) {
        if (feedbacks && feedbacks.length > 0) {
            feedbacks.forEach(feedback => {
                let feedbackItem = document.createElement("div");
                feedbackItem.className = "feedback-item";

                // Create a container for the feedback line
                const feedbackLine = document.createElement("div");
                feedbackLine.className = "feedback-line";

                const userSpan = document.createElement("span");
                userSpan.className = "feedback-user";
                userSpan.textContent = `user #${feedback.studentHasInternshipId}:`;

                // Create a span for the comment and rating
                const commentRatingSpan = document.createElement("span");
                commentRatingSpan.className = "feedback-comment-rating";

                // Create a span for the comment
                const commentSpan = document.createElement("span");
                commentSpan.className = "feedback-comment";
                commentSpan.textContent = feedback.text;

                // Create a span for the rating
                const ratingSpan = document.createElement("span");
                ratingSpan.className = "feedback-rating";
                ratingSpan.textContent = `Rating: ${feedback.rating} ⭐`;

                // Append comment and rating to the commentRatingSpan
                commentRatingSpan.appendChild(commentSpan);
                commentRatingSpan.appendChild(ratingSpan);

                // Append user and commentRatingSpan to the feedback line
                feedbackLine.appendChild(userSpan);
                feedbackLine.appendChild(commentRatingSpan);

                // Append the feedback line to the feedback item
                feedbackItem.appendChild(feedbackLine);

                // Append the feedback item to the feedbacks div
                feedbacksDiv.appendChild(feedbackItem);
            });
        } else {
            feedbacksDiv.innerHTML = "<p>No feedbacks available.</p>";
        }
    }


    function populateForEvaluation(studentHasInternshipId) {
        const studentInfoDiv = document.createElement('div');
        studentInfoDiv.id = 'student-info';
        console.log(isStudent);

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
            `;

            // Check if cv is not null
            if (cv) {
                // Create the link element
                const cvLink = document.createElement('a');
                cvLink.href = cv.file;
                cvLink.download = `cv_${student.name}_${student.surname}.pdf`;
                cvLink.textContent = 'Download CV';

                // Add a click event listener to call a function
                cvLink.addEventListener('click', function() {
                    immediateDownloadCv(cv);
                });

                // Append the link to the studentInfoDiv
                studentInfoDiv.appendChild(cvLink);
            }

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

    function immediateDownloadCv(cv) {
        const fileExtension = cv.fileType;
        console.log('File extension: ' + fileExtension);

        // Map extensions to MIME types
        const mimeTypes = {
            'pdf': 'application/pdf',
            'doc': 'application/msword',
            'docx': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'txt': 'text/plain',
            // Add more mappings as needed
        };

        // Determine MIME type, default to 'application/octet-stream'
        const mimeType = mimeTypes[fileExtension] || 'application/octet-stream';

        // Create a Blob from the Base64 string
        const byteCharacters = atob(cv.file); // Split to get the Base64 part
        const byteNumbers = new Array(byteCharacters.length);
        for (let i = 0; i < byteCharacters.length; i++) {
            byteNumbers[i] = byteCharacters.charCodeAt(i);
        }
        const byteArray = new Uint8Array(byteNumbers);
        const blob = new Blob([byteArray], {type: mimeType});

        // Create a URL for the Blob
        const fileUrl = URL.createObjectURL(blob);

        // Create a download link and trigger the download
        const a = document.createElement('a');
        a.href = fileUrl;
        a.download = `${student.name}_cv.${fileExtension}`; // Set the file name with extension
        document.body.appendChild(a); // Append to the body
        a.click(); // Trigger the download
        document.body.removeChild(a); // Clean up

        // Optionally revoke the object URL after some time
        setTimeout(() => URL.revokeObjectURL(fileUrl), 100);
    }

    function populateForSelectionProcess(studentHasInternshipId) {
        removeDiv('questionnaires-div');
        removeDiv('interviews-div');

        populateQuestionnaires(studentHasInternshipId);
        populateInterviews(studentHasInternshipId);
    }

    function populateQuestionnaires(studentHasInternshipId) {
        questionnairesDiv = document.getElementById('questionnaires-div');
        // Check if the div already exists
        if (!questionnairesDiv) {
            // Create the first div for questionnaires
            questionnairesDiv = document.createElement('div');
            questionnairesDiv.id = 'questionnaires-div';
            questionnairesDiv.className = 'data-container'; // Assign a class name
        }

        clearDiv('questionnaires-div');

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
                questionnaireItem.textContent = `Questionnaire ${index + 1}`; // Sequential numbering
                questionnairesDiv.appendChild(questionnaireItem);

                // Create buttons for students
                if (isStudent) {
                    const downloadButton = createButton('action-button', 'Download', '📥', () => {
                        console.log(`Downloading questionnaire ${index + 1}`);
                        immediateQuestionnaireDownload(questionnaire, index);
                    });

                    // Create the form element
                    const uploadForm = document.createElement('form');
                    uploadForm.id = 'completedQuestionnaireForm'; // Set an ID for the form
                    uploadForm.method = 'POST'; // Set the method
                    uploadForm.enctype = 'multipart/form-data'; // Set enctype for file uploads

                    // Create a hidden input for studentHasInternshipId
                    const studentInput = document.createElement('input');
                    studentInput.type = 'hidden';
                    studentInput.name = 'studentHasInternshipId';
                    studentInput.value = studentHasInternshipId; // Set the value to the ID
                    uploadForm.appendChild(studentInput);

                    // Create a hidden input for studentHasInternshipId
                    const questionnaireIdInput = document.createElement('input');
                    questionnaireIdInput.type = 'hidden';
                    questionnaireIdInput.name = 'questionnaireId';
                    questionnaireIdInput.value = questionnaire.id; // Set the value to the ID
                    uploadForm.appendChild(questionnaireIdInput);

                    // Create a file input element
                    const fileInput = document.createElement('input');
                    fileInput.type = 'file';
                    fileInput.name = 'questionnaire'; // Backend must recognize this field
                    fileInput.accept = '.pdf, .doc, .docx'; // Accept specific file types (optional)
                    fileInput.required = true; // Make it required
                    fileInput.style.display = 'none'; // Hide the file input
                    uploadForm.appendChild(fileInput);

                    const uploadButton = createButton('action-button', 'Upload', '📤', () => {
                        console.log(`Uploading questionnaire ${index + 1}`);

                        // Trigger the file input click
                        fileInput.click();
                    });

                    // Handle the file selection
                    fileInput.addEventListener('change', (event) => {
                        const file = event.target.files[0]; // Get the selected file
                        if (file) {
                            console.log('Selected file:', file.name);
                            // Call the uploadQuestionnaire function with the form as an argument
                            completeQuestionnaire(uploadForm, studentHasInternshipId);
                        } else {
                            console.log('No file selected.');
                        }
                    });

                    questionnairesDiv.appendChild(downloadButton);
                    // Append the button to the questionnairesDiv
                    questionnairesDiv.appendChild(uploadButton);
                    // Append the form to the questionnairesDiv (hidden)
                    questionnairesDiv.appendChild(uploadForm);
                } else {
                    // Create buttons for non-students
                    const downloadButtonCompany = createButton('action-button', 'Download', '📥', () => {
                        console.log(`Downloading questionnaire ${index + 1}`);
                        immediateQuestionnaireDownload(questionnaire, index);
                    });

                    if (questionnaire.evaluation == null) {
                        const approveButton = createButton('action-button', 'Approve', '✅', () => {
                            console.log(`Approving questionnaire ${index + 1}`);
                            evaluateQuestionnaire(studentHasInternshipId, questionnaire.id, true);
                        });
                        const rejectButton = createButton('action-button', 'Reject', '❌', () => {
                            console.log(`Rejecting questionnaire ${index + 1}`);
                            evaluateQuestionnaire(studentHasInternshipId, questionnaire.id, false);
                        });

                        questionnairesDiv.appendChild(approveButton);
                        questionnairesDiv.appendChild(rejectButton);
                    }

                    questionnairesDiv.appendChild(downloadButtonCompany);
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
            const uploadButton = createButton('action-button', 'Upload Questionnaire', '📤', () => {
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
        otherDataDiv.appendChild(questionnairesDiv);
    }

    function populateInterviews(studentHasInternshipId) {
        interviewsDiv = document.getElementById('interviews-div');
        // Check if the div already exists
        if (!interviewsDiv) {
            // Create the second div for interviews scheduled
            interviewsDiv = document.createElement('div');
            interviewsDiv.id = 'interviews-div';
            interviewsDiv.className = 'data-container'; // Assign the same class name
        } else {
            clearDiv('interviews-div');
        }

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

                // Check if the interview has been rejected
                if (interview.hasBeenRejected) {
                    const rejectedText = document.createElement('span');
                    rejectedText.textContent = ' Rejected'; // Text to display
                    rejectedText.style.color = 'red'; // Set the text color to red
                    interviewItem.appendChild(rejectedText); // Append the rejected text to the interview item
                }

                interviewsDiv.appendChild(interviewItem);

                if (isStudent && !interview.hasBeenRejected) {
                    // Create reject button for each interview
                    const rejectButton = createButton('action-button', 'Reject', '❌', () => {
                        console.log(`Rejecting interview scheduled on ${interview.date}`);
                        rejectInterviewDate(studentHasInternshipId, interview.id);
                    });
                    interviewsDiv.appendChild(rejectButton);
                }
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
                const sendButton = createButton('action-button', 'Send', '📤', () => {
                    const selectedDateTime = dateTimeInput.value;
                    const formattedDateTime = selectedDateTime.replace('T', ' '); // Replace 'T' with a space
                    console.log(`Sending date/time: ${formattedDateTime}`);
                    scheduleInterview(studentHasInternshipId, formattedDateTime);
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

    function populateForOngoing(studentHasInternshipId) {
        complaintsDiv = document.getElementById('complaints-div');
        // Check if the div already exists
        if (!complaintsDiv) {
            // Create a new div to hold the complaints
            complaintsDiv = document.createElement('div');
            complaintsDiv.className = 'complaints-container';
            complaintsDiv.id = 'complaint-div';
        }
        // Clear previous content
        clearDiv("complaints-div");

        // Check if there are complaints
        if (complaints.length === 0) {
            // Display a message if there are no complaints
            const noComplaintsMessage = document.createElement('div');
            noComplaintsMessage.className = 'no-complaints-message';
            noComplaintsMessage.textContent = 'No complaints to display.';
            complaintsDiv.appendChild(noComplaintsMessage);
        } else {
            // Iterate over each complaint and create a block for it
            complaints.forEach(complaint => {
                // Create a block for each complaint
                const complaintBlock = document.createElement('div');
                complaintBlock.className = 'complaint-block';

                // Create a scrollable div for the complaint text
                const complaintTextDiv = document.createElement('div');
                complaintTextDiv.className = 'complaint-text';
                complaintTextDiv.textContent = complaint.text;

                // Append the text and button to the complaint block
                complaintBlock.appendChild(complaintTextDiv);

                // Create a container for the buttons
                const buttonContainer = document.createElement('div');
                buttonContainer.className = 'button-container';

                if (!((isStudent && complaint.closedByStudent) || (isCompany && complaint.closedByCompany))) {
                    // Create a Close button using the createButton function
                    const closeButton = createButton('close-button btn-gossamer', 'Close', '❌', () => {
                        closeComplaint(complaint.id, studentHasInternshipId);
                    });
                    buttonContainer.appendChild(closeButton);
                }

                if (complaint.closedByStudent || complaint.closedByCompany) {
                    // Create a reject closure button using the createButton function
                    const rejectButton = createButton('close-button btn-gossamer', 'Reject closure', '👎', () => {
                        rejectComplaintClosure(complaint.id, studentHasInternshipId);
                    });
                    buttonContainer.appendChild(rejectButton);
                }

                // Append the button container to the complaint block
                complaintBlock.appendChild(buttonContainer);

                // Append the complaint block to the complaints container
                complaintsDiv.appendChild(complaintBlock);
            });
        }

        // Create a form for uploading a complaint
        const uploadForm = document.createElement('form');
        uploadForm.className = 'upload-form';
        uploadForm.onsubmit = (event) => {
            event.preventDefault(); // Prevent default form submission
            uploadComplaint(uploadForm, studentHasInternshipId); // Call the uploadComplaint function
            complaintInput.value = ''; // Clear the input field after submission
        };

        // Create a text input for the complaint
        const complaintInput = document.createElement('input');
        complaintInput.type = 'text';
        complaintInput.name = 'complaint'; // Name attribute for the input
        complaintInput.maxLength = 255; // Set maximum characters
        complaintInput.placeholder = 'Enter your complaint here...';
        complaintInput.required = true; // Make it required

        // Create a hidden input for ongoingInternshipId
        const internshipIdInput = document.createElement('input');
        internshipIdInput.type = 'hidden'; // Set type to hidden
        internshipIdInput.name = 'ongoingInternshipId'; // Set the name attribute
        internshipIdInput.value = studentHasInternshipId; // Set the value to the student's internship ID

        // Create the Upload complaint button
        const uploadButton = createButton('upload-button btn-gossamer', 'Upload complaint', '📤', () => {
            // This function will be called on form submission
        });

        // Append the input and button to the form
        uploadForm.appendChild(complaintInput);
        uploadForm.appendChild(uploadButton);
        uploadForm.appendChild(internshipIdInput);

        // Append the form to the complaints container
        complaintsDiv.appendChild(uploadForm);

        // Append the complaints container to the other-data div
        otherDataDiv.appendChild(complaintsDiv);
    }

    function populateForEnded(studentHasInternshipId) {
        // Clear any existing content in the other-data div
        clearDiv(otherDataDiv.id)

        // Create a new div for the message
        const messageDiv = document.createElement('div');
        messageDiv.id = 'end-message-div'
        messageDiv.className = 'ended-message'; // Add a class for styling
        messageDiv.textContent = 'The internship period has ended';

        // Append the message div to the other-data div
        otherDataDiv.appendChild(messageDiv);

        if (isStudent) {
            // Create the feedback form
            const feedbackForm = document.createElement('form');
            feedbackForm.id = 'feedbackForm';

            // Create a text area for feedback
            const feedbackTextArea = document.createElement('textarea');
            feedbackTextArea.name = 'text'; // Set the name attribute
            feedbackTextArea.placeholder = 'Enter your feedback (max 255 characters)';
            feedbackTextArea.maxLength = 255; // Set maximum length
            feedbackTextArea.style.width = '100%'; // Full width

            console.log('Feedback Text:', feedbackTextArea.value);
            console.log('Internship ID:', studentHasInternshipId);

            // Create a container for the textarea and rating selector
            const feedbackRatingContainer = document.createElement('div');
            feedbackRatingContainer.className = 'feedback-rating-container';

            // Append the textarea to the container
            feedbackRatingContainer.appendChild(feedbackTextArea);

            // Create a container for the rating selector
            const ratingContainer = document.createElement('div');
            ratingContainer.className = 'rating-container';

            // Create a label for the rating selector
            const ratingLabel = document.createElement('label');
            ratingLabel.textContent = 'Rating (1-10): ';
            ratingContainer.appendChild(ratingLabel);

            // Create a rating selector
            const ratingSelect = document.createElement('select');
            ratingSelect.name = 'rating'; // Set the name attribute
            for (let i = 1; i <= 10; i++) {
                const option = document.createElement('option');
                option.value = i;
                option.textContent = i;
                ratingSelect.appendChild(option);
            }
            ratingContainer.appendChild(ratingSelect);
            feedbackRatingContainer.appendChild(ratingContainer); // Append the rating container to the feedback-rating-container

            // Append the feedback-rating-container to the form
            feedbackForm.appendChild(feedbackRatingContainer);

            // Create a hidden input for ongoingInternshipId
            const internshipIdInput = document.createElement('input');
            internshipIdInput.type = 'hidden'; // Set type to hidden
            internshipIdInput.name = 'ongoingInternshipId'; // Set the name attribute
            internshipIdInput.value = studentHasInternshipId; // Set the value to the student's internship ID
            feedbackForm.appendChild(internshipIdInput);

            // Create a submit button
            const submitButton = document.createElement('button');
            submitButton.type = 'submit'; // Set type to submit
            submitButton.textContent = 'Send Feedback';
            feedbackForm.appendChild(submitButton);

            // Append the form to the feedbacks div
            feedbacksDiv.appendChild(feedbackForm);

            // Handle form submission
            feedbackForm.addEventListener('submit', (event) => {
                event.preventDefault(); // Prevent default form submission
                console.log('Feedback Text:', feedbackTextArea.value);
                console.log('Rating:', ratingSelect.value);
                console.log('Internship ID:', studentHasInternshipId);
                uploadFeedback(feedbackForm, studentHasInternshipId);
                feedbackTextArea.value = '';
            });
        }
    }

    function populateForRejected(studentHasInternshipId) {
        removeDiv('questionnaires-div');
        removeDiv('interviews-div');

        questionnairesDiv = document.getElementById('questionnaires-div');
        // Check if the div already exists
        if (!questionnairesDiv) {
            // Create the first div for questionnaires
            questionnairesDiv = document.createElement('div');
            questionnairesDiv.id = 'questionnaires-div';
            questionnairesDiv.className = 'data-container'; // Assign a class name
        }

        clearDiv('questionnaires-div');

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
                questionnaireItem.textContent = `Questionnaire ${index + 1}`; // Sequential numbering
                questionnairesDiv.appendChild(questionnaireItem);

                // Create buttons for students
                if (isStudent) {
                    const downloadButton = createButton('action-button', 'Download', '📥', () => {
                        console.log(`Downloading questionnaire ${index + 1}`);
                        immediateQuestionnaireDownload(questionnaire, index);
                    });


                    questionnairesDiv.appendChild(downloadButton);
                    // Append the button to the questionnairesDiv

                } else {
                    // Create buttons for non-students
                    const downloadButtonCompany = createButton('action-button', 'Download', '📥', () => {
                        console.log(`Downloading questionnaire ${index + 1}`);
                        immediateQuestionnaireDownload(questionnaire, index);
                    });

                    questionnairesDiv.appendChild(downloadButtonCompany);
                }
            });
        }
        populateInterviews(studentHasInternshipId);
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
                        applicationStatus = null;
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

    function completeQuestionnaire(form, studentHasInternshipId) {
        makeCall("POST", "CompleteQuestionnaire", form, function (x) {
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

    function evaluateQuestionnaire(studentHasInternshipId, questionnaireId, evaluation) {
        let tempFormEvaluateQuestionnaire = document.createElement('form');

        // Create hidden input for message
        const studentHasInternshipIdInput = document.createElement('input');
        studentHasInternshipIdInput.type = 'hidden';
        studentHasInternshipIdInput.name = 'studentHasInternshipId';
        studentHasInternshipIdInput.value = studentHasInternshipId;
        tempFormEvaluateQuestionnaire.appendChild(studentHasInternshipIdInput);

        // Create hidden input for internshipId
        const questionnaireIdInput = document.createElement('input');
        questionnaireIdInput.type = 'hidden';
        questionnaireIdInput.name = 'questionnaireId';
        questionnaireIdInput.value = questionnaireId;
        tempFormEvaluateQuestionnaire.appendChild(questionnaireIdInput);

        // Create hidden input for internshipId
        const evaluationInput = document.createElement('input');
        evaluationInput.type = 'hidden';
        evaluationInput.name = 'evaluation';
        evaluationInput.value = evaluation;
        tempFormEvaluateQuestionnaire.appendChild(evaluationInput);

        makeCall("POST", "EvaluateQuestionnaire", tempFormEvaluateQuestionnaire, function (x) {
            if (x.readyState === XMLHttpRequest.DONE) {
                var message = x.responseText;
                switch (x.status) {
                    case 200:

                        // Code to execute in case of successful callBack
                        let evaluatedQuestionnaire = questionnaires.find(questionnaire => questionnaire.id === questionnaireId);
                        evaluatedQuestionnaire.evalaution = evaluation;

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

    function rejectInterviewDate(studentHasInternshipId, interviewScheduleId) {
        let tempFormRejectInterview = document.createElement('form');

        // Create hidden input for message
        const studentHasInternshipIdInput = document.createElement('input');
        studentHasInternshipIdInput.type = 'hidden';
        studentHasInternshipIdInput.name = 'studentHasInternshipId';
        studentHasInternshipIdInput.value = studentHasInternshipId;
        tempFormRejectInterview.appendChild(studentHasInternshipIdInput);

        // Create hidden input for internshipId
        const interviewIdInput = document.createElement('input');
        interviewIdInput.type = 'hidden';
        interviewIdInput.name = 'interviewScheduleId';
        interviewIdInput.value = interviewScheduleId;
        tempFormRejectInterview.appendChild(interviewIdInput);

        makeCall("POST", "RejectInterviewDate", tempFormRejectInterview, function (x) {
            if (x.readyState === XMLHttpRequest.DONE) {
                var message = x.responseText;
                switch (x.status) {
                    case 200:

                        // Code to execute in case of successful callBack
                        let rejectedInterview = interviewsScheduled.find(interview => interview.id === interviewScheduleId);
                        rejectedInterview.hasBeenRejected = true;

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

    function startInternship(studentHasInternshipId) {
        let tempFormStartInternship = document.createElement('form');

        // Create hidden input for student-internship association id
        const studentHasInternshipIdInput = document.createElement('input');
        studentHasInternshipIdInput.type = 'hidden';
        studentHasInternshipIdInput.name = 'studentHasInternshipId';
        studentHasInternshipIdInput.value = studentHasInternshipId;
        tempFormStartInternship.appendChild(studentHasInternshipIdInput);


        makeCall("POST", "StartInternship", tempFormStartInternship, function (x) {
            if (x.readyState === XMLHttpRequest.DONE) {
                var message = x.responseText;
                switch (x.status) {
                    case 200:

                        // Code to execute in case of successful callBack
                        applicationStatus = "ongoing";
                        removeDiv(questionnairesDiv.id);
                        removeDiv(interviewsDiv.id);
                        openInformationPage(studentHasInternshipId);

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

    function uploadComplaint(form, studentHasInternshipId) {
        makeCall("POST", "UploadComplaint", form, function (x) {
            if (x.readyState === XMLHttpRequest.DONE) {
                var message = x.responseText;
                switch (x.status) {
                    case 200:
                        var jsonObject = JSON.parse(message);

                        // Code to execute in case of successful callBack
                        complaints = jsonObject.openComplaints;
                        clearDiv(complaintsDiv.id);
                        populateForOngoing(studentHasInternshipId);

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

    function closeComplaint(complaintId, studentHasInternshipId) {
        let tempFormCloseComplaint = document.createElement('form');

        // Create hidden input for student-internship association id
        const complaintIdInput = document.createElement('input');
        complaintIdInput.type = 'hidden';
        complaintIdInput.name = 'complaintId';
        complaintIdInput.value = complaintId;
        tempFormCloseComplaint.appendChild(complaintIdInput);


        makeCall("POST", "CloseComplaint", tempFormCloseComplaint, function (x) {
            if (x.readyState === XMLHttpRequest.DONE) {
                var message = x.responseText;
                switch (x.status) {
                    case 200:
                        var jsonObject = JSON.parse(message);

                        // Code to execute in case of successful callBack
                        complaints = jsonObject.openComplaints;
                        clearDiv(complaintsDiv.id);
                        populateForOngoing(studentHasInternshipId);

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

    function rejectComplaintClosure(complaintId, studentHasInternshipId) {
        let tempFormRejectCloseComplaint = document.createElement('form');

        // Create hidden input for student-internship association id
        const complaintIdInput = document.createElement('input');
        complaintIdInput.type = 'hidden';
        complaintIdInput.name = 'complaintId';
        complaintIdInput.value = complaintId;
        tempFormRejectCloseComplaint.appendChild(complaintIdInput);

        makeCall("POST", "RejectComplaintClosure", tempFormRejectCloseComplaint, function (x) {
            if (x.readyState === XMLHttpRequest.DONE) {
                var message = x.responseText;
                switch (x.status) {
                    case 200:
                        // Code to execute in case of successful callBack
                        let closedComplaint = complaints.find(complaint => complaint.id === complaintId);
                        closedComplaint.closeByStudent = false;
                        closedComplaint.closeByStudent = false;

                        clearDiv(complaintsDiv.id);
                        populateForOngoing(studentHasInternshipId);
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

    function endInternship(studentHasInternshipId) {
        let tempFormEndInternship = document.createElement('form');

        // Create hidden input for student-internship association id
        const studentHasInternshipIdInput = document.createElement('input');
        studentHasInternshipIdInput.type = 'hidden';
        studentHasInternshipIdInput.name = 'studentHasInternshipId';
        studentHasInternshipIdInput.value = studentHasInternshipId;
        tempFormEndInternship.appendChild(studentHasInternshipIdInput);


        makeCall("POST", "EndInternship", tempFormEndInternship, function (x) {
            if (x.readyState === XMLHttpRequest.DONE) {
                var message = x.responseText;
                switch (x.status) {
                    case 200:

                        // Code to execute in case of successful callBack
                        applicationStatus = "ended";
                        removeDiv(complaintsDiv.id);
                        openInformationPage(studentHasInternshipId);

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

    function uploadFeedback(form, studentHasInternshipId) {
        makeCall("POST", "UploadFeedback", form, function (x) {
            if (x.readyState === XMLHttpRequest.DONE) {
                var message = x.responseText;
                switch (x.status) {
                    case 200:
                        var jsonObject = JSON.parse(message);

                        // Code to execute in case of successful callBack
                        feedbacks = jsonObject.feedbacks;
                        clearDiv('feedbacks');
                        populateFeedbacks(studentHasInternshipId);
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

    function immediateQuestionnaireDownload(questionnaire, index) {
        // Extract file extension
        const fileExtension = questionnaire.fileType;
        console.log('File extension: ' + fileExtension);

        // Map extensions to MIME types
        const mimeTypes = {
            'pdf': 'application/pdf',
            'doc': 'application/msword',
            'docx': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'txt': 'text/plain',
            // Add more mappings as needed
        };

        // Determine MIME type, default to 'application/octet-stream'
        const mimeType = mimeTypes[fileExtension] || 'application/octet-stream';

        // Create a Blob from the Base64 string
        const byteCharacters = atob(questionnaire.file); // Split to get the Base64 part
        const byteNumbers = new Array(byteCharacters.length);
        for (let i = 0; i < byteCharacters.length; i++) {
            byteNumbers[i] = byteCharacters.charCodeAt(i);
        }
        const byteArray = new Uint8Array(byteNumbers);
        const blob = new Blob([byteArray], { type: mimeType });

        // Create a URL for the Blob
        const fileUrl = URL.createObjectURL(blob);

        // Create a download link and trigger the download
        const a = document.createElement('a');
        a.href = fileUrl;
        a.download = `Questionnaire${index + 1}.${fileExtension}`; // Set the file name with extension
        document.body.appendChild(a); // Append to the body
        a.click(); // Trigger the download
        document.body.removeChild(a); // Clean up

        // Optionally revoke the object URL after some time
        setTimeout(() => URL.revokeObjectURL(fileUrl), 100);
    }

    // Function to hide the page
    this.hide = function() {
        manageStudentListColumn(false)
        myInternshipPageDiv.style.display = "none";
    };
}