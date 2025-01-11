function HomePage(user, pageOrchestrator) {
    this.pageOrchestrator = pageOrchestrator;

    // Getting references of the HTML elements

    // Variables of the page

    // Adding listeners

    this.open = function () {
        // Construct query string
        var url;

        makeCall("GET", url, null, function (x) {
            if (x.readyState === XMLHttpRequest.DONE) {
                var message = x.responseText;
                switch (x.status) {
                    case 200:
                        var jsonObject = JSON.parse(message);

                            // Code to execute in case of successful callBack

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
                        pageOrchestrator.showPage("login");
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