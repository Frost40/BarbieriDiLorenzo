function MessagesPage(user, pageOrchestrator) {
    this.pageOrchestrator = pageOrchestrator;

    // **HTML Element References**
    const modal = document.getElementById("message-page-modal");
    const closeBtn = modal.querySelector(".close-btn");
    const chatList = document.getElementById("chat-list");
    const chatWindowContainer = document.getElementById("chat-window-container");
    const chatInterlocutorName = document.getElementById("chat-interlocutor-name");
    const messagesContainer = document.getElementById("messages-container");
    const messageInput = document.getElementById("message-input");
    const sendMessageBtn = document.getElementById("send-message-btn");
    const backToChatsBtn = document.getElementById("back-to-chats-btn");
    var userClass;
    // **Data Variables**
    let allChatsForStudent = [];
    let allChatsForCompany = {};
    let currentChatId = null; // Currently open chat ID

    // **Event Listeners**

    // Close Modal
    closeBtn.addEventListener("click", () => {
        closeModal();
    });

    // Close Modal when clicking outside the modal content
    window.addEventListener("click", (event) => {
        if (event.target === modal) {
            closeModal();
        }
    });

    // Back to Chats Button
    backToChatsBtn.addEventListener("click", () => {
        showChatList();
    });

    // Send Message
    sendMessageBtn.addEventListener("click", () => {
        sendMessage();
    });

    // Also send message on Enter key
    messageInput.addEventListener("keypress", (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            sendMessage();
        }
    });

    // **Modal Control Functions**

    /**
     * Opens the Messages Modal.
     */
    this.open = function (userType) {
        modal.style.display = "block";
        userClass = userType;
        getChats();
    };

    /**
     * Closes the Messages Modal.
     */
    function closeModal() {
        modal.style.display = "none";
        hideChatWindow();
        messagesContainer.innerHTML = "";
        messageInput.value = "";
    }

    /**
     * Shows the Chat List and hides the Chat Window.
     */
    function showChatList() {
        document.getElementById("chat-list-container").style.display = "block";
        chatWindowContainer.style.display = "none";
    }

    /**
     * Shows the Chat Window and hides the Chat List.
     */
    function showChatWindow() {
        document.getElementById("chat-list-container").style.display = "none";
        chatWindowContainer.style.display = "block";
    }

    /**
     * Hides the Chat Window and shows the Chat List.
     */
    function hideChatWindow() {
        chatWindowContainer.style.display = "none";
        document.getElementById("chat-list-container").style.display = "block";
    }

    // **Fetch Chats from Server**
    function getChats() {
        const url = "MessagesManager";

        makeCall("GET", url, null, function (x) {
            if (x.readyState === XMLHttpRequest.DONE) {
                if (x.status === 200) {
                    try {
                        const jsonObject = JSON.parse(x.responseText);
                        if (userClass === 'student') {
                            allChatsForStudent = jsonObject.allChats;
                        } else {
                            allChatsForCompany = jsonObject.allChats;
                        }
                        populateChatList();
                        //updateBadgeCounts();
                    } catch (e) {
                        console.error("Failed to parse chats JSON:", e);
                        showErrorAlert("Error", "Failed to load chats.");
                    }
                } else {
                    console.error("Failed to fetch chats:", x.responseText);
                    showErrorAlert("Error", "Failed to load chats.");
                }
            }
        });
    }

    // **Populate Chat List**
    function populateChatList() {
        chatList.innerHTML = ""; // Clear existing chats

        if (userClass === 'student') {
            // Handle Student: allChatsForStudent is an array of Internships
            if (allChatsForStudent.length > 0) {
                allChatsForStudent.forEach(chat => {
                    const chatEntry = document.createElement("div");
                    chatEntry.classList.add("chat-entry");
                    chatEntry.setAttribute("data-chat-id", chat.id); // For identification

                    chatEntry.innerHTML = `
                        <div>
                            <strong>${sanitizeHTML(chat.name)}</strong><br>
                        </div>
                    `;
                    chatEntry.addEventListener("click", () => {
                        openChat(chat.id, chat.name);
                    });

                    chatList.appendChild(chatEntry);
                });
            } else {
                chatList.innerHTML = "<p>No chats available.</p>";
            }
        } else {
            // Handle Company: allChatsForCompany is an object mapping Internship IDs to Student arrays
            const internships = Object.keys(allChatsForCompany);
            if (internships.length > 0) {
                internships.forEach(internshipId => {
                    const students = allChatsForCompany[internshipId];
                    students.forEach(student => {
                        const chatEntry = document.createElement("div");
                        chatEntry.classList.add("chat-entry");
                        chatEntry.setAttribute("data-chat-id", student.id); // Assuming student.id is unique

                        chatEntry.innerHTML = `
                            <div>
                                <strong>${sanitizeHTML(student.name)}</strong><br>
                            </div>
                        `;

                        chatEntry.addEventListener("click", () => {
                            openChat(student.id, student.name);
                        });

                        chatList.appendChild(chatEntry);
                    });
                });
            } else {
                chatList.innerHTML = "<p>No chats available.</p>";
            }
        }
    }
    // **Open Selected Chat**
    function openChat(chatId, interlocutorName) {
        currentChatId = chatId;
        chatInterlocutorName.textContent = interlocutorName;
        showChatWindow();
        fetchMessages(chatId);
    }
    /**
     * Populates the chat window with messages.
     * Only handles the student case.
     * @param {Array} messages - Array of Message objects.
     */
    function populateMessages(messages) {
        messagesContainer.innerHTML = ""; // Clear existing messages

        if (messages.length > 0) {
            messages.forEach(msg => {
                // Create a message div
                const messageDiv = document.createElement("div");
                messageDiv.classList.add("message");

                // Determine if the message is sent or received
                if (msg.fromInternship) {
                    // Received message (from internship/company)
                    messageDiv.classList.add("received");
                } else {
                    // Sent message (by student)
                    messageDiv.classList.add("sent");
                }
                // Sanitize and set the message text
                messageDiv.textContent = sanitizeHTML(msg.message);
                // Append the message to the container
                messagesContainer.appendChild(messageDiv);
            });
            // Scroll to the bottom to show the latest messages
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        } else {
            messagesContainer.innerHTML = "<p>No messages yet.</p>";
        }
    }
    function fetchMessages(chatId) {
        // Construct the URL with required parameters
        const url = "OpenChatWindow?otherAccountId=" + chatId;
        // Make the GET request
        makeCall("GET", url, null, function (x) {
            if (x.readyState === XMLHttpRequest.DONE) {
                if (x.status === 200) {
                    try {
                        // Parse the JSON response
                        const jsonObject = JSON.parse(x.responseText);
                        const chat = jsonObject.chat; // Array of messages

                        // Populate the chat window with messages
                        populateMessages(chat);
                        markChatAsRead(chat);
                    } catch (e) {
                        console.error("Failed to parse messages JSON:", e);
                        showErrorAlert("Error", "Failed to load messages.");
                    }
                } else {
                    console.error("Failed to fetch messages:", x.responseText);
                    showErrorAlert("Error", "Failed to load messages.");
                }
            }
        });
    }
    function markChatAsRead(chat) {
        if (!chat || chat.length === 0) return; // No messages to process

        chat.forEach(msg => {
            // Check if the message is unread by the student
            if (!msg.readByStudent) {
                // Create a temporary form element
                const form = document.createElement('form');
                form.method = 'POST';
                form.action = "MarkMessageAsRead"; // Update this if the servlet URL differs

                // Create an input element for the message ID
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = 'messageId';
                input.value = msg.id;
                form.appendChild(input);

                // Append form to the document body temporarily
                document.body.appendChild(form);

                // Send the form using makeCall
                makeCall("POST", "ReadMessage", form, function (x) {
                    if (x.readyState === XMLHttpRequest.DONE) {
                        if (x.status === 200) {
                            console.log(`Message ${msg.id} marked as read.`);
                        } else {
                            console.error(`Failed to mark message ${msg.id} as read:`, x.responseText);
                        }
                    }
                });

                // Remove form after sending
                document.body.removeChild(form);
            }
        });
    }
    function sendMessage() {
        const text = messageInput.value.trim();
        if (text === "" || !currentChatId) return; // Prevent sending empty messages

        // Create a temporary form
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = "SendMessage"; // Ensure this matches the servlet URL

        // Hidden input for message content
        const messageInputField = document.createElement('input');
        messageInputField.type = 'hidden';
        messageInputField.name = 'message';
        messageInputField.value = text;
        form.appendChild(messageInputField);

        // Hidden input for receiver ID (which is stored as currentChatId)
        const receiverInput = document.createElement('input');
        receiverInput.type = 'hidden';
        receiverInput.name = 'receiverId';
        receiverInput.value = currentChatId;
        form.appendChild(receiverInput);

        // Append form to the document body temporarily
        document.body.appendChild(form);

        // Callback function to handle the response
        function callback(req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                if (req.status === 200) {
                    // Instead of parsing response, fetch the latest messages from the DB
                    fetchMessages(currentChatId);

                    // Clear the input field after sending the message
                    messageInput.value = "";
                } else {
                    console.error("Failed to send message:", req.responseText);
                    showErrorAlert("Error", "Failed to send message.");
                }
            }
        }

        // Make the POST call using makeCall
        makeCall('POST', "SendMessage", form, callback);

        // Clean up by removing the form from the DOM
        document.body.removeChild(form);
    }

    // **Utility Functions**

    /**
     * Sanitizes a string to prevent XSS attacks.
     * @param {string} str - The string to sanitize.
     * @returns {string} - The sanitized string.
     */
    function sanitizeHTML(str) {
        const temp = document.createElement('div');
        temp.textContent = str;
        return temp.innerHTML;
    }

    /**
     * Displays an error alert using SweetAlert2.
     * @param {string} title - The title of the alert.
     * @param {string} message - The message body of the alert.
     */
    function showErrorAlert(title, message) {
        Swal.fire({
            title: title,
            text: message,
            icon: 'error',
            confirmButtonText: 'OK'
        });
    }
}
