document.addEventListener('DOMContentLoaded', () => {
    var user = JSON.parse(sessionStorage.getItem('user'));

    if (!user) {
        window.location.href = "mainPage.html"; // reindirizza alla pagina di login se l'utente non è loggato
    } else {
        var pageOrchestrator = new PageOrchestrator(user);
        pageOrchestrator.showPage('home page'); // mostra la sezione homePage all'inizio
    }
});

//It manages the interaction between all the different page
function PageOrchestrator(user){
    // Initialize generic useful element
    let actualPage = null;

    // Initialize all the pages
    let homePage = new HomePage(user, this);
    let myInternshipsPage = new MyInternshipsPage(user, this);
    let searchPage = new SearchPage(user, this);
    let messagesPage = new MessagesPage(user, this);
    let notificationSection = new NotificationSection(user, this);

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
                myInternshipsPage.open();
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

            case 'main page':
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