function SearchPage(user, pageOrchestrator) {
    this.pageOrchestrator = pageOrchestrator;

    // Getting references of the HTML elements
    let searchPageDiv = document.getElementById('search-page-div');
    let internships = []; // Initialize the internships array

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
                <p class="company-name">Company: ${sanitizeHTML(internship.companyName)}</p>
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
                // Implement message functionality
                alert(`Message Internship ID: ${internshipId}`);
            });
        });

        document.querySelectorAll('.apply-btn').forEach(button => {
            button.addEventListener('click', function() {
                const internshipId = this.getAttribute('data-id');
                // Implement apply functionality
                alert(`Apply to Internship ID: ${internshipId}`);
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

    // Method to open the Search Page
    this.open = function () {
        searchPageDiv.style.display = "block";
        // Hide other sections if necessary
        // Example:
        // document.getElementById('home-page-div').style.display = 'none';
        // document.getElementById('my-internship-page-div').style.display = 'none';

        // Fetch and display internships
        fetchInternships();
    }

    // Method to hide the Search Page
    this.hide = function() {
        searchPageDiv.style.display = "none";
    }
}
