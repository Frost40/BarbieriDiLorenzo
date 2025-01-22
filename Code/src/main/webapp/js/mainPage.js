/**
 * Login management
 */

(function() {
    var loginButton                   = document.getElementById("login_button");
    var loginIdInput                  = loginButton.closest("form").querySelector('input[name="id"]');
    var loginPasswordInput            = loginButton.closest("form").querySelector('input[name="password"]');
    var loginWarningDiv               = document.getElementById('login_warning_id');
    var openRegisterButton            = document.getElementById("open_register_button");
    var registerDiv                   = document.getElementById("register-div");
    var registerButton                = document.getElementById("register_button");
    var registerEmailInput            = registerButton.closest("form").querySelector('input[name="email"]');
    var registerUsernameInput         = registerButton.closest("form").querySelector('input[name="name"]');
    var registerPasswordInput 	      = registerButton.closest("form").querySelector('input[name="password"]');
    var registerRepeatPasswordInput   = registerButton.closest("form").querySelector('input[name="repeat_pwd"]');
    var registerWarningDiv            = document.getElementById('register_warning_id');

    const studentRadio = document.getElementById('student-radio');
    const companyRadio = document.getElementById('company-radio');
    const studentFields = document.getElementById('student-fields');
    const companyFields = document.getElementById('company-fields');
    const registerMainOfficeInput = document.getElementById('register-mainOffice');
    const registerOfficesInput = document.getElementById('register-offices');
    const registerSurnameInput = document.getElementById('register-surname');
    const registerNationalityInput = document.getElementById('register-nationality');
    const registerCityOfBirthInput = document.getElementById('register-cityOfBirth');


    studentRadio.addEventListener('change', () => {
        studentFields.style.display = 'block';
        companyFields.style.display = 'none';
        registerMainOfficeInput.required = false;
        registerOfficesInput.required = false;
        registerSurnameInput.required = true;
        registerNationalityInput.required = true;
        registerCityOfBirthInput.required = true;
    });
    companyRadio.addEventListener('change', () => {
        studentFields.style.display = 'none';
        companyFields.style.display = 'block';
        registerSurnameInput.required = false;
        registerNationalityInput.required = false;
        registerCityOfBirthInput.required = false;
        registerMainOfficeInput.required = true;
        registerOfficesInput.required = true;
    });
    // Attaches to login button
    loginButton.addEventListener("click", (e) => {

        var form = e.target.closest("form");
        loginWarningDiv.style.display = 'none';
        // Does a form check
        if (form.checkValidity()) {
            // Checks if the login input fields are null
            if (loginIdInput.value == "" || loginPasswordInput.value == "") {

                loginWarningDiv.textContent   = "One or more parameters are missing";
                loginWarningDiv.style.display = 'block';
                return;
            }

            sendToServer(form, loginWarningDiv, 'LoginManager', true);

        } else {
            //If not valid, notifies
            form.reportValidity();
        }
    });

    //Attaches to register button
    registerButton.addEventListener("click", (e) => {

        var form = e.target.closest("form");
        registerWarningDiv.style.display = 'none';
        // Checks if the inserted string (EMAIL) matches with an e-mail syntax (RCF5322 e-mail) by using a RegEx
        const emailRegEx = new RegExp("^(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])$");
        // Does a form check
        if (form.checkValidity()) {
            // Checks if the register input fields are null
            if (registerEmailInput.value           === "" || registerUsernameInput.value        === "" ||
                registerPasswordInput.value        === "" || registerRepeatPasswordInput.value  === "") {

                registerWarningDiv.textContent   = "One or more parameters are missing";
                registerWarningDiv.style.display = 'block';
                return;
            }
            // Checks if repeat_pwd and password field are not equal. If so sets a warning
            if (registerRepeatPasswordInput.value !== registerPasswordInput.value) {

                registerWarningDiv.textContent   = "Passwords do not match";
                registerWarningDiv.style.display = 'block';
                return;
            }
            // Checks if the email is not valid. If so sets a warning
            if (!emailRegEx.test(registerEmailInput.value || registerEmailInput.value <= 0 || registerEmailInput.value > 100)) {

                registerWarningDiv.textContent   = "The email is not valid";
                registerWarningDiv.style.display = 'block';
                return;
            }
            if (containsSpecialCharacters(registerUsernameInput.value)) {
                registerWarningDiv.textContent   = "The username can not contain special characters";
                registerWarningDiv.style.display = 'block';
                return;
            }
            // Get the selected user type
            const selectedUserType = document.querySelector('input[name="userType"]:checked').value;

            // Check for required fields based on user type
            if (selectedUserType === "student") {
                if (document.getElementById('register-surname').value === "" ||
                    document.getElementById('register-nationality').value === "" ||
                    document.getElementById('register-cityOfBirth').value === "") {

                    registerWarningDiv.textContent = "One or more parameters are missing";
                    registerWarningDiv.style.display = 'block';
                    return;
                }
            } else if (selectedUserType === "company") {
                if (document.getElementById('register-mainOffice').value === "" || document.getElementById('register-offices').value === "" ) {

                    registerWarningDiv.textContent = "One or more parameters are missing";
                    registerWarningDiv.style.display = 'block';
                    return;
                }
            }
            console.log("Form is valid. Sending to server.");
            sendToServer(form, registerWarningDiv, 'SignUpManager', false);

        } else {
            //If not valid, notifies
            form.reportValidity();
        }
    });


    //Attaches to register view button
    openRegisterButton.addEventListener("click", function(e) {

        if (e.target.textContent === "Register now!") {

            e.target.textContent       = "Hide register form";
            registerDiv.style.display = 'block';

        } else {

            e.target.textContent       = "Register now!";
            registerDiv.style.display = 'none';
        }
    });

    var self = this;

    function sendToServer(form, error_div, request_url, isLogin) {
        console.log("In send to server");
        makeCall("POST", request_url, form, function(req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                var message = req.responseText;

                switch(req.status) {

                    case 200: // ok

                        if (isLogin) {

                            sessionStorage.setItem('user', message);
                            window.location.href = "homePage.html";
                            console.log(sessionStorage.getItem('user'));

                        } else {

                            var click = new Event("click");
                            self.open_register_button.dispatchEvent(click);
                        }

                        break;

                    case 400: // bad request
                        error_div.textContent   = req.responseText;
                        error_div.style.display = 'block';
                        break;

                    case 401: // unauthorized
                        error_div.textContent   = req.responseText;
                        error_div.style.display = 'block';
                        break;

                    case 500: // server error
                        error_div.textContent   = req.responseText;
                        error_div.style.display = 'block';
                        break;

                    default: // error
                        error_div.textContent   = "Request reported status " + req.status;
                        error_div.style.display = 'block';
                }
            }
        });
    }

    function containsSpecialCharacters(input) {
        const specialCharactersPattern = /[^a-zA-Z0-9]/;

        return input !== null && specialCharactersPattern.test(input);
    }

})();