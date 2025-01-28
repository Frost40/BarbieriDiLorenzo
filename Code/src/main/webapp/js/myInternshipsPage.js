function MyInternshipsPage(user, pageOrchestrator) {
    this.pageOrchestrator = pageOrchestrator;

    // Getting references of the HTML elements
    let myInternshipPageDiv = document.getElementById('my-internship-page-div');
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

    // Adding listeners
    homePageButton.addEventListener('click', function() {
        pageOrchestrator.showPage("home page", "placeHolder", "placeHolder", "placeHolder");
    });
    logoutButton.addEventListener('click', function() {
        pageOrchestrator.showPage("login", "placeHolder", "placeHolder", "placeHolder");
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
                        this.internshipsData = jsonObject.data; // Save the data in the variable


                        console.log(this.internshipsData);
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

    // Function to hide the page
    this.hide = function() {
        myInternshipPageDiv.style.display = "none";
    };
}