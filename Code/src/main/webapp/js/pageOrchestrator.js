document.addEventListener('DOMContentLoaded', () => {
    var user = JSON.parse(sessionStorage.getItem('user'));

    if (!user) {
        window.location.href = "login.html"; // Redirect to login if no user is present
    } else {
        var pageOrchestrator = new PageOrchestrator(user);

        // Check if the user came from the login page
        if (sessionStorage.getItem('cameFromLogin') === 'true') {
            sessionStorage.removeItem('cameFromLogin'); // Clear the flag after use
            window.location.href = "homePage.html"; // Redirect to home page
        } else {
            // Use showPage for other pages
            pageOrchestrator.showPage('home');
        }
    }
});

//It manages the interaction between all the different page
function PageOrchestrator(user){
    // Initialize generic useful element
    let actualPage = null;

    // Initialize all the pages
    let homePage = new homePage(user, this);
    let myInternshipsPage = new myInternshipsPage(this);
    let searchPage = new searchPage(user, this);
    let messagesPage = new messagesPage(user, this);
    let notificationSection = new notificationSection(user, this);

    this.logout = function (){
        makeCall("POST", "Logout", null,
            function (x){
                if (x.readyState === XMLHttpRequest.DONE) {
                    let message = x.responseText;
                    switch (x.status) {
                        case 200:
                            sessionStorage.clear();
                            window.location.href = "mainPage.html";
                            break;

                        default:
                            showErrorAlert(message);
                            break;
                    }
                }
            }
        )
    }

    this.hideActualPage = function() {
        if (actualPage !== null) {
            actualPage.hide();
        }
    }

    this.showPage = function (pageName, info1, info2, info3) {
        switch(pageName) {
            case 'home page':
                this.hideActualPage();
                homePage.open();
                actualPage = homePage;
                break;

            case 'my internships page':
                this.hideActualPage();
                myInternshipsPage.open(info1);
                actualPage = myInternshipsPage;
                break;

            case 'messages page':
                messagesPage.open(info1, info2, info3);
                break;

            case 'search page':
                this.hideActualPage();
                searchPage.open(info1, info2, info3);
                actualPage = searchPage;
                break;

            case 'notification section':
                notificationSection.open(info1, info2, info3);
                break;

            case 'login':
                this.logout();
                break;

            default:
                console.log('Pagina non trovata');
                break;
        }
    }



    /*checks if the session is still active
    * in case if it isn't it shows the HomePage*/
    /*
    window.addEventListener("load", () => {
        if (user.id == null) {
            window.location.href = "login.html";
        } else {
            //pageOrchestrator.start(); // initialize the components
        } // display initial content
    }, false);
    */
}