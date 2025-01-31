function HomePage(user, pageOrchestrator) {
    this.pageOrchestrator = pageOrchestrator;

    // **Variables to store db data** //
    let cv;
    var internships;
    var Messages;
    var Complaints;
    var Feedbacks;
    var user_class;
    var userType;
    let homePageDiv = document.getElementById('home-page-div')
    // **Sidebar Variables** //
    var username = document.getElementById("username");
    var email = document.getElementById("email");
    var notificationsDropdownButton = document.getElementById("notifications-dropdown-btn");
    var notificationsContainer = document.getElementById("dynamic-notifications");
    var ComplaintsDropdownButton = document.getElementById("complaints-dropdown-btn");
    var ComplaintsContainer = document.getElementById("dynamic-complaints");
    var FeedbackDropdownButton = document.getElementById("feedback-dropdown-btn");
    var FeedbackContainer = document.getElementById("dynamic-feedback");
    var RecommendationDropdownButton = document.getElementById("recommendations-dropdown-btn");
    var RecommendationsContainer = document.getElementById("dynamic-recommendations");
    var messageButton = document.getElementById("messagesButton");
    // **My Cv Variables** //
    var saveCvForm = document.getElementById('saveCvForm');
    var cvActionBtn = document.getElementById("cv-action-btn");
    var  experiencesInput = document.getElementById("experiences-input");
    var  programmingSkillsInput = document.getElementById("programming-skills-input");
    var  otherSkillsInput = document.getElementById("other-skills-input");
    var cvFileInput = document.getElementById("cv-file-input");
    var uploadCvBtn = document.getElementById("upload-cv-btn");
    var selectedFileName = document.getElementById("selected-file-name");
    const readOnlyTextareas = [experiencesInput, programmingSkillsInput, otherSkillsInput];
    // **Internship Variables** //
    var saveInternshipBtn = document.getElementById('save-internship-btn');
    // **Badge Elements** //
    var notificationsBadge = document.getElementById("notifications-badge");
    var complaintsBadge = document.getElementById("complaints-badge");
    var feedbackBadge = document.getElementById("feedback-badge");
    var messagesBadge = document.getElementById("messages-badge");
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

    FeedbackDropdownButton.addEventListener("click", () => {

        FeedbackDropdownButton.classList.toggle("active");
        if (FeedbackContainer.style.display === "block") {

            FeedbackContainer.style.display = "none";
        } else {
            if (FeedbackContainer.children.length === 0) {

                const noFeedbackMsg = document.createElement('p');
                noFeedbackMsg.textContent = "No current feedbacks.";
                noFeedbackMsg.style.padding = "10px";
                noFeedbackMsg.style.color = "#555"; // Optional: Styling the message
                noFeedbackMsg.id = "no-feedbacks-msg"; // Assign an ID for future reference if needed

                FeedbackContainer.appendChild(noFeedbackMsg);
            }

            FeedbackContainer.style.display = "block";
        }
    });

    RecommendationDropdownButton.addEventListener("click", () => {
        showInfoAlert("Coming soon","This feature is not available yet.");
    });
    messageButton.addEventListener("click", () => {
        pageOrchestrator.showPage("messages page",userType);
    });

    // Internship Save Button Event Lister
    saveInternshipBtn.addEventListener('click',function(){
        saveInternship();
    });

    // CV Action Button Event Listener
    cvActionBtn.addEventListener('click', function() {
        if (!isEditing) {
            // Switch to editing mode
            isEditing = true;

            cvActionBtn.textContent = 'Save CV';

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
        if (!isEditing) {
            showInfoAlert("Edit CV First", "Please click on 'Create New CV' or 'Modify CV' before uploading a file.");
            return;
        }
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
        if(userType === 'student'){
            pageOrchestrator.showPage('search page', 'placeholder1', 'placeholder2', 'placeholder3');
        }
        else {
            showInfoAlert("Coming soon","This feature is not available yet.");
        }
    });
    logoutBtn.addEventListener("click", () => {
        pageOrchestrator.showPage('main page', 'placeholder1', 'placeholder2', 'placeholder3');
    });

    //** Series of functions utilized to populate the UI **//
    function updateBadgeCounts() {
        // Update Complaints Badge
        updateBadge(complaintsBadge, Complaints.length);

        // Update Feedback Badge (only for companies)
        if (userType === 'company') {
            updateBadge(feedbackBadge, Feedbacks.length);
        }

        // Update Messages Badge
        console.log(Messages);
        updateBadge(messagesBadge, Messages.length);

        // Update Notifications Badge (sum of Complaints and Feedbacks for companies, Complaints for students)
        if (userType === 'company') {
            const totalNotifications = Complaints.length + Feedbacks.length;
            updateBadge(notificationsBadge, totalNotifications);
        } else if (userType === 'student') {
            updateBadge(notificationsBadge, Complaints.length);
        }
    }
    /**
     * Updates the badge element with the given count.
     * @param {HTMLElement} badgeElement - The badge element to update.
     * @param {number} count - The number to display.
     */
    function updateBadge(badgeElement, count) {
        if (count > 0) {
            badgeElement.textContent = count;
            badgeElement.classList.add('show');
        } else {
            badgeElement.classList.remove('show');
            badgeElement.textContent = '0';
        }
    }

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
                    // Determine the MIME type (default to PDF)
                    let mimeType = 'application/pdf';
                    // Optionally, set MIME type based on some logic or additional data

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

                    // Set the inner HTML with a download link using a default name
                    existingFileInfo.innerHTML = `
                    <p>Current CV: 
                        <a href="${fileUrl}" download="Your_CV.pdf">Download CV</a>
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
        if (!FeedbackContainer) {
            console.error('Feedback container element not found in the DOM.');
            return;
        }

        // Clear existing feedbacks
        FeedbackContainer.innerHTML = '';

        if (Feedbacks && Feedbacks.length > 0) {
            Feedbacks.forEach(feedback => {
                // Create a button element
                const button = document.createElement('button');
                button.classList.add('feedback-button'); // Add a class for styling

                // Extract the first 3-4 words from the feedback text
                const words = feedback.text.split(' ');
                const buttonText = words.slice(0, 4).join(' ') + (words.length > 4 ? '...' : '');
                button.textContent = buttonText;

                button.addEventListener('click', () => {
                    showFeedbackDetails(feedback);
                });

                FeedbackContainer.appendChild(button);
            });
        } else {

            const noFeedbackMsg = document.createElement('p');
            noFeedbackMsg.textContent = "No current feedbacks.";
            noFeedbackMsg.style.padding = "10px";
            noFeedbackMsg.style.color = "#555"; // Optional: Styling the message
            noFeedbackMsg.id = "no-feedback-msg"; // Assign an ID for future reference if needed

            FeedbackContainer.appendChild(noFeedbackMsg);

        }
        // Update Complaints Badge
        updateBadge(feedbackBadge, Feedbacks.length);
        updateBadge(notificationsBadge, Complaints.length + (Feedbacks ? Feedbacks.length : 0));

    }
    // Function to populate the internships
    function populateInternships() {
        const internshipsList = document.getElementById('existing-internships-list');
        internshipsList.innerHTML = ''; // Clear previous entries

        internships.forEach(internship => {
            const entry = document.createElement('div');
            entry.className = 'internship-entry';
            entry.setAttribute('data-id', internship.id);

            // Internship details
            entry.innerHTML = `
            <p><strong>Name:</strong> ${sanitizeHTML(internship.name)}</p>
            <p><strong>Description:</strong> ${sanitizeHTML(internship.description)}</p>
            <p><strong>Rating:</strong> ${internship.rating.toFixed(1)} ⭐</p>
            <button type="button" class="modify-internship-btn" data-id="${internship.id}">Modify</button>
        `;

            internshipsList.appendChild(entry);
        });

        // Add event listeners for modify buttons
        document.querySelectorAll('.modify-internship-btn').forEach(button => {
            button.addEventListener('click', function () {
                const internshipId = this.getAttribute('data-id');
                const internship = internships.find(i => i.id == internshipId);
                populateModifyInternship(internship);
            });
        });
    }
    // Function to populate the internship form for modification
    function populateModifyInternship(internship) {
        // Populate form fields with internship data
        document.getElementById('internship-name-input').value = internship.name;
        document.getElementById('internship-description-input').value = internship.description;
        document.getElementById('internship-id-input').value = internship.id; // Hidden field

        // Optionally, change the Save button text to indicate modification
        const saveButton = document.getElementById('save-internship-btn');
        saveButton.textContent = "Modify Internship";

        // Scroll to the form for better UX
        document.getElementById('manageInternshipsForm').scrollIntoView({ behavior: 'smooth' });
    }

// Helper function to sanitize HTML
    function sanitizeHTML(str) {
        const temp = document.createElement('div');
        temp.textContent = str;
        return temp.innerHTML;
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
        // Update Complaints Badge
        updateBadge(complaintsBadge, Complaints.length);
        updateBadge(notificationsBadge, Complaints.length + (Feedbacks ? Feedbacks.length : 0));
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
            confirmButtonText: 'OK',
            customClass: {
                popup: 'complaint-modal-popup' // Optional: For additional styling
            }
        }).then((result) => {
            if (result.isConfirmed) {
                // User clicked OK, proceed to mark the complaint as read
                markComplaintAsRead(complaint.id);
            }
        });
    }
    function showFeedbackDetails(feedback) {

        // Create the HTML content for the modal
        const modalContent = `
        <p><strong>Feedback ID:</strong> ${feedback.id}</p>
        <p><strong>Feedback Text:</strong> ${feedback.text}</p>
        <p><strong>Rating:</strong> ${feedback.rating} ⭐</p>
         `;

        // Display the modal using SweetAlert2
        Swal.fire({
            title: 'Feedback Details',
            html: modalContent,
            confirmButtonText: 'OK',
            customClass: {
                popup: 'feedback-modal-popup' // Optional: For additional styling
            }
        }).then((result) => {
            if (result.isConfirmed) {
                // User clicked OK, proceed to mark the complaint as read
                markFeedbackAsRead(feedback.id);
            }
        });
    }
    /**
     * Marks a complaint as read by calling the ReadComplaint servlet.
     * @param {number} complaintId - The ID of the complaint to mark as read.
     */
    function markComplaintAsRead(complaintId) {
        // Create a hidden form element to send the complaintId
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = 'ReadComplaint'; // Ensure this path is correct

        // Create an input for complaintId
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'complaintId';
        input.value = complaintId;
        form.appendChild(input);

        // Optionally, append the form to the body if required by your server
        document.body.appendChild(form);

        // Callback function to handle the response
        function callback(req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                if (req.status === 200) {
                    console.log('Complaint marked as read:', req.responseText);
                    // Optionally, update the UI
                    window.location.reload();

                } else {
                    console.error('Failed to mark complaint as read:', req.responseText);
                    Swal.fire({
                        title: 'Error',
                        text: 'There was a problem marking the complaint as read.',
                        icon: 'error',
                        confirmButtonText: 'OK'
                    });
                }
            }
        }

        // Make the POST call using makeCall
        makeCall('POST', 'ReadComplaint', form, callback);

        // Clean up by removing the form from the DOM
        document.body.removeChild(form);
    }
    /**
     * Marks a complaint as read by calling the ReadComplaint servlet.
     * @param {number} complaintId - The ID of the complaint to mark as read.
     */
    function markFeedbackAsRead(feedbackId) {

        const form = document.createElement('form');
        form.method = 'POST';
        form.action = 'ReadFeedback';

        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'feedbackId';
        input.value = feedbackId;
        form.appendChild(input);

        document.body.appendChild(form);

        // Callback function to handle the response
        function callback(req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                if (req.status === 200) {
                    console.log('Feedback marked as read:', req.responseText);
                    // Optionally, update the UI
                    window.location.reload();

                } else {
                    console.error('Failed to mark feedback as read:', req.responseText);
                    Swal.fire({
                        title: 'Error',
                        text: 'There was a problem marking the feedback as read.',
                        icon: 'error',
                        confirmButtonText: 'OK'
                    });
                }
            }
        }

        // Make the POST call using makeCall
        makeCall('POST', 'ReadFeedback', form, callback);

        // Clean up by removing the form from the DOM
        document.body.removeChild(form);
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

        // Append CV ID if modifying an existing CV
        if (hasCv && currentCvId) {
            // Create a hidden input for cvId if it doesn't exist
            let cvIdInput = saveCvForm.querySelector('input[name="cvId"]');
            if (!cvIdInput) {
                cvIdInput = document.createElement('input');
                cvIdInput.type = 'hidden';
                cvIdInput.name = 'cvId';
                saveCvForm.appendChild(cvIdInput);
            }
            cvIdInput.value = currentCvId;
            cvActionBtn.innerText = 'Modify Cv';
            isEditing = false;
            window.location.reload();
        }

        // Show loading indicator
        Swal.fire({
            title: 'Saving CV...',
            allowOutsideClick: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });

        // Determine which servlet to call
        let servletUrl = "UploadNewCv";
        if (hasCv) {
            servletUrl = "ModifyCv";
        }

        // Make the AJAX call using makeCall
        makeCall("POST", servletUrl, saveCvForm, function (x) {
            if (x.readyState === XMLHttpRequest.DONE) {
                Swal.close(); // Close the loading indicator

                var message = x.responseText;
                switch (x.status) {
                    case 200:
                        if (servletUrl === "UploadNewCv") {
                            showSuccessAlert("Success", "Your CV has been created successfully.");
                            hasCv = true;
                            // Optionally, retrieve and set the new CV ID from the server response
                            // Example:
                            // const responseData = JSON.parse(x.responseText);
                            // currentCvId = responseData.cvId;
                        } else if (servletUrl === "ModifyCv") {
                            showSuccessAlert("Success", "Your CV has been modified successfully.");
                            // Optionally, process the returned data if necessary
                            // Example:
                            // let data = JSON.parse(x.responseText);
                            // if (data.modifiedCv) { ... }
                            try {
                                let data = JSON.parse(x.responseText);
                                if (data.modifiedCv) {
                                    // Update CV data and ID based on server response
                                    // Example:
                                     cv = data.modifiedCv;
                                     currentCvId = cv.id;
                                }
                            } catch (e) {
                                // Handle JSON parse error if response is not JSON
                                console.error("Failed to parse server response as JSON.", e);
                            }
                        }

                        // Update UI to read-only mode
                        experiencesInput.readOnly = true;
                        programmingSkillsInput.readOnly = true;
                        otherSkillsInput.readOnly = true;

                        // Disable file input and upload button
                        cvFileInput.disabled = true;
                        uploadCvBtn.disabled = true;
                        selectedFileName.textContent = '';
                        selectedFile = null;

                        break;

                    case 400: // Bad Request
                    case 401: // Unauthorized
                    case 500: // Server Error
                        showErrorAlert("Error", message);
                        break;

                    default:
                        showErrorAlert("Error", "An unexpected error occurred.");
                        break;
                }
            }
        });
    }

    function saveInternship() {
        // Get the form element
        const form = document.getElementById('manageInternshipsForm');

        // Collect and validate data
        const internshipName = document.getElementById('internship-name-input').value.trim();
        const internshipDescription = document.getElementById('internship-description-input').value.trim();
        const internshipId = document.getElementById('internship-id-input').value; // Check for modification

        if (!internshipName || !internshipDescription) {
            showErrorAlert("Validation Error", "All internship fields must be filled out.");
            return;
        }

        // Determine the servlet URL based on presence of internshipId
        const servletUrl = internshipId ? "ModifyInternship" : "UploadNewInternship";

        // Show a loading indicator
        Swal.fire({
            title: internshipId ? 'Modifying Internship...' : 'Saving Internship...',
            allowOutsideClick: false,
            didOpen: () => Swal.showLoading(),
        });

        // Send the form data using makeCall
        makeCall("POST", servletUrl, form, function (response) {
            if (response.readyState === XMLHttpRequest.DONE) {
                Swal.close(); // Close the loading indicator

                if (response.status === 200) {
                    showSuccessAlert("Success", internshipId ? "Internship modified successfully." : "Internship created successfully.");
                    populateInternships(); // Refresh the list of internships
                    form.reset(); // Clear form inputs
                    document.getElementById('internship-id-input').value = ''; // Clear the internship ID

                    // Reset the Save button text if it was modified
                    if (servletUrl === "ModifyInternship") {
                        document.getElementById('save-internship-btn').textContent = "Save Internship";
                    }
                    window.location.reload();
                } else {
                    showErrorAlert("Error", response.responseText || "An unexpected error occurred.");
                }
            }
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
    // Function to initialize the homepage based on user type
    function initializeHomePage() {
        if (userType === 'company') {
            // Show company-specific sidebar elements
            document.getElementById('feedback-dropdown-btn').style.display = 'block';
            document.getElementById('dynamic-feedback').style.display = 'block';

            // Hide student-specific sidebar elements
            document.getElementById('cv-action-btn').style.display = 'none';
            document.getElementById('save-internship-btn').style.display = 'block';

            // Show company main content and hide student content
            document.getElementById('company-content').style.display = 'block';
            document.getElementById('student-content').style.display = 'none';
            document.getElementById('search-btn').innerText = 'Analytics Page';
        } else if (userType === 'student') {
            // Hide company-specific sidebar elements
            document.getElementById('feedback-dropdown-btn').style.display = 'none';
            document.getElementById('dynamic-feedback').style.display = 'none';

            // Show student-specific sidebar elements
            document.getElementById('cv-action-btn').style.display = 'block';
            document.getElementById('save-internship-btn').style.display = 'none';

            // Show student main content and hide company content
            document.getElementById('student-content').style.display = 'block';
            document.getElementById('company-content').style.display = 'none';
        }
    }
    // Function to process data from db
    function processUserData(jsonObject) {
        // **Assign Common Variables**
        internships = jsonObject.internships || [];
        Messages = jsonObject.unreadMessages || [];
        Complaints = jsonObject.unreadComplaints || [];
        var isStudent = false;
        // **Determine User Type and Assign Specific Variables**
        if (jsonObject.hasOwnProperty('student')) {
            userType = 'student';
            user_class = jsonObject.student;
            cv = jsonObject.cv || null;
            isStudent = true;
            console.log('User is a Student:', user_class);
            console.log('CV:', cv);
            if (cv) {
                hasCv = true;
            }
        } else if (jsonObject.hasOwnProperty('company')) {
            userType = 'company';
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
            // **Update the CV Action Button Based on CV Existence**
            if (hasCv) {
                cvActionBtn.textContent = 'Modify CV';
                currentCvId = cv.id;
            } else {
                cvActionBtn.textContent = 'Create New CV';
            }
            populateCv();
        } else {

            populateFeedback();
            populateInternships();
        }

        populateComplaints();
        // **Initialize the Homepage Layout Based on User Type**
        initializeHomePage();
        // **Update Badge Counts**
        updateBadgeCounts();
    }


    this.open = function () {
        homePageDiv.style.display = "";
        // Construct query string
        var url = "HomePageManager";
        console.log(hasCv);

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