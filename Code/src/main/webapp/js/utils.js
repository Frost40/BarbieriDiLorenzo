/**
 * Makes an HTTP request using the XMLHttpRequest API.
 *
 * @param {string} method - The HTTP method to use (e.g., "GET", "POST", "PUT", "DELETE").
 * @param {string} url - The URL to send the request to.
 * @param {HTMLFormElement|null} formElement - An HTML form element to send as the request body, or null if no body is required.
 * @param {function} cback - A callback function to handle the response. Receives the XMLHttpRequest object as an argument.
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