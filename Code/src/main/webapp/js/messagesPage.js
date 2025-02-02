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

    // **Data Variables**
    let allChatsForStudent = [];
    let allChatsForCompany = {};
    let currentChatId = null; // Currently open chat ID
    var userClass;
    var selectedInternshipId;
    var unreadMessages;
    // **Event Listeners**

    // Close Modal
    closeBtn.addEventListener("click", () => {
        closeModal();
        window.location.reload();
    });

    // Close Modal when clicking outside the modal content
    window.addEventListener("click", (event) => {
        if (event.target === modal) {
            closeModal();
            window.location.reload();
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
    this.open = function (userType,messages) {
        modal.style.display = "block";
        userClass = userType;
        unreadMessages = messages;
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
        if(userClass === 'company'){
            document.getElementById("student-list-container").style.display = "none";
        }
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
                        showInfoAlert("Error", "No current chats.");
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
                    const unreadCount = unreadMessages.filter(msg => msg.senderId === chat.id).length;

                    chatEntry.innerHTML = `
                        <div>
                            <strong>${sanitizeHTML(chat.name)}</strong><br>
                            ${unreadCount > 0 ? `<span class="chat-badge">${unreadCount}</span>` : ""}
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
            // Handle Company: Display Each Internship Once with Associated Students
            const internshipMap = new Map(); // Map to store internships and their students

            allChatsForCompany.forEach(([internship, students]) => {
                if (!internshipMap.has(internship.id)) {
                    internshipMap.set(internship.id, { internship, students: [...students] });
                } else {
                    // Merge students for the same internship
                    internshipMap.get(internship.id).students.push(...students);
                }
            });

            // Now, create UI elements for unique internships
            internshipMap.forEach(({internship, students}) => {
                const internshipEntry = document.createElement("div");
                internshipEntry.classList.add("chat-entry");
                internshipEntry.setAttribute("data-internship-id", internship.id);
                const unreadCount = unreadMessages.filter(msg =>
                    students.some(student => msg.senderId === student.id && msg.receiverId === internship.id)
                ).length;
                internshipEntry.innerHTML = `
                <div>
                    <strong>${sanitizeHTML(internship.name)}</strong><br>
                     ${unreadCount > 0 ? `<span class="chat-badge">${unreadCount}</span>` : ""}
                </div>
            `;

                internshipEntry.addEventListener("click", () => {
                    showStudentList(internship.id, students);
                });

                chatList.appendChild(internshipEntry);
            });

            if (internshipMap.size === 0) {
                chatList.innerHTML = "<p>No chats available.</p>";
            }
        }
    }
    // **Open Selected Chat**
    function openChat(chatId, interlocutorName, internshipId = null) {
        currentChatId = chatId;  // The receiver (student for internship, internship for student)
        chatInterlocutorName.textContent = interlocutorName;

        // Set internship ID if the user is a company
        if (userClass === "company" && internshipId) {
            selectedInternshipId = internshipId; // Store the selected internship's ID
        }
        // Remove unread messages for this chat
        unreadMessages = unreadMessages.filter(msg => msg.senderId !== chatId);

        // Update UI badges
        updateChatBadges();
        showChatWindow();
        fetchMessages(chatId, internshipId);
    }
    // **Opens the student list for the internship case
    function showStudentList(internshipId, students) {
        // Get reference to chat list container & student list container
        const chatListContainer = document.getElementById("chat-list-container");
        const studentListContainer = document.getElementById("student-list-container");

        // Hide the internship list and show the student list
        chatListContainer.style.display = "none";
        studentListContainer.style.display = "block";

        // Clear existing student entries
        studentListContainer.innerHTML = "";

        if (students.length > 0) {
            students.forEach(student => {
                const studentEntry = document.createElement("div");
                studentEntry.classList.add("chat-entry");
                studentEntry.setAttribute("data-student-id", student.id); // Set student ID
                const unreadCount = unreadMessages.filter(msg =>
                    students.some(student => msg.senderId === student.id && msg.receiverId === internshipId)
                ).length;
                studentEntry.innerHTML = `
                <div>
                    <strong>${sanitizeHTML(student.name)}</strong><br>
                    ${unreadCount > 0 ? `<span class="chat-badge">${unreadCount}</span>` : ""}
                </div>
            `;

                // When clicking a student, open the chat with them
                studentEntry.addEventListener("click", () => {
                    openChat(student.id, student.name, internshipId);
                });

                studentListContainer.appendChild(studentEntry);
            });
        } else {
            studentListContainer.innerHTML = "<p>No students available.</p>";
        }

        // Add a back button to return to internships
        const backButton = document.createElement("button");
        backButton.classList.add("back-btn");
        backButton.textContent = "← Back to Internships";
        backButton.addEventListener("click", () => {
            studentListContainer.style.display = "none";
            chatListContainer.style.display = "block";
        });

        studentListContainer.appendChild(backButton);
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

                // Determine if the message is sent or received based on user type
                const isInternship = userClass === 'company'; // Assuming `userClass` is set globally

                if ((isInternship && msg.fromInternship) || (!isInternship && !msg.fromInternship)) {
                    // Sent message (sent by the current user)
                    messageDiv.classList.add("sent");
                } else {
                    // Received message (sent by the other person)
                    messageDiv.classList.add("received");
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
    function fetchMessages(chatId, internshipId = null) {
        // Construct the URL with required parameters
        let url = "OpenChatWindow?otherAccountId=" + chatId;
        if (internshipId) {
            url += `&internshipId=${internshipId}`;
        }
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
            if(userClass === 'student') {
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
            } else {
                if (!msg.readByInternship) {
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
                }
            }

        });
        // Remove unread messages for this chat after marking as read
        unreadMessages = unreadMessages.filter(msg => msg.senderId !== currentChatId);
        updateChatBadges();
    }
    function updateChatBadges() {
        document.querySelectorAll(".chat-entry").forEach(entry => {
            const chatId = entry.getAttribute("data-chat-id") || entry.getAttribute("data-student-id");
            if (!chatId) return;

            const unreadCount = unreadMessages.filter(msg => msg.senderId == chatId).length;
            let badge = entry.querySelector(".chat-badge");

            if (unreadCount > 0) {
                if (!badge) {
                    badge = document.createElement("span");
                    badge.classList.add("chat-badge");
                    entry.querySelector("div").appendChild(badge);
                }
                badge.textContent = unreadCount;
            } else if (badge) {
                badge.remove(); // Remove badge if no unread messages
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

        // **If Internship is Sending, Add Sender ID (Internship ID)**
        if (userClass === "company") {
            const senderInput = document.createElement('input');
            senderInput.type = 'hidden';
            senderInput.name = 'senderId';
            senderInput.value = selectedInternshipId; // Store selected internship ID when user picks an internship
            console.log('sender Id:', senderInput.value);
            form.appendChild(senderInput);
        }

        // Append form to the document body temporarily
        document.body.appendChild(form);

        // Callback function to handle the response
        function callback(req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                if (req.status === 200) {
                    // Instead of parsing response, fetch the latest messages from the DB
                    fetchMessages(currentChatId,selectedInternshipId);

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
