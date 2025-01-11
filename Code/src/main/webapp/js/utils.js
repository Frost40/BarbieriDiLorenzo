/**
 * Makes an HTTP request using the XMLHttpRequest API.
 *
 * @param {string} method - The HTTP method to use (e.g., "GET", "POST", "PUT", "DELETE").
 * @param {string} url - The URL to send the request to.
 * @param {HTMLFormElement|null} formElement - An HTML form element to send as the request body, or null if no body is required.
 * @param {function} cback - A callback function to handle the response. Receives the XMLHttpRequest object as an argument.
 *
 * @description
 * This function creates a new XMLHttpRequest, sets up an event listener for state changes, and sends the request.
 * - If `formElement` is provided, its data is serialized into a FormData object and sent in the request body.
 * - If `formElement` is null, the request is sent without a body (useful for "GET" requests).
 */
function makeCall(method, url, formElement, cback) {
    let req = new XMLHttpRequest(); // visible by closure
    req.onreadystatechange = function () {
        cback(req);
    }; // closure
    req.open(method, url);
    if (formElement !== null) {
        var formData = new FormData(formElement)
        req.send(formData)
    } else {
        req.send();
    }
}

/**
* Displays an error alert using SweetAlert2.
*
* @param {string} message - The message to display in the alert.
*
* @description
* This function triggers an error alert with a red error icon,
* a title of "Error!", and the provided message.
* The user can dismiss the alert by clicking the "OK" button.
*/
function showErrorAlert(message) {
    Swal.fire({
        title: 'Error!',
        text: message,
        icon: 'error',
        confirmButtonText: 'OK'
    });
}

/**
 * Displays a success alert using SweetAlert2.
 *
 * @param {string} message - The message to display in the alert.
 *
 * @description
 * This function triggers a success alert with a green success icon,
 * a title of "Success!", and the provided message.
 * The user can dismiss the alert by clicking the "OK" button.
 */
function showSuccessAlert(message) {
    Swal.fire({
        title: 'Success!',
        text: message,
        icon: 'success',
        confirmButtonText: 'OK'
    });
}

/**
 * Displays an informational alert using SweetAlert2.
 *
 * @param {string} message - The message to display in the alert.
 *
 * @description
 * This function triggers an informational alert with a blue info icon,
 * a title of "Not authorized!", and the provided message.
 * The user can dismiss the alert by clicking the "OK" button.
 */
function showInfoAlert(message) {
    Swal.fire({
        title: 'Not authorized!',
        text: message,
        icon: 'info',
        confirmButtonText: 'OK'
    });
}