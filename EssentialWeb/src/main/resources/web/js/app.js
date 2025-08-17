// Global variables
let isLoggedIn = false;
let websocket = null;
let currentPage = 'home';

// DOM elements
const pages = {
    home: document.getElementById('home-page'),
    maps: document.getElementById('maps-page'),
    server: document.getElementById('server-page'),
    login: document.getElementById('login-page')
};

// Initialize the application when the DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    // Initialize language based on browser settings
    window.i18n.init();

    // Translate the UI
    window.i18n.translateUI();

    // Check if user is already logged in (has a valid session)
    checkLoginStatus();

    // Set up navigation
    setupNavigation();

    // Set up login form
    setupLoginForm();

    // Set up map upload
    setupMapUpload();

    // Set up chat form
    setupChatForm();

    // Load initial data
    loadFeaturedMaps();

    // Log the current language
    console.log('Current language: ' + window.i18n.getCurrentLanguage());
});

// Check if the user is already logged in
function checkLoginStatus() {
    fetch('/api/auth/status')
        .then(response => {
            if (response.ok) {
                return response.json();
            } else {
                throw new Error(window.i18n.translate('error.not.logged.in'));
            }
        })
        .then(data => {
            isLoggedIn = true;
            updateLoginState(data.username);
        })
        .catch(() => {
            isLoggedIn = false;
            updateLoginState();
        });
}

// Update UI based on login state
function updateLoginState(username) {
    const loginLinks = document.querySelectorAll('#nav-login, #drawer-login');
    const logoutLinks = document.querySelectorAll('#nav-logout, #drawer-logout');

    if (isLoggedIn) {
        loginLinks.forEach(link => link.style.display = 'none');
        logoutLinks.forEach(link => {
            link.style.display = 'block';
            link.textContent = `${window.i18n.translate('nav.logout')} (${username})`;
        });

        // Load protected data
        loadMaps();
        loadServerStatus();
        setupWebSocket();
    } else {
        loginLinks.forEach(link => link.style.display = 'block');
        logoutLinks.forEach(link => link.style.display = 'none');

        // Show login page if trying to access protected pages
        if (currentPage === 'maps' || currentPage === 'server') {
            showPage('login');
        }
    }
}

// Set up navigation
function setupNavigation() {
    // Main navigation links
    document.getElementById('nav-home').addEventListener('click', () => showPage('home'));
    document.getElementById('nav-maps').addEventListener('click', () => showPage('maps'));
    document.getElementById('nav-server').addEventListener('click', () => showPage('server'));
    document.getElementById('nav-login').addEventListener('click', () => showPage('login'));
    document.getElementById('nav-logout').addEventListener('click', logout);

    // Drawer navigation links
    document.getElementById('drawer-home').addEventListener('click', () => showPage('home'));
    document.getElementById('drawer-maps').addEventListener('click', () => showPage('maps'));
    document.getElementById('drawer-server').addEventListener('click', () => showPage('server'));
    document.getElementById('drawer-login').addEventListener('click', () => showPage('login'));
    document.getElementById('drawer-logout').addEventListener('click', logout);
}

// Show the specified page
function showPage(pageName) {
    // Check if user is logged in for protected pages
    if ((pageName === 'maps' || pageName === 'server') && !isLoggedIn) {
        pageName = 'login';
    }

    // Hide all pages
    Object.values(pages).forEach(page => {
        if (page) page.style.display = 'none';
    });

    // Show the selected page
    if (pages[pageName]) {
        pages[pageName].style.display = 'block';
        currentPage = pageName;

        // Load data for the page if needed
        if (pageName === 'maps' && isLoggedIn) {
            loadMaps();
        } else if (pageName === 'server' && isLoggedIn) {
            loadServerStatus();
            loadChatHistory();
        } else if (pageName === 'home') {
            loadFeaturedMaps();
        }
    }
}

