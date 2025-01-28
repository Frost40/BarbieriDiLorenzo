function HomePage(user, pageOrchestrator) {
    this.pageOrchestrator = pageOrchestrator;

    // **Variables to store db data** //
    var cv;
    var internships;
    var Messages;
    var Complaints;
    var Feedbacks;
    var user_class;
    let homePageDiv = document.getElementById('home-page-div')
    // **Sidebar Variables** //
    var username = document.getElementById("username");
    var email = document.getElementById("email");
    var notificationsDropdownButton = document.getElementById("notifications-dropdown-btn");
    var notificationsContainer = document.getElementById("dynamic-notifications");
    var ComplaintsDropdownButton = document.getElementById("complaints-dropdown-btn");
    var ComplaintsContainer = document.getElementById("dynamic-complaints");
    var RecommendationDropdownButton = document.getElementById("recommendations-dropdown-btn");
    var RecommendationsContainer = document.getElementById("dynamic-recommendations");
    // **My Cv Variables** //
    var cvActionBtn = document.getElementById("cv-action-btn");
    var  experiencesInput = document.getElementById("experiences-input");
    var  programmingSkillsInput = document.getElementById("programming-skills-input");
    var  otherSkillsInput = document.getElementById("other-skills-input");
    var cvFileInput = document.getElementById("cv-file-input");
    var uploadCvBtn = document.getElementById("upload-cv-btn");
    var selectedFileName = document.getElementById("selected-file-name");
    const readOnlyTextareas = [experiencesInput, programmingSkillsInput, otherSkillsInput];
    // **Bottom Navigation Variables** //
    var homePageBtn = document.getElementById("home_page-btn");
    var myInternshipsBtn = document.getElementById("my-internships-btn");
    var searchBtn = document.getElementById("search-btn");
    var logoutBtn = document.getElementById("logout-btn");

    // Variable used in Cv editing/saving process
    let isEditing = false;
    let selectedFile = null;
    let hasCv = false; // Indicates if the user already has a CV
    let currentCvId = null; // Stores the CV ID if exists
    // **Event Listeners** //

    // SideBar Event Listeners
    notificationsDropdownButton.addEventListener("click", () => {
        // Toggle the dropdown visibility
        notificationsDropdownButton.classList.toggle("active");
        if (notificationsContainer.style.display === "block") {
            notificationsContainer.style.display = "none";
        } else {
            notificationsContainer.style.display = "block";
        }
    });
    ComplaintsDropdownButton.addEventListener("click", () => {

        ComplaintsDropdownButton.classList.toggle("active");
        // Check if the ComplaintsContainer is currently visible
        if (ComplaintsContainer.style.display === "block") {
            // If visible, hide it
            ComplaintsContainer.style.display = "none";
        } else {
            if (ComplaintsContainer.children.length === 0) {
                // If empty, insert the "No current complaints" message
                const noComplaintsMsg = document.createElement('p');
                noComplaintsMsg.textContent = "No current complaints.";
                noComplaintsMsg.style.padding = "10px";
                noComplaintsMsg.style.color = "#555"; // Optional: Styling the message
                noComplaintsMsg.id = "no-complaints-msg"; // Assign an ID for future reference if needed

                ComplaintsContainer.appendChild(noComplaintsMsg);
            }

            // Show the ComplaintsContainer
            ComplaintsContainer.style.display = "block";
        }
    });
    RecommendationDropdownButton.addEventListener("click", () => {
        showInfoAlert("Coming soon","This feature is not available yet.");
    });

    // CV Action Button Event Listener
    cvActionBtn.addEventListener('click', function() {
        if (!isEditing) {
            // Switch to editing mode
            isEditing = true;

            // Determine the action based on whether the user has a CV
            if (hasCv) {
                cvActionBtn.textContent = 'Save CV';
            } else {
                cvActionBtn.textContent = 'Save CV';
            }

            // Make textareas editable
            experiencesInput.readOnly = false;
            programmingSkillsInput.readOnly = false;
            otherSkillsInput.readOnly = false;

            // Enable file input and upload button
            cvFileInput.disabled = false;
            uploadCvBtn.disabled = false;

            // Optionally, focus the first textarea
            experiencesInput.focus();
        } else {
            // If we are currently editing, save the CV
            saveCv();
        }
    });

    // My Cv Listeners
    readOnlyTextareas.forEach(textarea => {
        textarea.addEventListener('click', function() {
            if (this.readOnly) {
                promptUserAction();
            }
        });
    });
    // Upload CV Button Event Listener
    uploadCvBtn.addEventListener('click', function () {
        cvFileInput.click();
    });

    // File Input Change Event Listener
    cvFileInput.addEventListener('change', function (event) {
        if (cvFileInput.files.length > 0) {
            const file = cvFileInput.files[0];
            const allowedTypes = [
                'application/pdf',
                'application/msword',
                'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
                'text/plain'
            ];

            // Validate MIME type
            if (!allowedTypes.includes(file.type)) {
                showErrorAlert("Invalid File Type", "Please upload a PDF, Word (.doc/.docx), or plain text (.txt) file.");
                // Reset the file input
                cvFileInput.value = "";
                selectedFileName.textContent = '';
                selectedFile = null;
                return;
            }

            // Optional: Check file size (e.g., limit to 5MB)
            const maxSizeInBytes = 5 * 1024 * 1024; // 5MB
            if (file.size > maxSizeInBytes) {
                showErrorAlert("File Too Large", "Please upload a file smaller than 5MB.");
                // Reset the file input
                cvFileInput.value = "";
                selectedFileName.textContent = '';
                selectedFile = null;
                return;
            }

            // If validation passes
            selectedFile = file;
            selectedFileName.textContent = selectedFile.name;
        } else {
            selectedFile = null;
            selectedFileName.textContent = '';
        }
    });

    //Function to prompt user when clicking on read-only textareas
    function promptUserAction() {
        if (!isEditing) {
            showInfoAlert("Modify Cv","This field is read-only. Please click the 'Create New CV' or 'Modify CV' button to edit your CV.");
        }
        // If already editing, no need to prompt
    }
    // Bottom navigation bar event listeners
    myInternshipsBtn.addEventListener("click", () => {
        this.pageOrchestrator.showPage('my internships page', 'placeholder1', 'placeholder2', 'placeholder3');
    });
    searchBtn.addEventListener("click", () => {
        pageOrchestrator.showPage('search page', 'placeholder1', 'placeholder2', 'placeholder3');
    });
    logoutBtn.addEventListener("click", () => {
        pageOrchestrator.showPage('main page', 'placeholder1', 'placeholder2', 'placeholder3');
    });

    //** Series of functions utilized to populate the UI **//

    function populateUserInfo(user_class) {
        username.textContent = user_class.name;
        email.textContent = user_class.email;
    }
    function populateCv() {
        if (cv) {
            // **Populate Textareas with CV Data**
            const maxItemsPerLine = 10; // Adjust this number as needed
            experiencesInput.value = joinWithSpacesAndNewlines(cv.experiences, maxItemsPerLine);
            programmingSkillsInput.value = joinWithSpacesAndNewlines(cv.programmingSkills, maxItemsPerLine);
            otherSkillsInput.value = joinWithSpacesAndNewlines(cv.otherSkills, maxItemsPerLine);

            // **Populate Existing File Information**
            const existingFileInfo = document.getElementById("existing-file-info");
            if (existingFileInfo) {
                if (cv.file) {
                    // Determine the MIME type based on file extension (optional)
                    const fileExtension = cv.fileName ? cv.fileName.split('.').pop().toLowerCase() : 'pdf';
                    let mimeType = 'application/pdf'; // Default MIME type
                    switch (fileExtension) {
                        case 'doc':
                        case 'docx':
                            mimeType = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
                            break;
                        case 'txt':
                            mimeType = 'text/plain';
                            break;
                        // Add more cases if needed
                    }

                    // Create a Blob from the Base64 string
                    const byteCharacters = atob(cv.file);
                    const byteNumbers = new Array(byteCharacters.length);
                    for (let i = 0; i < byteCharacters.length; i++) {
                        byteNumbers[i] = byteCharacters.charCodeAt(i);
                    }
                    const byteArray = new Uint8Array(byteNumbers);
                    const blob = new Blob([byteArray], { type: mimeType });

                    // Create a URL for the Blob
                    const fileUrl = URL.createObjectURL(blob);

                    // Set the inner HTML with a download link
                    existingFileInfo.innerHTML = `
                    <p>Current CV: 
                        <a href="${fileUrl}" download="${cv.fileName || 'CV'}">${cv.fileName || 'Download CV'}</a>
                    </p>
                `;
                } else {
                    existingFileInfo.textContent = "No CV file uploaded.";
                }
            } else {
                console.warn('Existing file info element not found.');
            }
        }
        else {
            console.warn("No CV data available to populate.");
            // Optionally, clear existing file info
            const existingFileInfo = document.getElementById("existing-file-info");
            if (existingFileInfo) {
                existingFileInfo.textContent = "No CV file uploaded.";
            }
        }
    }
    /**
     * Joins array elements with spaces and inserts a newline after every `maxPerLine` items.
     * @param {Array} items - The array of strings to join.
     * @param {number} maxPerLine - The maximum number of items per line before inserting a newline.
     * @returns {string} - The formatted string with spaces and occasional newlines.
     */
    function joinWithSpacesAndNewlines(items, maxPerLine) {
        const lines = [];
        for (let i = 0; i < items.length; i += maxPerLine) {
            const chunk = items.slice(i, i + maxPerLine);
            lines.push(chunk.join(' '));
        }
        return lines.join('\n');
    }
    function populateFeedback() {

    }
    function populateInternships() {

    }
    function populateComplaints() {

            if (!ComplaintsContainer) {
                console.error('Complaints container element not found in the DOM.');
                return;
            }

            // Clear existing complaints
            ComplaintsContainer.innerHTML = '';

            if (Complaints && Complaints.length > 0) {
                Complaints.forEach(complaint => {
                    // Create a button element
                    const button = document.createElement('button');
                    button.classList.add('complaint-button'); // Add a class for styling

                    // Extract the first 3-4 words from the complaint text
                    const words = complaint.text.split(' ');
                    const buttonText = words.slice(0, 4).join(' ') + (words.length > 4 ? '...' : '');
                    button.textContent = buttonText;

                    // Optionally, add an event listener to show full complaint details
                    button.addEventListener('click', () => {
                        showComplaintDetails(complaint);
                    });

                    // Append the button to the complaints container
                    ComplaintsContainer.appendChild(button);
                });
            } else {
                // If no complaints, display a message
                const noComplaintsMsg = document.createElement('p');
                noComplaintsMsg.textContent = "No current complaints.";
                noComplaintsMsg.style.padding = "10px";
                noComplaintsMsg.style.color = "#555"; // Optional: Styling the message
                noComplaintsMsg.id = "no-complaints-msg"; // Assign an ID for future reference if needed

                ComplaintsContainer.appendChild(noComplaintsMsg);
            }
    }
    /**
     * Displays the full details of a complaint using SweetAlert2.
     * @param {Object} complaint - The complaint object containing details.
     */
    function showComplaintDetails(complaint) {
        // Determine the status based on boolean flags
        let status = 'Open';
        if (complaint.closedByStudent || complaint.closedByCompany) {
            status = 'Proposed to Close';
        }

        // Create the HTML content for the modal
        const modalContent = `
        <p><strong>Complaint ID:</strong> ${complaint.id}</p>
        <p><strong>Complaint Text:</strong> ${complaint.text}</p>
        <p><strong>Status:</strong> ${status}</p>
    `;

        // Display the modal using SweetAlert2
        Swal.fire({
            title: 'Complaint Details',
            html: modalContent,
            icon: 'info',
            confirmButtonText: 'OK',
            customClass: {
                popup: 'complaint-modal-popup' // Optional: For additional styling
            }
        });
    }

    function populateMessages() {

    }

    // Function to save CV
    function saveCv() {
        // Collect data from textareas
        const experiences = experiencesInput.value.trim();
        const programmingSkills = programmingSkillsInput.value.trim();
        const otherSkills = otherSkillsInput.value.trim();

        // Validate inputs
        if (experiences === "" || programmingSkills === "" || otherSkills === "") {
            showErrorAlert("Validation Error", "All CV fields must be filled out.");
            return;
        }

        // Validate file selection
        if (!selectedFile) {
            showErrorAlert("Validation Error", "Please select a CV file to upload.");
            return;
        }

        // Create FormData object
        const formData = new FormData();
        formData.append("experiences", experiences);
        formData.append("programmingSkills", programmingSkills);
        formData.append("otherSkills", otherSkills);
        formData.append("cv", selectedFile);

        // Determine which servlet to call
        let servletUrl = "UploadNewCv";
        if (hasCv && currentCvId) {
            // If modifying, include the CV ID
            formData.append("cvId", currentCvId);
            servletUrl = "ModifyCv";
        }

        // Show loading indicator
        Swal.fire({
            title: 'Saving CV...',
            allowOutsideClick: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });

        // Send POST request to the appropriate servlet
        fetch(servletUrl, {
            method: "POST",
            body: formData
        })
            .then(response => {
                if (response.ok) {
                    if (servletUrl === "ModifyCv") {
                        return response.json(); // ModifyCv returns JSON
                    } else {
                        return response.text(); // UploadNewCv returns text
                    }
                } else {
                    return response.text().then(text => { throw new Error(text) });
                }
            })
            .then(data => {
                // Hide loading indicator by closing the current SweetAlert
                Swal.close();

                if (servletUrl === "UploadNewCv") {
                    showSuccessAlert("Success", "Your CV has been created successfully.");
                    // After creating a new CV, update the flags
                    hasCv = true;
                    // Optionally, refresh the CV data from the server to get the new CV ID
                    // You can call this.open() here if necessary
                } else if (servletUrl === "ModifyCv") {
                    showSuccessAlert("Success", "Your CV has been modified successfully.");
                    // Optionally, you can process the returned JSON data if needed
                    // For example, update `cv` and `currentCvId` if they might have changed
                    // Assuming the server returns the modified CV
                    if (data.modifiedCv) {
                        cv = data.modifiedCv;
                        currentCvId = cv.id; // Update CV ID if necessary
                    }
                }

                // Update UI to read-only mode
                isEditing = false;
                cvActionBtn.textContent = hasCv ? 'Modify CV' : 'Create New CV';
                experiencesInput.readOnly = true;
                programmingSkillsInput.readOnly = true;
                otherSkillsInput.readOnly = true;

                // Disable file input and upload button
                cvFileInput.disabled = true;
                uploadCvBtn.disabled = true;
                selectedFileName.textContent = '';
                selectedFile = null;

                // Optionally, refresh the CV data from the server
                // Uncomment the line below if you want to refresh the data without reloading the page
                // this.open();
            })
            .catch(error => {
                // Hide loading indicator by closing the current SweetAlert
                Swal.close();
                showErrorAlert("Error", error.message);
            });
    }
    // Helper functions for alerts
    function showErrorAlert(title, message) {
        Swal.fire({
            title: title,
            text: message,
            icon: 'error',
            confirmButtonText: 'OK'
        });
    }

    function showSuccessAlert(title, message) {
        Swal.fire({
            title: title,
            text: message,
            icon: 'success',
            confirmButtonText: 'OK'
        });
    }
    // Function to process data from db
    function processUserData(jsonObject) {
        // **Assign Common Variables**
        internships = jsonObject.internships || [];
        Messages = jsonObject.unreadMessages || [];
        Complaints = jsonObject.unreadComplaints || [];
        var isStudent = false;
        let hasCv = false;

        // **Determine User Type and Assign Specific Variables**
        if (jsonObject.hasOwnProperty('student')) {
            user_class = jsonObject.student;
            cv = jsonObject.cv || null;
            isStudent = true;
            console.log('User is a Student:', user_class);
            console.log('CV:', cv);
            if (cv) {
                hasCv = true;
            }
        } else if (jsonObject.hasOwnProperty('company')) {
            user_class = jsonObject.company;
            Feedbacks = jsonObject.unreadFeedbacks || [];
            console.log('User is a Company:', user_class);
            console.log('Unread Feedbacks:', Feedbacks);
        } else {
            console.warn('Unknown user type. JSON object does not contain "student" or "company" keys.');
            return; // Exit the function or handle accordingly
        }

        // **Populate UI with User Information**
        populateUserInfo(user_class);
        if (isStudent) {
            populateCv();
        } else {
            populateFeedback();
            populateInternships();
        }
        populateComplaints();
        populateMessages();

        // **Update the CV Action Button Based on CV Existence**
        if (isStudent) {
            if (hasCv) {
                cvActionBtn.textContent = 'Modify CV';
            } else {
                cvActionBtn.textContent = 'Create New CV';
            }
        }
    }


    this.open = function () {
        homePageDiv.style.display = "";
        // Construct query string
        var url = "HomePageManager";

        makeCall("GET", url, null, function (x) {
            if (x.readyState === XMLHttpRequest.DONE) {
                var message = x.responseText;
                switch (x.status) {
                    case 200:
                        var jsonObject = JSON.parse(message);
                        processUserData(jsonObject);

                        break;

                    case 400: // bad request
                        pageOrchestrator.showPage("main page");
                        showErrorAlert(message);
                        break;

                    case 401: // unauthorized
                        pageOrchestrator.showPage("main page");
                        showErrorAlert(message);
                        break;

                    case 500: // server error
                        //pageOrchestrator.showPage("home page");
                        showErrorAlert(message);
                        console.log(message);
                        break;

                    default:
                        pageOrchestrator.showPage("main page");
                        break;
                }
            }
        });
    };

    // Methods called internally from the js class

    // Function to hide the page
    this.hide = function() {
        homePageDiv.style.display = 'none';
    };
}