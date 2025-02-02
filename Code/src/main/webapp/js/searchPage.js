function SearchPage(user, pageOrchestrator) {
    this.pageOrchestrator = pageOrchestrator;

    // Variable Initialization
    var homePageButton = document.getElementById('home-page-btn-search');
    var myInternshipsPageButton = document.getElementById('my-internships-btn-search');
    var logoutButton = document.getElementById('logout-btn-search');
    let searchPageDiv = document.getElementById('search-page-div');
    let internships = [];

    // Bottom navigation bar event listeners
    myInternshipsPageButton.addEventListener("click", () => {
        this.pageOrchestrator.showPage('my internships page', 'placeholder1', 'placeholder2', 'placeholder3');
    });
    homePageButton.addEventListener("click", () => {
        pageOrchestrator.showPage('home page', 'placeholder1', 'placeholder2', 'placeholder3');
    });
    logoutButton.addEventListener("click", () => {
        pageOrchestrator.showPage('main page', 'placeholder1', 'placeholder2', 'placeholder3');
    });

    // Function to sanitize HTML to prevent XSS
    function sanitizeHTML(str) {
        const temp = document.createElement('div');
        temp.textContent = str;
        return temp.innerHTML;
    }

    // Function to populate search results
    function populateSearchResults(internshipsArray) {
        const resultsContainer = document.getElementById('search-results');
        resultsContainer.innerHTML = ''; // Clear previous results

        internshipsArray.forEach(internship => {
            const entry = document.createElement('div');
            entry.className = 'internship-entry';

            entry.innerHTML = `
                <h3>${sanitizeHTML(internship.name)}</h3>
                <p>${sanitizeHTML(internship.description)}</p>
                <p class="rating">Rating: ${internship.rating.toFixed(1)} ⭐</p>
                <div class="action-buttons">
                    <button class="message-btn" data-id="${internship.id}">Message</button>
                    <button class="apply-btn" data-id="${internship.id}">Apply</button>
                </div>
            `;

            resultsContainer.appendChild(entry);
        });

        // Add event listeners for action buttons
        document.querySelectorAll('.message-btn').forEach(button => {
            button.addEventListener('click', function() {
                const internshipId = this.getAttribute('data-id');
                console.log(internshipId);
                openMessageModal(internshipId);
            });
        });

        document.querySelectorAll('.apply-btn').forEach(button => {
            button.addEventListener('click', function() {
                const internshipId = this.getAttribute('data-id');
                applyToInternship(internshipId);
            });
        });
    }

    // Function to fetch internships from the servlet
    function fetchInternships() {
        makeCall("GET", "SearchPageManager", null, function(response) {
            if (response.readyState === XMLHttpRequest.DONE) {
                if (response.status === 200) {
                    const data = JSON.parse(response.responseText);
                    internships = data.allInternships; // Update the internships array
                } else {
                    showErrorAlert("Error", response.responseText || "Failed to fetch internships.");
                }
            }
        });
    }
    // Search functionality
    document.getElementById('search-internships-btn').addEventListener('click', function() {
        const query = document.getElementById('search-input').value.trim().toLowerCase();
        if (query === "") {
            populateSearchResults(internships); // Show all if query is empty
        } else {
            const filtered = internships.filter(internship =>
                internship.name.toLowerCase().includes(query)
            );
            populateSearchResults(filtered);
        }
    });

    // Show All Internships
    document.getElementById('show-all-btn').addEventListener('click', function() {
        document.getElementById('search-input').value = '';
        populateSearchResults(internships);
    });
    // Functions to send message (modal page + send)
    function openMessageModal(internshipId) {
        Swal.fire({
            title: 'Send a Message',
            input: 'textarea',
            inputLabel: 'Your Message',
            inputPlaceholder: 'Type your message here...',
            inputAttributes: {
                'aria-label': 'Type your message here'
            },
            showCancelButton: true,
            confirmButtonText: 'Send',
            preConfirm: (message) => {
                if (!message || message.trim().length === 0) {
                    Swal.showValidationMessage('Message cannot be empty!');
                } else if (message.length > 255) {
                    Swal.showValidationMessage('Message cannot exceed 255 characters!');
                }
                return message;
            }
        }).then((result) => {
            if (result.isConfirmed) {
                const message = result.value.trim();
                console.log(internshipId);
                sendMessage(internshipId, message);
            }
        });
    }

    // Function to send the message to the server using a temporary form
    function sendMessage(internshipId, message) {
        // Create a temporary form
        const tempForm = document.createElement('form');
        tempForm.style.display = 'none';
        tempForm.method = 'POST';
        tempForm.action = 'SendMessage';

        // Create hidden input for message
        const messageInput = document.createElement('input');
        messageInput.type = 'hidden';
        messageInput.name = 'message';
        messageInput.value = message;
        tempForm.appendChild(messageInput);
        // Create hidden input for internshipId
        const internshipInput = document.createElement('input');
        internshipInput.type = 'hidden';
        internshipInput.name = 'receiverId';
        internshipInput.value = internshipId;
        tempForm.appendChild(internshipInput);
        console.log('Check'+ internshipInput.value);

        // Append the form to the body
        document.body.appendChild(tempForm);

        // Show a loading indicator
        Swal.fire({
            title: 'Sending Message...',
            allowOutsideClick: false,
            didOpen: () => Swal.showLoading(),
        });

        // Make the POST call using makeCall with the temporary form
        makeCall("POST", "SendMessage", tempForm, function(response) {
            if (response.readyState === XMLHttpRequest.DONE) {
                Swal.close(); // Close the loading indicator

                if (response.status === 200) {
                    showSuccessAlert("Message Sent!", "Your message has been successfully sent.");
                } else {
                   // showErrorAlert("Failed to Send", response.responseText || "An error occurred while sending your message.");
                }

                // Remove the temporary form
                document.body.removeChild(tempForm);
            }
        });
    }

    // Function to apply to an internship
    function applyToInternship(internshipId) {
        // Create a temporary form to send the internshipId
        const tempForm = document.createElement('form');
        tempForm.style.display = 'none';
        tempForm.method = 'POST';
        tempForm.action = 'ApplyToInternship';

        // Create a hidden input for internshipId
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'internshipId';
        input.value = internshipId;
        tempForm.appendChild(input);

        document.body.appendChild(tempForm);

        makeCall("POST", "ApplyToInternship", tempForm, function(response) {
            if (response.readyState === XMLHttpRequest.DONE) {
                if (response.status === 200) {
                    showSuccessAlert("Applied!", "You have successfully applied to this internship.");
                } else {
                    showErrorAlert("Application Failed", response.responseText || "Failed to apply to internship.");
                }
                document.body.removeChild(tempForm);
            }
        });
    }

    // Method to open the Search Page
    this.open = function () {
        searchPageDiv.style.display = "block";
        fetchInternships();
    }

    // Method to hide the Search Page
    this.hide = function() {
        searchPageDiv.style.display = "none";
    }
}