// Set up login form
function setupLoginForm() {
    const loginForm = document.getElementById('login-form');
    const loginBtn = document.getElementById('login-btn');
    const loginError = document.getElementById('login-error');

    loginBtn.addEventListener('click', () => {
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;

        if (!username || !password) {
            loginError.textContent = window.i18n.translate('login.error.fields');
            loginError.style.display = 'block';
            return;
        }

        loginError.style.display = 'none';

        fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        })
        .then(response => {
            if (response.ok) {
                return response.json();
            } else if (response.status === 403) {
                return response.json().then(data => {
                    throw new Error(data.message || window.i18n.translate('login.error.forbidden'));
                });
            } else {
                throw new Error(window.i18n.translate('login.error.credentials'));
            }
        })
        .then(data => {
            isLoggedIn = true;
            updateLoginState(data.username);
            showPage('home');
            loginForm.reset();
        })
        .catch(error => {
            loginError.textContent = error.message;
            loginError.style.display = 'block';
        });
    });
}

// Logout function
function logout() {
    fetch('/api/auth/logout')
        .then(() => {
            isLoggedIn = false;
            updateLoginState();
            showPage('home');

            // Close WebSocket if open
            if (websocket) {
                websocket.close();
                websocket = null;
            }
        });
}

// Load featured maps for the home page
function loadFeaturedMaps() {
    const featuredMapsContainer = document.getElementById('featured-maps');

    fetch('/api/maps')
        .then(response => {
            if (response.ok) {
                return response.json();
            } else if (response.status === 401) {
                // Not logged in, show placeholder
                throw new Error(window.i18n.translate('error.auth.required'));
            } else {
                throw new Error(window.i18n.translate('error.load.maps'));
            }
        })
        .then(maps => {
            featuredMapsContainer.innerHTML = '';

            // Display up to 4 featured maps
            const featuredMaps = maps.slice(0, 4);

            if (featuredMaps.length === 0) {
                featuredMapsContainer.innerHTML = `<p>${window.i18n.translate('maps.none')}</p>`;
                return;
            }

            featuredMaps.forEach(map => {
                const mapCard = createMapCard(map, false);
                featuredMapsContainer.appendChild(mapCard);
            });
        })
        .catch(error => {
            if (error.message === window.i18n.translate('error.auth.required')) {
                featuredMapsContainer.innerHTML = `
                    <div class="mdl-card mdl-shadow--2dp">
                        <div class="mdl-card__title">
                            <h2 class="mdl-card__title-text">${window.i18n.translate('login.required')}</h2>
                        </div>
                        <div class="mdl-card__supporting-text">
                            ${window.i18n.translate('login.to.view')}
                        </div>
                        <div class="mdl-card__actions mdl-card--border">
                            <a class="mdl-button mdl-button--colored mdl-js-button mdl-js-ripple-effect" href="#" onclick="showPage('login')">
                                ${window.i18n.translate('login.button')}
                            </a>
                        </div>
                    </div>
                `;
            } else {
                featuredMapsContainer.innerHTML = `<p>${window.i18n.translate('maps.error', error.message)}</p>`;
            }
        });
}

// Load all maps for the maps page
function loadMaps() {
    const mapListContainer = document.getElementById('map-list');

    fetch('/api/maps')
        .then(response => {
            if (response.ok) {
                return response.json();
            } else {
                throw new Error(window.i18n.translate('error.load.maps'));
            }
        })
        .then(maps => {
            mapListContainer.innerHTML = '';

            if (maps.length === 0) {
                mapListContainer.innerHTML = `<p>${window.i18n.translate('maps.none')}</p>`;
                return;
            }

            maps.forEach(map => {
                const mapCard = createMapCard(map, true);
                mapListContainer.appendChild(mapCard);
            });
        })
        .catch(error => {
            mapListContainer.innerHTML = `<p>${window.i18n.translate('maps.error', error.message)}</p>`;
        });
}

// Create a map card element
function createMapCard(map, showDownload) {
    const card = document.createElement('div');
    card.className = 'mdl-card mdl-shadow--2dp map-card';

    const imageUrl = map.preview || '';
    const authorText = window.i18n.translate('maps.author');
    const planetText = window.i18n.translate('maps.planet');
    const unknownText = window.i18n.translate('maps.unknown');
    const noDescriptionText = window.i18n.translate('maps.no.description');
    const downloadText = window.i18n.translate('maps.download');

    card.innerHTML = `
        <div class="map-card__image" ${imageUrl ? `style="background-image: url(${imageUrl})"` : ''}>
            ${!imageUrl ? '<i class="material-icons">map</i>' : ''}
        </div>
        <div class="mdl-card__title">
            <h2 class="mdl-card__title-text">${map.name}</h2>
        </div>
        <div class="mdl-card__supporting-text">
            <p><strong>${authorText}:</strong> ${map.author || unknownText}</p>
            <p><strong>${planetText}:</strong> ${map.planet || unknownText}</p>
            <p>${map.description || noDescriptionText}</p>
        </div>
        ${showDownload ? `
        <div class="mdl-card__actions mdl-card--border">
            <a href="/api/maps/download/${map.name}" class="mdl-button mdl-button--colored mdl-js-button mdl-js-ripple-effect">
                <i class="material-icons">file_download</i> ${downloadText}
            </a>
        </div>
        ` : ''}
    `;

    return card;
}

