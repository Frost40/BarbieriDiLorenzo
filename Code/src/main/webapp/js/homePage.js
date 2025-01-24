function HomePage(user, pageOrchestrator) {
    this.pageOrchestrator = pageOrchestrator;

    // Getting references of the HTML elements

    // Variables to store data
    var cv;
    var internships = []; // To store the internships received from the servlet
    var unreadMessages;
    var unreadComplaints;
    var unreadFeedbacks;

    var notificationsDropdownButton = document.querySelector('.sidenav .dropdown-btn:nth-of-type(1)'); // Second dropdown-btn
    var notificationsContainer = document.getElementById("dynamic-notifications");
    var cvActionBtn = document.getElementById("cv-action-btn");
    var  experiencesInput = document.getElementById("experiences-input");
    var  programmingSkillsInput = document.getElementById("programming-skills-input");
    var  otherSkillsInput = document.getElementById("other-skills-input");
    // Variable used in Cv editing/saving process
    let isEditing = false;

    notificationsDropdownButton.addEventListener("click", () => {
        // Toggle the dropdown visibility
        notificationsDropdownButton.classList.toggle("active");
        if (notificationsContainer.style.display === "block") {
            notificationsContainer.style.display = "none";
        } else {
            notificationsContainer.style.display = "block";
        }
    });

    cvActionBtn.addEventListener('click', function() {
        if (!isEditing) {
            // If not editing yet, switch to editing mode
            isEditing = true;

            // Change button label to 'Save CV'
            cvActionBtn.textContent = 'Save CV';

            // Make textareas editable
            experiencesInput.readOnly = false;
            programmingSkillsInput.readOnly = false;
            otherSkillsInput.readOnly = false;
        } else {
            // If we are currently editing, save and switch back to read-only
            isEditing = false;

            // Change button label back to 'Create New CV'
            cvActionBtn.textContent = 'Modify CV';

            // Make textareas read-only again
            experiencesInput.readOnly = true;
            programmingSkillsInput.readOnly = true;
            otherSkillsInput.readOnly = true;

            }
    })


    this.open = function () {
        // Construct query string
        var url = "HomePageManager";

        makeCall("GET", url, null, function (x) {
            if (x.readyState === XMLHttpRequest.DONE) {
                var message = x.responseText;
                switch (x.status) {
                    case 200:
                        var jsonObject = JSON.parse(message);

                        cv = jsonObject.cv || 1;
                        internships = jsonObject.internships || [];
                        unreadMessages = jsonObject.unreadMessages || [];
                        unreadComplaints = jsonObject.unreadComplaints || [];
                        unreadFeedbacks = jsonObject.unreadFeedbacks || [];

                        console.log(cv);
                        console.log(unreadFeedbacks);
                        console.log(unreadComplaints);
                        console.log(unreadMessages);


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
    }

    // Methods called internally from the js class

    // Function to hide the page
    this.hide = function() {
    };
}