// Set up map upload
function setupMapUpload() {
    const uploadBtn = document.getElementById('upload-map-btn');
    const uploadDialog = document.getElementById('upload-dialog');
    const uploadForm = document.getElementById('upload-form');
    const uploadError = document.getElementById('upload-error');
    const fileInput = document.getElementById('map-file');
    const fileName = document.getElementById('file-name');

    // Show dialog when upload button is clicked
    uploadBtn.addEventListener('click', () => {
        uploadDialog.showModal();
        uploadError.style.display = 'none';
        uploadForm.reset();
    });

    // Update file name when file is selected
    fileInput.addEventListener('change', () => {
        fileName.value = fileInput.files[0] ? fileInput.files[0].name : '';
    });

    // Cancel button closes the dialog
    document.getElementById('upload-cancel').addEventListener('click', () => {
        uploadDialog.close();
    });

    // Confirm button uploads the file
    document.getElementById('upload-confirm').addEventListener('click', () => {
        if (!fileInput.files[0]) {
            uploadError.textContent = window.i18n.translate('upload.select');
            uploadError.style.display = 'block';
            return;
        }

        const file = fileInput.files[0];

        // Check file extension
        if (!file.name.endsWith('.msav')) {
            uploadError.textContent = window.i18n.translate('upload.only.msav');
            uploadError.style.display = 'block';
            return;
        }

        // Check file size (10MB max)
        if (file.size > 10 * 1024 * 1024) {
            uploadError.textContent = window.i18n.translate('upload.size.limit');
            uploadError.style.display = 'block';
            return;
        }

        const formData = new FormData();
        formData.append('file', file);

        fetch('/api/maps/upload', {
            method: 'POST',
            body: formData
        })
        .then(response => {
            if (response.ok) {
                uploadDialog.close();
                loadMaps();
                showSnackbar(window.i18n.translate('upload.success'));
            } else {
                return response.text().then(text => {
                    throw new Error(text || window.i18n.translate('upload.error'));
                });
            }
        })
        .catch(error => {
            uploadError.textContent = error.message;
            uploadError.style.display = 'block';
        });
    });
}

// Load server status
function loadServerStatus() {
    fetch('/api/server/status')
        .then(response => {
            if (response.ok) {
                return response.json();
            } else {
                throw new Error(window.i18n.translate('error.load.status'));
            }
        })
        .then(status => {
            document.getElementById('current-map').textContent = status.map;
            document.getElementById('server-tps').textContent = status.tps.toFixed(1);
            document.getElementById('server-wave').textContent = status.wave;
            document.getElementById('server-time').textContent = status.gameTime;

            // Update player list
            const playerList = document.getElementById('player-list');
            playerList.innerHTML = '';

            if (status.players.length === 0) {
                playerList.innerHTML = `<li class="mdl-list__item"><span class="mdl-list__item-primary-content">${window.i18n.translate('server.no.players')}</span></li>`;
                return;
            }

            status.players.forEach(player => {
                const playerItem = document.createElement('li');
                playerItem.className = 'mdl-list__item';
                playerItem.innerHTML = `<span class="mdl-list__item-primary-content">${player}</span>`;
                playerList.appendChild(playerItem);
            });
        })
        .catch(error => {
            console.error('Error loading server status:', error);
        });
}

// Load chat history
function loadChatHistory() {
    fetch('/api/server/chat')
        .then(response => {
            if (response.ok) {
                return response.json();
            } else {
                throw new Error(window.i18n.translate('error.load.chat'));
            }
        })
        .then(messages => {
            const chatMessages = document.getElementById('chat-messages');
            chatMessages.innerHTML = '';

            if (messages.length === 0) {
                chatMessages.innerHTML = `<p class="chat-message">${window.i18n.translate('server.chat.none')}</p>`;
                return;
            }

            messages.forEach(message => {
                const messageElement = document.createElement('p');
                messageElement.className = 'chat-message';
                messageElement.textContent = `${message.player}: ${message.message}`;
                chatMessages.appendChild(messageElement);
            });

            // Scroll to bottom
            chatMessages.scrollTop = chatMessages.scrollHeight;
        })
        .catch(error => {
            console.error('Error loading chat history:', error);
        });
}

// Set up chat form
function setupChatForm() {
    const chatForm = document.getElementById('chat-form');
    const chatInput = document.getElementById('chat-input');

    chatForm.addEventListener('submit', event => {
        event.preventDefault();

        const message = chatInput.value.trim();
        if (!message) return;

        fetch('/api/server/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'text/plain'
            },
            body: message
        })
        .then(response => {
            if (response.ok) {
                chatInput.value = '';

                // Add message to chat (optimistic update)
                const chatMessages = document.getElementById('chat-messages');
                const messageElement = document.createElement('p');
                messageElement.className = 'chat-message own';
                messageElement.textContent = `${window.i18n.translate('server.chat.you')}: ${message}`;
                chatMessages.appendChild(messageElement);

                // Scroll to bottom
                chatMessages.scrollTop = chatMessages.scrollHeight;
            } else {
                throw new Error(window.i18n.translate('error.send.message'));
            }
        })
        .catch(error => {
            console.error('Chat message send error:', error);
            showSnackbar(window.i18n.translate('error.send.message'));
        });
    });
}

// Set up WebSocket for live updates
function setupWebSocket() {
    if (websocket) {
        websocket.close();
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/api/server/live`;

    websocket = new WebSocket(wsUrl);

    websocket.onopen = () => {
        console.log(window.i18n.translate('ws.connected'));
    };

    websocket.onmessage = event => {
        const data = JSON.parse(event.data);

        // Update server status
        document.getElementById('current-map').textContent = data.map;
        document.getElementById('server-tps').textContent = data.tps.toFixed(1);
        document.getElementById('server-wave').textContent = data.wave;
        document.getElementById('server-time').textContent = data.gameTime;

        // Update player list
        const playerList = document.getElementById('player-list');
        playerList.innerHTML = '';

        if (data.players.length === 0) {
            playerList.innerHTML = `<li class="mdl-list__item"><span class="mdl-list__item-primary-content">${window.i18n.translate('server.no.players')}</span></li>`;
            return;
        }

        data.players.forEach(player => {
            const playerItem = document.createElement('li');
            playerItem.className = 'mdl-list__item';
            playerItem.innerHTML = `<span class="mdl-list__item-primary-content">${player}</span>`;
            playerList.appendChild(playerItem);
        });
    };

    websocket.onerror = error => {
        console.error(window.i18n.translate('ws.error'), error);
    };

    websocket.onclose = () => {
        console.log(window.i18n.translate('ws.closed'));

        // Try to reconnect after 5 seconds
        setTimeout(() => {
            if (isLoggedIn && currentPage === 'server') {
                setupWebSocket();
            }
        }, 5000);
    };
}

// Helper function to format time in minutes to MM:SS
function formatTime(minutes) {
    const mins = Math.floor(minutes);
    const secs = Math.floor((minutes - mins) * 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
}

// Show a snackbar message
function showSnackbar(message) {
    // Check if MDL snackbar container exists
    let snackbarContainer = document.querySelector('.mdl-snackbar');

    // Create it if it doesn't exist
    if (!snackbarContainer) {
        snackbarContainer = document.createElement('div');
        snackbarContainer.className = 'mdl-snackbar';
        snackbarContainer.setAttribute('aria-live', 'assertive');
        snackbarContainer.setAttribute('aria-atomic', 'true');
        snackbarContainer.setAttribute('aria-relevant', 'text');

        const snackbarText = document.createElement('div');
        snackbarText.className = 'mdl-snackbar__text';

        const snackbarAction = document.createElement('button');
        snackbarAction.className = 'mdl-snackbar__action';
        snackbarAction.type = 'button';

        snackbarContainer.appendChild(snackbarText);
        snackbarContainer.appendChild(snackbarAction);

        document.body.appendChild(snackbarContainer);
    }

    // Show the message
    const data = {
        message: message,
        timeout: 2000
    };

    snackbarContainer.MaterialSnackbar.showSnackbar(data);
}
