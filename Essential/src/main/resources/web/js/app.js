// Global variables
let isLoggedIn = false;
let currentUsername = '';
let currentPage = 'home';
let allMaps = [];
let chatPollInterval = null;
let statusPollInterval = null;
let historyPollInterval = null;
let contributionPollInterval = null;
let isFirstChatLoad = true;
let lastChatHistoryJson = '';
let currentServerMode = 'none';

// DOM elements
const pages = {
    home: document.getElementById('home-page'),
    maps: document.getElementById('maps-page'),
    server: document.getElementById('server-page'),
    profile: document.getElementById('profile-page'),
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

    // Set up Hamburger menu
    setupHamburgerMenu();

    // Set up login form
    setupLoginForm();

    // Set up map upload
    setupMapUpload();

    // Set up map filter bar
    setupMapFilters();

    // Set up chat form
    setupChatForm();

    // Load initial data
    loadFeaturedMaps();

    // Log the current language
    console.log('Current language: ' + window.i18n.getCurrentLanguage());
});

// Check if the user is already logged in
function checkLoginStatus() {
    fetch('api/auth/status')
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
    currentUsername = username || '';
    const loginLinks = document.querySelectorAll('#nav-login, #drawer-login');
    const logoutLinks = document.querySelectorAll('#nav-logout, #drawer-logout');
    const profileContainer = document.getElementById('user-profile-container');

    if (isLoggedIn) {
        loginLinks.forEach(link => link.style.display = 'none');
        logoutLinks.forEach(link => {
            link.style.display = 'block';
        });

        if (profileContainer) {
            profileContainer.style.display = 'flex';
            const nameEl = document.getElementById('user-name-display');
            if (nameEl && username) nameEl.textContent = `${username} (${username})`;
        }

        // Load protected data
        loadMaps();
        loadServerStatus();
        
        // Start polling if on Home or Server page
        if (currentPage === 'home') {
            startChatPolling();
            startStatusPolling();
            stopHistoryPolling();
        } else if (currentPage === 'server') {
            stopChatPolling();
            stopStatusPolling();
            startHistoryPolling();
        }
    } else {
        loginLinks.forEach(link => link.style.display = 'block');
        logoutLinks.forEach(link => link.style.display = 'none');
        
        if (profileContainer) {
            profileContainer.style.display = 'none';
        }

        stopChatPolling();
        stopStatusPolling();
        stopHistoryPolling();

        // Show login page if trying to access protected pages
        if (currentPage === 'maps' || currentPage === 'server' || currentPage === 'profile') {
            showPage('login');
        }
    }
}

// Set up Hamburger sliding drawer menu
function setupHamburgerMenu() {
    const hamburgerBtn = document.getElementById('hamburger-btn');
    const drawerCloseBtn = document.getElementById('drawer-close');
    const drawerOverlay = document.getElementById('drawer-overlay');
    const appDrawer = document.getElementById('app-drawer');

    if (hamburgerBtn && appDrawer && drawerOverlay) {
        hamburgerBtn.addEventListener('click', () => {
            appDrawer.classList.add('open');
            drawerOverlay.classList.add('show');
        });
    }

    const closeMobileDrawer = () => {
        if (appDrawer && drawerOverlay) {
            appDrawer.classList.remove('open');
            drawerOverlay.classList.remove('show');
        }
    };

    if (drawerCloseBtn) drawerCloseBtn.addEventListener('click', closeMobileDrawer);
    if (drawerOverlay) drawerOverlay.addEventListener('click', closeMobileDrawer);
}

function closeMobileDrawer() {
    const appDrawer = document.getElementById('app-drawer');
    const drawerOverlay = document.getElementById('drawer-overlay');
    if (appDrawer && drawerOverlay) {
        appDrawer.classList.remove('open');
        drawerOverlay.classList.remove('show');
    }
}

// Set up navigation
function setupNavigation() {
    // Main navigation links
    document.getElementById('nav-home').addEventListener('click', () => showPage('home'));
    document.getElementById('nav-maps').addEventListener('click', () => showPage('maps'));
    document.getElementById('nav-server').addEventListener('click', () => showPage('server'));
    document.getElementById('nav-profile').addEventListener('click', () => showPage('profile'));
    document.getElementById('nav-login').addEventListener('click', () => showPage('login'));
    document.getElementById('nav-logout').addEventListener('click', logout);

    // Drawer navigation links
    document.getElementById('drawer-home').addEventListener('click', () => showPage('home'));
    document.getElementById('drawer-maps').addEventListener('click', () => showPage('maps'));
    document.getElementById('drawer-server').addEventListener('click', () => showPage('server'));
    document.getElementById('drawer-profile').addEventListener('click', () => showPage('profile'));
    document.getElementById('drawer-login').addEventListener('click', () => showPage('login'));
    document.getElementById('drawer-logout').addEventListener('click', logout);
}

// Show the specified page
function showPage(pageName) {
    // Check if user is logged in for protected pages
    if ((pageName === 'maps' || pageName === 'server' || pageName === 'profile') && !isLoggedIn) {
        pageName = 'login';
    }

    // Hide all pages
    Object.values(pages).forEach(page => {
        if (page) page.style.display = 'none';
    });

    // Handle navigation tab active class styling
    document.querySelectorAll('.nav-link, .drawer-link').forEach(link => {
        link.classList.remove('active');
    });

    // Set active link in header & drawer
    const activeNavId = `nav-${pageName}`;
    const activeDrawerId = `drawer-${pageName}`;
    const activeNavLink = document.getElementById(activeNavId);
    const activeDrawerLink = document.getElementById(activeDrawerId);
    if (activeNavLink) activeNavLink.classList.add('active');
    if (activeDrawerLink) activeDrawerLink.classList.add('active');

    // Close mobile drawer when clicking navigations
    closeMobileDrawer();

    // Show the selected page
    if (pages[pageName]) {
        pages[pageName].style.display = 'block';
        currentPage = pageName;
    }

    // Handle polling based on page
    if (pageName === 'home' && isLoggedIn) {
        startChatPolling();
        startStatusPolling();
        stopHistoryPolling();
    } else if (pageName === 'server' && isLoggedIn) {
        stopChatPolling();
        stopStatusPolling();
        startHistoryPolling();
        startContributionPolling();
    } else {
        stopChatPolling();
        stopStatusPolling();
        stopHistoryPolling();
        stopContributionPolling();
    }

    // Load data for the page if needed
    if (pageName === 'maps' && isLoggedIn) {
        loadMaps();
    } else if (pageName === 'profile' && isLoggedIn) {
        loadMyInfo();
    } else if (pageName === 'home') {
        loadFeaturedMaps();
        if (isLoggedIn) {
            loadServerStatus();
        }
    }
}

// Start polling chat messages
function startChatPolling() {
    stopChatPolling();
    isFirstChatLoad = true;
    loadChatHistory();
    chatPollInterval = setInterval(loadChatHistory, 2000);
}

// Stop polling chat messages
function stopChatPolling() {
    if (chatPollInterval) {
        clearInterval(chatPollInterval);
        chatPollInterval = null;
    }
}

// Start polling server status
function startStatusPolling() {
    stopStatusPolling();
    loadServerStatus();
    statusPollInterval = setInterval(loadServerStatus, 2000);
}

// Stop polling server status
function stopStatusPolling() {
    if (statusPollInterval) {
        clearInterval(statusPollInterval);
        statusPollInterval = null;
    }
}

// Start polling history
function startHistoryPolling() {
    stopHistoryPolling();
    loadServerHistory();
    historyPollInterval = setInterval(loadServerHistory, 10000);
}

// Stop polling history
function stopHistoryPolling() {
    if (historyPollInterval) {
        clearInterval(historyPollInterval);
        historyPollInterval = null;
    }
}

// Start polling live contribution ranking
function startContributionPolling() {
    stopContributionPolling();
    loadContributionRanking();
    contributionPollInterval = setInterval(loadContributionRanking, 2000);
}

// Stop polling contribution ranking
function stopContributionPolling() {
    if (contributionPollInterval) {
        clearInterval(contributionPollInterval);
        contributionPollInterval = null;
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

        fetch('api/auth/login', {
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
    fetch('api/auth/logout')
        .then(() => {
            isLoggedIn = false;
            updateLoginState();
            showPage('home');
            stopChatPolling();
            stopStatusPolling();
            stopHistoryPolling();
        });
}

// Format Mindustry color tags (e.g. [#ff0000]Text) into styled spans
function formatMindustryColors(text) {
    if (!text) return '';
    let formatted = text;
    // Escaping HTML tags to prevent XSS
    formatted = formatted
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#x27;');

    let openSpans = 0;
    formatted = formatted.replace(/\[#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})\]/g, (match, color) => {
        let prefix = '</span>'.repeat(openSpans);
        openSpans = 1;
        return `${prefix}<span style="color: #${color}">`;
    });
    
    // Handle standard color names
    const standardColors = ['red', 'blue', 'green', 'yellow', 'orange', 'purple', 'cyan', 'white', 'grey', 'lightgray', 'darkgray'];
    standardColors.forEach(color => {
        const regex = new RegExp(`\\[${color}\\]`, 'g');
        formatted = formatted.replace(regex, () => {
            let prefix = '</span>'.repeat(openSpans);
            openSpans = 1;
            return `${prefix}<span style="color: ${color}">`;
        });
    });

    if (openSpans > 0) {
        formatted += '</span>'.repeat(openSpans);
    }
    return formatted;
}

// Create a map card element (vertical or horizontal layout)
// Open a fullscreen popup showing the full map image
function openMapImageModal(imageUrl, title) {
    let overlay = document.getElementById('map-image-modal');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'map-image-modal';
        overlay.className = 'map-image-modal';
        overlay.innerHTML = `
            <div class="map-image-modal-content">
                <i class="material-icons map-image-modal-close">close</i>
                <img class="map-image-modal-img" alt="">
                <div class="map-image-modal-title"></div>
            </div>
        `;
        document.body.appendChild(overlay);

        const close = () => overlay.classList.remove('open');
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay || e.target.closest('.map-image-modal-close')) close();
        });
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') close();
        });
    }

    overlay.querySelector('.map-image-modal-img').src = imageUrl;
    overlay.querySelector('.map-image-modal-title').innerHTML = title || '';
    overlay.classList.add('open');
}

function createMapCard(map, showDownload, horizontal = false) {
    const card = document.createElement('div');
    card.className = horizontal ? 'map-card-horizontal' : 'map-card-vertical';

    const imageUrl = map.preview || '';
    const authorText = window.i18n.translate('maps.author');
    const planetText = window.i18n.translate('maps.planet');
    const unknownText = window.i18n.translate('maps.unknown');
    const noDescriptionText = window.i18n.translate('maps.no.description');
    const downloadText = window.i18n.translate('maps.download');
    const uploaderText = window.i18n.translate('maps.uploader');

    // Theme selection depending on Planet
    const planetLower = (map.planet || 'serpulo').toLowerCase();
    const planetClass = planetLower === 'erekir' ? 'erekir-theme' : 'serpulo-theme';
    const planetDisplay = planetLower === 'erekir' ? 'Erekir' : 'Serpulo';

    const formattedAuthor = formatMindustryColors(map.author) || unknownText;
    const formattedName = formatMindustryColors(map.name);
    const votesText = window.i18n.translate('maps.votes', map.votes || 0);

    // Random image focus position, re-rolled on every render
    const bgPos = `${Math.floor(Math.random() * 101)}% ${Math.floor(Math.random() * 101)}%`;
    const imageStyle = imageUrl ? `style="background-image: url(${imageUrl}); background-position: ${bgPos}"` : '';

    if (horizontal) {
        card.innerHTML = `
            <div class="map-image-left ${planetClass}" ${imageStyle}>
                ${!imageUrl ? '<i class="material-icons">map</i>' : ''}
            </div>
            <div class="map-info-right">
                <div class="map-header">
                    <h3 class="map-name">${formattedName}</h3>
                    <i class="material-icons info-icon-btn" title="${map.description || noDescriptionText}">info_outline</i>
                </div>
                <div class="map-meta">
                    <p><strong>${authorText}:</strong> ${formattedAuthor}</p>
                    <p><strong>${planetText}:</strong> ${planetDisplay}</p>
                    ${map.uploader ? `<p><strong>${uploaderText}:</strong> ${map.uploader}</p>` : ''}
                    <p><strong>${votesText}</strong></p>
                </div>
                ${showDownload ? `
                <div class="map-actions">
                    <a href="api/maps/download/${encodeURIComponent(map.name)}" class="download-outline-btn">
                        <i class="material-icons">file_download</i> <span>${downloadText}</span>
                    </a>
                </div>
                ` : ''}
            </div>
        `;

        // Click card -> open full map image popup (only when an image exists)
        if (imageUrl) {
            card.classList.add('map-card-clickable');
            card.addEventListener('click', (e) => {
                // Ignore clicks on the download link/button
                if (e.target.closest('a')) return;
                openMapImageModal(imageUrl, formattedName);
            });
        }
    } else {
        card.innerHTML = `
            <div class="map-image-top ${planetClass}" ${imageStyle}>
                ${!imageUrl ? '<i class="material-icons">map</i>' : ''}
            </div>
            <div class="map-info-bottom">
                <div class="map-header">
                    <h3 class="map-name">${formattedName}</h3>
                    <i class="material-icons info-icon-btn" title="${map.description || noDescriptionText}">info_outline</i>
                </div>
                <div class="map-meta">
                    <p><strong>${authorText}:</strong> ${formattedAuthor}</p>
                    <p><strong>${planetText} [Planet]:</strong> ${planetDisplay}</p>
                    ${map.uploader ? `<p><strong>${uploaderText}:</strong> ${map.uploader}</p>` : ''}
                    <p><strong>${votesText}</strong></p>
                </div>
                <div class="map-actions">
                    <a href="api/maps/download/${encodeURIComponent(map.name)}" class="download-outline-btn-centered">
                        <i class="material-icons">file_download</i> <span>${downloadText}</span>
                    </a>
                </div>
            </div>
        `;
    }

    return card;
}

// Load currently running map for the home page (by checking api/server/status and matching with api/maps list)
function loadFeaturedMaps() {
    const latestMapContainer = document.getElementById('latest-map-container');
    if (!latestMapContainer) return;

    // First fetch server status to get the currently running map name
    fetch('api/server/status')
        .then(response => {
            if (response.ok) {
                return response.json();
            } else {
                throw new Error(window.i18n.translate('error.load.status'));
            }
        })
        .then(status => {
            const currentMapName = status.map;

            // Now fetch custom maps to find a match
            return fetch('api/maps')
                .then(response => {
                    if (response.ok) {
                        return response.json();
                    } else if (response.status === 401) {
                        // Not logged in
                        throw new Error(window.i18n.translate('error.auth.required'));
                    } else {
                        throw new Error(window.i18n.translate('error.load.maps'));
                    }
                })
                .then(maps => {
                    latestMapContainer.innerHTML = '';

                    // Find map in loaded custom maps
                    let matchedMap = maps.find(m => m.name === currentMapName);

                    if (!matchedMap) {
                        // Fallback placeholder map card if it's a default/built-in map
                        matchedMap = {
                            name: currentMapName || 'Unknown Map',
                            author: 'System',
                            description: window.i18n.translate('home.current_map.desc'),
                            planet: 'Serpulo',
                            votes: 0
                        };
                    }

                    const mapCard = createMapCard(matchedMap, true, false); // Vertical layout
                    latestMapContainer.appendChild(mapCard);
                });
        })
        .catch(error => {
            if (error.message === window.i18n.translate('error.auth.required')) {
                latestMapContainer.innerHTML = `
                    <div class="map-card-vertical" style="padding: 24px; text-align: center; background-color: white; border: 1px solid #e2e8f0; border-radius: var(--border-radius);">
                        <i class="material-icons" style="font-size: 48px; color: var(--text-muted); margin-bottom: 12px;">lock</i>
                        <h4 style="margin: 0 0 8px 0;">${window.i18n.translate('login.required')}</h4>
                        <p style="font-size: 13px; color: var(--text-muted); margin: 0 0 16px 0;">${window.i18n.translate('login.to.view')}</p>
                        <button class="download-outline-btn-centered" onclick="showPage('login')">
                            ${window.i18n.translate('login.button')}
                        </button>
                    </div>
                `;
            } else {
                latestMapContainer.innerHTML = `<p>${window.i18n.translate('maps.error', error.message)}</p>`;
            }
        });
}

// Load all maps for the maps page
function loadMaps() {
    const mapListContainer = document.getElementById('map-list');
    if (!mapListContainer) return;

    fetch('api/maps')
        .then(response => {
            if (response.status === 401) {
                isLoggedIn = false;
                updateLoginState();
                throw new Error(window.i18n.translate('error.not.logged.in'));
            }
            if (response.ok) {
                return response.json();
            } else {
                throw new Error(window.i18n.translate('error.load.maps'));
            }
        })
        .then(maps => {
            allMaps = maps;
            renderMaps();
        })
        .catch(error => {
            mapListContainer.innerHTML = `<p>${window.i18n.translate('maps.error', error.message)}</p>`;
        });
}

// Setup planet filters and search inputs
function setupMapFilters() {
    const searchInput = document.getElementById('map-search-input');
    const planetSelect = document.getElementById('map-planet-select');
    const sortLatestBtn = document.getElementById('sort-latest-btn');
    const sortPopularBtn = document.getElementById('sort-popular-btn');

    if (searchInput) searchInput.addEventListener('input', renderMaps);
    if (planetSelect) planetSelect.addEventListener('change', renderMaps);

    if (sortLatestBtn && sortPopularBtn) {
        sortLatestBtn.addEventListener('click', () => {
            sortLatestBtn.classList.add('active');
            sortPopularBtn.classList.remove('active');
            renderMaps();
        });
        sortPopularBtn.addEventListener('click', () => {
            sortPopularBtn.classList.add('active');
            sortLatestBtn.classList.remove('active');
            renderMaps();
        });
    }
}

// Filters & sorts maps and renders them on the Maps page
function renderMaps() {
    const mapListContainer = document.getElementById('map-list');
    if (!mapListContainer) return;

    const searchQuery = (document.getElementById('map-search-input')?.value || '').toLowerCase().trim();
    const selectedPlanet = document.getElementById('map-planet-select')?.value || 'all';
    const isLatestActive = document.getElementById('sort-latest-btn')?.classList.contains('active');

    // Filter maps array
    let filteredMaps = allMaps.filter(map => {
        const matchesSearch = !searchQuery || 
            (map.name || '').toLowerCase().includes(searchQuery) ||
            (map.author || '').toLowerCase().includes(searchQuery) ||
            (map.description || '').toLowerCase().includes(searchQuery);
            
        const matchesPlanet = selectedPlanet === 'all' || 
            (map.planet || 'serpulo').toLowerCase() === selectedPlanet;

        return matchesSearch && matchesPlanet;
    });

    // Sort maps array
    if (!isLatestActive) {
        // Sort by votes score descending (MapVoting/Rating data popularity)
        filteredMaps.sort((a, b) => (b.votes || 0) - (a.votes || 0));
    }

    mapListContainer.innerHTML = '';

    if (filteredMaps.length === 0) {
        mapListContainer.innerHTML = `<p style="grid-column: span 2; text-align: center; color: var(--text-muted); padding: 40px 0;">${window.i18n.translate('maps.none')}</p>`;
        return;
    }

    filteredMaps.forEach(map => {
        const mapCard = createMapCard(map, true, true); // Horizontal card layout
        mapListContainer.appendChild(mapCard);
    });
}

// Set up map upload dialog actions
function setupMapUpload() {
    const uploadBtnShortcut = document.getElementById('upload-card-shortcut');
    const uploadBtnTop = document.getElementById('upload-map-btn-top');
    const uploadDialog = document.getElementById('upload-dialog');
    const uploadForm = document.getElementById('upload-form');
    const uploadError = document.getElementById('upload-error');
    const fileInput = document.getElementById('map-file');
    const browseBtn = document.getElementById('browse-btn');
    const fileInfo = document.getElementById('file-info');
    const fileName = document.getElementById('file-name');
    const fileDropArea = document.getElementById('file-drop-area');

    // Helper to open upload dialog modal
    const openUploadModal = () => {
        if (uploadDialog) {
            uploadDialog.showModal();
            if (uploadError) uploadError.style.display = 'none';
            if (uploadForm) uploadForm.reset();
            if (fileInfo) fileInfo.style.display = 'none';
        }
    };

    if (uploadBtnShortcut) uploadBtnShortcut.addEventListener('click', openUploadModal);
    if (uploadBtnTop) uploadBtnTop.addEventListener('click', openUploadModal);

    // Setup browse button triggers hidden file input
    if (browseBtn && fileInput) {
        browseBtn.addEventListener('click', () => {
            fileInput.click();
        });
    }

    // Trigger file name display on select
    if (fileInput) {
        fileInput.addEventListener('change', () => {
            if (fileInput.files[0]) {
                if (fileName) fileName.textContent = fileInput.files[0].name;
                if (fileInfo) fileInfo.style.display = 'flex';
            } else {
                if (fileInfo) fileInfo.style.display = 'none';
            }
        });
    }

    // Drag and drop event listeners
    if (fileDropArea && fileInput) {
        ['dragenter', 'dragover'].forEach(eventName => {
            fileDropArea.addEventListener(eventName, (e) => {
                e.preventDefault();
                fileDropArea.classList.add('dragover');
            }, false);
        });

        ['dragleave', 'drop'].forEach(eventName => {
            fileDropArea.addEventListener(eventName, (e) => {
                e.preventDefault();
                fileDropArea.classList.remove('dragover');
            }, false);
        });

        fileDropArea.addEventListener('drop', (e) => {
            const dt = e.dataTransfer;
            const files = dt.files;
            if (files.length > 0) {
                fileInput.files = files;
                // Trigger change event manually
                const event = new Event('change');
                fileInput.dispatchEvent(event);
            }
        });
    }

    // Close Dialog Modal triggers
    document.getElementById('upload-cancel').addEventListener('click', () => {
        uploadDialog.close();
    });
    document.getElementById('upload-close-icon').addEventListener('click', () => {
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

        fetch('api/maps/upload', {
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

// Render dynamic player item row
function renderPlayerItem(player) {
    const playerItem = document.createElement('li');
    playerItem.className = 'player-item';
    
    // Clean player name from color tags
    const cleanName = formatMindustryColors(player.name);
    const durationText = player.playTime;

    playerItem.innerHTML = `
        <div class="player-name-wrapper">
            <span class="player-dot"></span>
            <span>${cleanName}</span>
        </div>
        <span class="player-duration">${durationText}</span>
    `;
    return playerItem;
}

// Update server dashboard controls (gauge, waves, player count, time)
function updateServerDashboard(data) {
    currentServerMode = data.mode || 'none';
    // 1. Update TPS gauge needle
    const tps = data.tps;
    document.getElementById('server-tps').textContent = tps.toFixed(1);
    
    const needle = document.getElementById('tps-needle');
    if (needle) {
        // Map 0-60 TPS range to -90deg to +90deg rotation
        const clampedTps = Math.min(Math.max(tps, 0), 60);
        const deg = -90 + (clampedTps / 60) * 180;
        needle.style.transform = `rotate(${deg}deg)`;
    }

    // 2. Update stats values
    const modeStat = document.getElementById('server-mode-stat');
    const modeLabel = document.getElementById('server-mode-label');
    const modeValue = document.getElementById('server-mode-value');

    if (modeStat && modeLabel && modeValue) {
        if (data.mode === 'wave') {
            modeStat.style.display = 'flex';
            modeLabel.setAttribute('data-i18n', 'server.wave');
            modeLabel.textContent = window.i18n.translate('server.wave');
            modeValue.innerHTML = `${window.i18n.translate('server.wave')} <span id="server-wave">${data.wave}</span>`;
        } else if (data.mode === 'pvp') {
            modeStat.style.display = 'flex';
            modeLabel.setAttribute('data-i18n', 'server.active_teams');
            modeLabel.textContent = window.i18n.translate('server.active_teams');
            modeValue.innerHTML = `<span id="server-wave">${data.activeTeams}</span>`;
        } else {
            modeStat.style.display = 'none';
        }
    }

    document.getElementById('server-time').textContent = data.gameTime;
    document.getElementById('server-players-count').textContent = `${data.players.length} players`;

    // 3. Update player list
    const playerList = document.getElementById('player-list');
    if (playerList) {
        playerList.innerHTML = '';
        if (data.players.length === 0) {
            playerList.innerHTML = `<li class="player-item loading">${window.i18n.translate('server.no.players')}</li>`;
            return;
        }

        data.players.forEach(player => {
            const playerItem = renderPlayerItem(player);
            playerList.appendChild(playerItem);
        });
    }
}

// Load server status
function loadServerStatus() {
    if (!isLoggedIn) return;

    fetch('api/server/status')
        .then(response => {
            if (response.ok) {
                return response.json();
            } else {
                throw new Error(window.i18n.translate('error.load.status'));
            }
        })
        .then(status => {
            updateServerDashboard(status);
        })
        .catch(error => {
            console.error('Error loading server status:', error);
        });
}

// Split CamelCase achievement enum names into readable words
function prettifyAchievementName(name) {
    if (!name) return '';
    return name
        .replace(/([a-z])([A-Z])/g, '$1 $2')
        .replace(/([A-Z]+)([A-Z][a-z])/g, '$1 $2');
}

// Format an ISO LocalDateTime string (e.g. 2024-01-05T12:34:56) to "YYYY-MM-DD HH:MM"
function formatDateTime(isoStr) {
    if (!isoStr) return '-';
    const m = isoStr.match(/(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/);
    if (!m) return isoStr;
    return `${m[1]}-${m[2]}-${m[3]} ${m[4]}:${m[5]}`;
}

// Load personal player data for the My Info page
function loadMyInfo() {
    if (!isLoggedIn) return;

    const container = document.getElementById('profile-content');
    if (!container) return;

    container.innerHTML = '<div class="loading-spinner"></div>';

    fetch('api/me')
        .then(response => {
            if (response.status === 401) {
                isLoggedIn = false;
                updateLoginState();
                throw new Error(window.i18n.translate('error.not.logged.in'));
            }
            if (response.ok) {
                return response.json();
            } else {
                throw new Error(window.i18n.translate('profile.error'));
            }
        })
        .then(info => renderMyInfo(info))
        .catch(error => {
            container.innerHTML = `<p style="text-align: center; color: var(--text-muted); padding: 40px 0;">${error.message}</p>`;
        });
}

// Render the personal data and achievement progress
function renderMyInfo(info) {
    const container = document.getElementById('profile-content');
    if (!container) return;

    const t = (k, ...p) => window.i18n.translate(k, ...p);

    // Account info rows
    const accountRows = [
        { label: t('profile.uuid'), value: info.uuid, copy: true },
        { label: t('profile.permission'), value: info.permission },
        { label: t('profile.joined'), value: formatDateTime(info.firstPlayed) },
        { label: t('profile.lastlogin'), value: formatDateTime(info.lastLogin) },
        { label: t('profile.playtime'), value: info.totalPlayed }
    ];

    // Stat cards
    const stats = [
        { label: t('profile.blocks_placed'), value: info.blockPlaceCount.toLocaleString(), icon: 'add_box' },
        { label: t('profile.blocks_broken'), value: info.blockBreakCount.toLocaleString(), icon: 'delete' },
        { label: t('profile.level'), value: info.level.toLocaleString(), icon: 'military_tech' },
        { label: t('profile.attendance'), value: info.attendanceDays.toLocaleString(), icon: 'event_available' },
        { label: t('profile.pvp_win'), value: info.pvpWinCount.toLocaleString(), icon: 'emoji_events' },
        { label: t('profile.pvp_lose'), value: info.pvpLoseCount.toLocaleString(), icon: 'sentiment_dissatisfied' },
        { label: t('profile.pvp_winrate'), value: `${info.pvpWinRate}%`, icon: 'percent' },
        { label: t('profile.wave_clear'), value: info.waveClear.toLocaleString(), icon: 'waves' },
        { label: t('profile.attack_clear'), value: info.attackClear.toLocaleString(), icon: 'swords' }
    ];

    // EXP bar (placed at the very top)
    const expMax = info.expMax > 0 ? info.expMax : Math.max(info.exp, 1);
    const expPct = Math.min(100, Math.round((info.exp / expMax) * 100));
    const expHtml = `
        <div class="card profile-exp-card">
            <div class="profile-exp-head">
                <span class="profile-exp-label">${t('profile.exp')} <span class="profile-exp-level">Lv. ${info.level}</span></span>
                <span class="profile-exp-count">${info.exp.toLocaleString()} / ${expMax.toLocaleString()}</span>
            </div>
            <div class="exp-bar">
                <div class="exp-bar-fill" style="width: ${expPct}%;"></div>
            </div>
        </div>
    `;

    const accountHtml = accountRows.map(r => `
        <div class="profile-info-row">
            <span class="profile-info-label">${r.label}</span>
            <span class="profile-info-value${r.copy ? ' copyable' : ''}"${r.copy ? ` title="${r.value}" data-copy="${r.value}"` : ''}>${r.value}</span>
        </div>
    `).join('');

    const statsHtml = stats.map(s => `
        <div class="profile-stat-card">
            <i class="material-icons">${s.icon}</i>
            <span class="profile-stat-value">${s.value}</span>
            <span class="profile-stat-label">${s.label}</span>
        </div>
    `).join('');

    // Achievements (completed first, then by progress percentage)
    const achievements = (info.achievements || []).slice().sort((a, b) => {
        if (a.completed !== b.completed) return a.completed ? -1 : 1;
        const pa = a.target > 0 ? a.current / a.target : 0;
        const pb = b.target > 0 ? b.current / b.target : 0;
        return pb - pa;
    });

    const achievementsHtml = achievements.map(a => {
        const pct = a.completed ? 100 : (a.target > 0 ? Math.min(100, Math.round((a.current / a.target) * 100)) : 0);
        const title = a.title || prettifyAchievementName(a.name);
        const description = a.description && a.description !== a.name ? a.description : '';
        // Cleared achievements show the localized goal at the bottom; otherwise show current/target progress
        const goalText = a.goal ? a.goal : `${t('profile.target')}: ${a.target.toLocaleString()}`;
        const footer = a.completed
            ? `<div class="achievement-footer completed">${goalText}</div>`
            : `<div class="achievement-footer">${a.current.toLocaleString()} / ${a.target.toLocaleString()}</div>`;
        const sparkle = (a.hidden && a.completed) ? ' hidden-cleared' : '';
        return `
        <div class="achievement-item${a.completed ? ' completed' : ''}${sparkle}">
            <div class="achievement-head">
                <span class="achievement-name">
                    <i class="material-icons">${a.completed ? 'check_circle' : 'lock_open'}</i>
                    ${title}
                </span>
            </div>
            ${description ? `<p class="achievement-desc">${description}</p>` : ''}
            <div class="achievement-bar">
                <div class="achievement-bar-fill" style="width: ${pct}%;"></div>
            </div>
            ${footer}
        </div>`;
    }).join('');

    container.innerHTML = `
        ${expHtml}
        <div class="profile-grid">
            <div class="card profile-card">
                <h3 class="card-title">${t('profile.account')}</h3>
                <div class="profile-info-list">${accountHtml}</div>
            </div>
            <div class="card profile-card">
                <h3 class="card-title">${t('profile.stats')}</h3>
                <div class="profile-stats-grid">${statsHtml}</div>
            </div>
        </div>
        <div class="card profile-card profile-achievements-card">
            <div class="chart-header">
                <h3 class="card-title">${t('profile.achievements')}</h3>
                <span class="chart-current-val">${t('profile.achievements_progress', info.achievementsCompleted, info.achievementsTotal)}</span>
            </div>
            <div class="achievement-list">${achievementsHtml}</div>
        </div>
    `;

    // Click-to-copy UUID
    container.querySelectorAll('[data-copy]').forEach(el => {
        el.addEventListener('click', () => {
            const text = el.getAttribute('data-copy');
            if (navigator.clipboard) {
                navigator.clipboard.writeText(text).then(() => {
                    showSnackbar(window.i18n.translate('profile.copied'));
                });
            }
        });
    });
}

// Render dynamic chat bubble row
function renderChatMessage(player, text, isOwn, isWeb = false) {
    const row = document.createElement('div');
    row.className = isOwn ? 'chat-row own' : 'chat-row';

    const bubble = document.createElement('div');
    bubble.className = isWeb ? 'chat-bubble web-message' : 'chat-bubble';

    const cleanPlayer = formatMindustryColors(player);
    const cleanText = formatMindustryColors(text);

    const webBadge = isWeb ? `<span class="web-chat-tag">WEB</span>` : '';

    bubble.innerHTML = `
        <span class="chat-name-badge">${webBadge}${cleanPlayer}</span>
        <span class="chat-text-bubble">${cleanText}</span>
    `;

    row.appendChild(bubble);
    return row;
}

// Load chat history
function loadChatHistory() {
    if (!isLoggedIn) return;

    fetch('api/server/chat')
        .then(response => {
            if (response.status === 401) {
                isLoggedIn = false;
                updateLoginState();
                throw new Error(window.i18n.translate('error.not.logged.in'));
            }
            if (response.ok) {
                return response.json();
            } else {
                throw new Error(window.i18n.translate('error.load.chat'));
            }
        })
        .then(messages => {
            const currentJson = JSON.stringify(messages);
            if (currentJson === lastChatHistoryJson) {
                return; // skip DOM update to avoid flicker
            }
            lastChatHistoryJson = currentJson;

            const chatMessages = document.getElementById('chat-messages');
            if (!chatMessages) return;

            // Check if user was scrolled to bottom before appending
            const isScrolledToBottom = isFirstChatLoad || (chatMessages.scrollHeight - chatMessages.clientHeight <= chatMessages.scrollTop + 30);

            chatMessages.innerHTML = '';

            if (messages.length === 0) {
                chatMessages.innerHTML = `<p class="chat-placeholder">${window.i18n.translate('server.chat.none')}</p>`;
                isFirstChatLoad = false;
                return;
            }

            // Render message logs
            messages.forEach(message => {
                // Determine if message is from the logged-in user (simple comparison)
                const isOwn = message.isWeb && message.player === currentUsername;
                const messageElement = renderChatMessage(message.player, message.message, isOwn, message.isWeb);
                chatMessages.appendChild(messageElement);
            });

            // Scroll to bottom if was scrolled to bottom or first load
            if (isScrolledToBottom) {
                chatMessages.scrollTop = chatMessages.scrollHeight;
            }
            isFirstChatLoad = false;
        })
        .catch(error => {
            console.error('Error loading chat history:', error);
        });
}

// Set up chat form
function setupChatForm() {
    const chatForm = document.getElementById('chat-form');
    const chatInput = document.getElementById('chat-input');

    if (!chatForm || !chatInput) return;

    chatForm.addEventListener('submit', event => {
        event.preventDefault();

        const message = chatInput.value.trim();
        if (!message) return;

        fetch('api/server/chat', {
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
                if (chatMessages) {
                    // Remove placeholder if active
                    const placeholder = chatMessages.querySelector('.chat-placeholder');
                    if (placeholder) placeholder.remove();

                    const messageElement = renderChatMessage(window.i18n.translate('server.chat.you'), message, true, true);
                    chatMessages.appendChild(messageElement);

                    // Scroll to bottom
                    chatMessages.scrollTop = chatMessages.scrollHeight;
                }
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

// Helper function to format time in minutes to MM:SS
function formatTime(minutes) {
    const mins = Math.floor(minutes);
    const secs = Math.floor((minutes - mins) * 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
}

// Show custom snackbar message
function showSnackbar(message) {
    let snackbar = document.getElementById('custom-snackbar');
    if (!snackbar) {
        snackbar = document.createElement('div');
        snackbar.id = 'custom-snackbar';
        snackbar.className = 'custom-snackbar';
        document.body.appendChild(snackbar);
    }
    snackbar.textContent = message;
    
    // Force browser reflow to reset transitions
    void snackbar.offsetWidth;
    snackbar.classList.add('show');
    
    if (window.snackbarTimeout) {
        clearTimeout(window.snackbarTimeout);
    }
    
    window.snackbarTimeout = setTimeout(() => {
        snackbar.classList.remove('show');
    }, 3000);
}

// Load server status history
function loadServerHistory() {
    if (!isLoggedIn) return;

    fetch('api/server/history')
        .then(response => {
            if (response.status === 401) {
                isLoggedIn = false;
                updateLoginState();
                throw new Error('Unauthorized');
            }
            if (response.ok) {
                return response.json();
            } else {
                throw new Error('Failed to load server history');
            }
        })
        .then(history => {
            // Draw standard charts
            drawChart('tps-chart-container', history, p => p.tps, '#3b82f6');
            drawChart('players-chart-container', history, p => p.players, '#10b981');

            const resourceCard = document.getElementById('resource-chart-card');
            const resourceTitle = document.getElementById('chart-title-resources');
            const unitsTitle = document.getElementById('chart-title-units');
            const buildingsTitle = document.getElementById('chart-title-buildings');

            let mode = currentServerMode;
            if (mode === 'none' && history.length > 0) {
                const latest = history[history.length - 1];
                if (latest.teamResources && Object.keys(latest.teamResources).length > 0) {
                    mode = 'pvp';
                } else if (latest.resources && Object.keys(latest.resources).length > 0) {
                    mode = 'wave';
                }
            }

            if (mode === 'pvp') {
                if (resourceCard) resourceCard.style.display = 'flex';
                if (resourceTitle) resourceTitle.textContent = '팀별 자원량';
                if (unitsTitle) unitsTitle.textContent = '팀별 유닛 수';
                if (buildingsTitle) buildingsTitle.textContent = '팀별 건물 수';

                drawMultiLineChart('resources-chart-container', history, 'teamResources');
                drawMultiLineChart('units-chart-container', history, 'teamUnits');
                drawMultiLineChart('buildings-chart-container', history, 'teamBuildings');
            } else if (mode === 'wave') {
                if (resourceCard) resourceCard.style.display = 'flex';
                if (resourceTitle) resourceTitle.textContent = '코어 자원량';
                if (unitsTitle) unitsTitle.textContent = '맵 유닛 수';
                if (buildingsTitle) buildingsTitle.textContent = '맵 건물 수';

                drawMultiLineChart('resources-chart-container', history, 'resources');
                drawChart('units-chart-container', history, p => p.units, '#f59e0b');
                drawChart('buildings-chart-container', history, p => p.buildings, '#f97316');
            } else {
                if (resourceCard) resourceCard.style.display = 'none';
                if (unitsTitle) unitsTitle.textContent = '맵 유닛 수';
                if (buildingsTitle) buildingsTitle.textContent = '맵 건물 수';

                drawChart('units-chart-container', history, p => p.units, '#f59e0b');
                drawChart('buildings-chart-container', history, p => p.buildings, '#f97316');
            }

            // Update current values in top-right of cards
            if (history.length > 0) {
                const latest = history[history.length - 1];
                document.getElementById('chart-current-tps').textContent = latest.tps.toFixed(1);
                document.getElementById('chart-current-players').textContent = latest.players;

                if (mode === 'pvp') {
                    const totalRes = Object.values(latest.teamResources || {}).reduce((a, b) => a + b, 0);
                    document.getElementById('chart-current-resources').textContent = totalRes.toLocaleString();
                    document.getElementById('chart-current-units').textContent = latest.units;
                    document.getElementById('chart-current-buildings').textContent = latest.buildings;
                } else if (mode === 'wave') {
                    const totalRes = Object.values(latest.resources || {}).reduce((a, b) => a + b, 0);
                    document.getElementById('chart-current-resources').textContent = totalRes.toLocaleString();
                    document.getElementById('chart-current-units').textContent = latest.units;
                    document.getElementById('chart-current-buildings').textContent = latest.buildings;
                } else {
                    document.getElementById('chart-current-units').textContent = latest.units;
                    document.getElementById('chart-current-buildings').textContent = latest.buildings;
                }
            } else {
                document.getElementById('chart-current-tps').textContent = '--';
                document.getElementById('chart-current-players').textContent = '--';
                if (document.getElementById('chart-current-resources')) {
                    document.getElementById('chart-current-resources').textContent = '--';
                }
                document.getElementById('chart-current-units').textContent = '--';
                document.getElementById('chart-current-buildings').textContent = '--';
            }
        })
        .catch(error => {
            console.error('Error loading server history:', error);
        });
}

// Load and render the live contribution list (current online players)
function loadContributionRanking() {
    const container = document.getElementById('contribution-ranking');
    if (!container) return;

    fetch('api/server/contribution')
        .then(response => {
            if (response.status === 403) {
                const card = document.getElementById('contribution-card');
                if (card) card.style.display = 'none';
                stopContributionPolling();
                return null;
            }
            if (response.ok) {
                return response.json();
            }
            throw new Error('Failed to load contribution ranking');
        })
        .then(ranking => {
            if (ranking === null) return;
            const card = document.getElementById('contribution-card');
            if (card) card.style.display = '';

            if (!ranking || ranking.length === 0) {
                container.innerHTML = `<div class="chart-no-data">${window.i18n.translate('server.contribution.empty')}</div>`;
                return;
            }

            const gamesLabel = window.i18n.translate('server.contribution.games');
            const avgLabel = window.i18n.translate('server.contribution.average');

            const escapeHtml = (s) => String(s)
                .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

            // Render one player's bar row. maxCurrent = baseline for 100%, barColor = fill color.
            const renderRow = (entry, maxCurrent, barColor) => {
                const pct = maxCurrent > 0 ? Math.max(0, entry.current) / maxCurrent * 100 : 0;
                const fillStyle = barColor
                    ? `width: ${pct.toFixed(1)}%; background: ${barColor};`
                    : `width: ${pct.toFixed(1)}%;`;
                return `
                    <div class="contribution-row">
                        <div class="contribution-info">
                            <span class="contribution-name">${escapeHtml(entry.name)}</span>
                            <span class="contribution-values">
                                <span class="contribution-current">${entry.current.toFixed(1)}</span>
                                <span class="contribution-avg">${avgLabel} ${entry.average.toFixed(1)} (${entry.games} ${gamesLabel})</span>
                            </span>
                        </div>
                        <div class="contribution-bar-track">
                            <div class="contribution-bar-fill" style="${fillStyle}"></div>
                        </div>
                    </div>`;
            };

            const isPvp = ranking.some(e => e.team);

            if (isPvp) {
                // Group by team; within each team sort by current; per-team top = 100% baseline.
                const teams = {};
                ranking.forEach(e => {
                    const key = e.team || 'unknown';
                    if (!teams[key]) teams[key] = { color: e.teamColor, players: [] };
                    teams[key].players.push(e);
                });

                // Order teams by their top player's current contribution.
                const orderedTeams = Object.entries(teams).sort((a, b) => {
                    const aMax = Math.max(...a[1].players.map(p => p.current), 0);
                    const bMax = Math.max(...b[1].players.map(p => p.current), 0);
                    return bMax - aMax;
                });

                container.innerHTML = orderedTeams.map(([teamName, team]) => {
                    const sorted = team.players.slice().sort((a, b) => b.current - a.current);
                    const maxCurrent = Math.max(...sorted.map(p => p.current), 0);
                    const color = team.color || '#10b981';
                    const rows = sorted.map(p => renderRow(p, maxCurrent, color)).join('');
                    return `
                        <div class="contribution-team">
                            <div class="contribution-team-header" style="color: ${color};">
                                <span class="contribution-team-dot" style="background: ${color};"></span>
                                ${escapeHtml(teamName)}
                            </div>
                            ${rows}
                        </div>`;
                }).join('');
            } else {
                // Non-PvP: single list; the top player's current = 100% baseline.
                const sorted = ranking.slice().sort((a, b) => b.current - a.current);
                const maxCurrent = Math.max(...sorted.map(e => e.current), 0);
                container.innerHTML = sorted.map(e => renderRow(e, maxCurrent, null)).join('');
            }
        })
        .catch(error => {
            console.error('Error loading contribution ranking:', error);
            container.innerHTML = `<div class="chart-no-data">${window.i18n.translate('server.contribution.empty')}</div>`;
        });
}

// Custom inline SVG area/line chart rendering logic
function drawChart(containerId, dataPoints, getValueFn, color) {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.innerHTML = '';

    // Remove existing legend from the card if it exists
    const card = container.parentElement;
    if (card) {
        const existingLegend = card.querySelector('.chart-legend');
        if (existingLegend) {
            existingLegend.remove();
        }
    }

    const width = container.clientWidth || 400;
    const height = width / 3;

    if (dataPoints.length === 0) {
        container.innerHTML = `<div class="chart-no-data">${window.i18n.translate('server.chart.no_data')}</div>`;
        return;
    }

    // Determine min/max values to scale properly
    const values = dataPoints.map(getValueFn);
    let minVal = Math.min(...values);
    let maxVal = Math.max(...values);

    // Padding / rounding for nice display
    if (maxVal === minVal) {
        maxVal += 1;
        minVal -= 1;
    }
    if (minVal < 0) minVal = 0;

    // Add some buffer to top and bottom
    const range = maxVal - minVal;
    maxVal += range * 0.1;
    minVal -= range * 0.1;
    if (minVal < 0) minVal = 0;

    const paddingLeft = 40;
    const paddingRight = 15;
    const paddingTop = 15;
    const paddingBottom = 25;

    const chartWidth = width - paddingLeft - paddingRight;
    const chartHeight = height - paddingTop - paddingBottom;

    // Build SVG
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('width', '100%');
    svg.setAttribute('height', '100%');
    svg.setAttribute('viewBox', `0 0 ${width} ${height}`);
    svg.style.overflow = 'visible';

    // Gradients
    const gradientId = `grad-${containerId}`;
    svg.innerHTML = `
        <defs>
            <linearGradient id="${gradientId}" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="${color}" stop-opacity="0.3"/>
                <stop offset="100%" stop-color="${color}" stop-opacity="0.0"/>
            </linearGradient>
        </defs>
    `;

    // Draw horizontal grid lines
    const gridLines = 3;
    for (let i = 0; i <= gridLines; i++) {
        const val = minVal + (maxVal - minVal) * (i / gridLines);
        const y = paddingTop + chartHeight * (1 - i / gridLines);
        
        // Grid line
        const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
        line.setAttribute('x1', paddingLeft);
        line.setAttribute('y1', y);
        line.setAttribute('x2', width - paddingRight);
        line.setAttribute('y2', y);
        line.setAttribute('stroke', '#e2e8f0');
        line.setAttribute('stroke-dasharray', '3,3');
        svg.appendChild(line);

        // Y-axis label
        const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
        text.setAttribute('x', paddingLeft - 8);
        text.setAttribute('y', y + 4);
        text.setAttribute('text-anchor', 'end');
        text.setAttribute('font-size', '10');
        text.setAttribute('fill', '#94a3b8');
        text.setAttribute('font-family', 'var(--font-primary)');
        text.textContent = Math.round(val);
        svg.appendChild(text);
    }

    // Map data points to coordinates
    const coords = [];
    const step = dataPoints.length > 1 ? chartWidth / (dataPoints.length - 1) : chartWidth;
    
    dataPoints.forEach((p, idx) => {
        const val = getValueFn(p);
        const x = paddingLeft + idx * step;
        const y = paddingTop + chartHeight * (1 - (val - minVal) / (maxVal - minVal));
        coords.push({ x, y });
    });

    if (coords.length > 0) {
        // Line path
        let dLine = `M ${coords[0].x} ${coords[0].y}`;
        for (let i = 1; i < coords.length; i++) {
            dLine += ` L ${coords[i].x} ${coords[i].y}`;
        }

        // Area path (closed polygon)
        const dArea = `${dLine} L ${coords[coords.length - 1].x} ${paddingTop + chartHeight} L ${coords[0].x} ${paddingTop + chartHeight} Z`;

        // Draw area
        const areaPath = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        areaPath.setAttribute('d', dArea);
        areaPath.setAttribute('fill', `url(#${gradientId})`);
        svg.appendChild(areaPath);

        // Draw line
        const linePath = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        linePath.setAttribute('d', dLine);
        linePath.setAttribute('fill', 'none');
        linePath.setAttribute('stroke', color);
        linePath.setAttribute('stroke-width', '2.5');
        linePath.setAttribute('stroke-linecap', 'round');
        linePath.setAttribute('stroke-linejoin', 'round');
        svg.appendChild(linePath);

        // Draw point circles on hover or just simple points for the last one
        const last = coords[coords.length - 1];
        const pulseOuter = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
        pulseOuter.setAttribute('cx', last.x);
        pulseOuter.setAttribute('cy', last.y);
        pulseOuter.setAttribute('r', '6');
        pulseOuter.setAttribute('fill', color);
        pulseOuter.setAttribute('opacity', '0.4');
        svg.appendChild(pulseOuter);

        const pulseInner = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
        pulseInner.setAttribute('cx', last.x);
        pulseInner.setAttribute('cy', last.y);
        pulseInner.setAttribute('r', '3');
        pulseInner.setAttribute('fill', 'white');
        pulseInner.setAttribute('stroke', color);
        pulseInner.setAttribute('stroke-width', '2');
        svg.appendChild(pulseInner);
    }

    // Draw X-axis time labels (first, middle, last)
    if (dataPoints.length > 0) {
        const labelsToDraw = [];
        labelsToDraw.push({ idx: 0, align: 'start' });
        if (dataPoints.length > 2) {
            labelsToDraw.push({ idx: Math.floor(dataPoints.length / 2), align: 'middle' });
        }
        if (dataPoints.length > 1) {
            labelsToDraw.push({ idx: dataPoints.length - 1, align: 'end' });
        }

        labelsToDraw.forEach(item => {
            const p = dataPoints[item.idx];
            const coord = coords[item.idx];
            const date = new Date(p.time);
            const hours = date.getHours().toString().padStart(2, '0');
            const minutes = date.getMinutes().toString().padStart(2, '0');
            const timeStr = `${hours}:${minutes}`;

            const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
            text.setAttribute('x', coord.x);
            text.setAttribute('y', paddingTop + chartHeight + 16);
            text.setAttribute('text-anchor', item.align);
            text.setAttribute('font-size', '10');
            text.setAttribute('fill', '#94a3b8');
            text.setAttribute('font-family', 'var(--font-primary)');
            text.textContent = timeStr;
            svg.appendChild(text);
        });
    }

    container.appendChild(svg);
}

const MINDISTRY_COLORS = {
    // Teams
    'sharded': '#f2af13',
    'crux': '#e23f3f',
    'derelict': '#8c8c8c',
    'green': '#3da23d',
    'blue': '#2563eb',
    'purple': '#8b5cf6',
    
    // Items
    'copper': '#d99d73',
    'lead': '#ab99bc',
    'metaglass': '#94a3b8',
    'graphite': '#475569',
    'sand': '#f5e0b3',
    'coal': '#1e293b',
    'titanium': '#85a3e0',
    'thorium': '#f472b6',
    'scrap': '#64748b',
    'silicon': '#334155',
    'plastanium': '#a3e635',
    'phase-fabric': '#fbbf24',
    'surge-alloy': '#facc15',
    'blast-compound': '#ef4444',
    'pyratite': '#f97316',
    'spore-pod': '#a855f7',
    'beryl': '#86efac',
    'tungsten': '#cbd5e1',
    'oxide': '#e2e8f0',
    'carbonate': '#dfd2bc'
};

const KEY_TRANSLATIONS = {
    // Teams
    'sharded': '샤디드 (Sharded)',
    'crux': '크럭스 (Crux)',
    'derelict': '버려진 팀 (Derelict)',
    'green': '그린 팀 (Green)',
    'blue': '블루 팀 (Blue)',
    'purple': '퍼플 팀 (Purple)',
    
    // Items
    'copper': '구리 (Copper)',
    'lead': '납 (Lead)',
    'metaglass': '메타글라스 (Metaglass)',
    'graphite': '흑연 (Graphite)',
    'sand': '모래 (Sand)',
    'coal': '석탄 (Coal)',
    'titanium': '티타늄 (Titanium)',
    'thorium': '토륨 (Thorium)',
    'scrap': '고철 (Scrap)',
    'silicon': '실리콘 (Silicon)',
    'plastanium': '플라스타늄 (Plastanium)',
    'phase-fabric': '설상 천 (Phase Fabric)',
    'surge-alloy': '서지 합금 (Surge Alloy)',
    'blast-compound': '폭발물 (Blast Compound)',
    'pyratite': '파이라타이트 (Pyratite)',
    'spore-pod': '포자 (Spore Pod)',
    'beryl': '녹주석 (Beryl)',
    'tungsten': '텅스텐 (Tungsten)',
    'oxide': '산화물 (Oxide)',
    'carbonate': '탄산염 (Carbonate)'
};

function getColor(key) {
    const lower = key.toLowerCase();
    if (MINDISTRY_COLORS[lower]) {
        return MINDISTRY_COLORS[lower];
    }
    let hash = 0;
    for (let i = 0; i < key.length; i++) {
        hash = key.charCodeAt(i) + ((hash << 5) - hash);
    }
    const h = Math.abs(hash % 360);
    return `hsl(${h}, 70%, 55%)`;
}

function translateKey(key) {
    const lower = key.toLowerCase();
    return KEY_TRANSLATIONS[lower] || key;
}

function drawMultiLineChart(containerId, dataPoints, dataKey) {
    const container = document.getElementById(containerId);
    if (!container) return;

    // Save dataPoints and dataKey for redraws
    container._dataPoints = dataPoints;
    container._dataKey = dataKey;

    // Initialize selection states if they don't exist
    if (!container._selectedKeys) {
        container._selectedKeys = {};
    }

    const isSelected = (key) => {
        return container._selectedKeys[key] !== false;
    };

    container.innerHTML = '';

    // Remove existing legend from the card if it exists
    const card = container.parentElement;
    if (card) {
        const existingLegend = card.querySelector('.chart-legend');
        if (existingLegend) {
            existingLegend.remove();
        }
    }

    const width = container.clientWidth || 600;
    const svgHeight = width / 3;

    if (dataPoints.length === 0) {
        container.innerHTML = `<div class="chart-no-data">${window.i18n.translate('server.chart.no_data')}</div>`;
        return;
    }

    // Find active keys: keys that exist and have a value > 0 at least once
    const allKeys = new Set();
    dataPoints.forEach(p => {
        const map = p[dataKey];
        if (map) {
            Object.keys(map).forEach(k => allKeys.add(k));
        }
    });

    const activeKeys = [];
    allKeys.forEach(k => {
        const everPositive = dataPoints.some(p => {
            const map = p[dataKey];
            return map && map[k] > 0;
        });
        if (everPositive) {
            activeKeys.push(k);
        }
    });

    if (activeKeys.length === 0) {
        container.innerHTML = `<div class="chart-no-data">${window.i18n.translate('server.chart.no_data')}</div>`;
        return;
    }

    // Find global min and max
    let minVal = 0;
    let maxVal = 0;
    let hasData = false;

    dataPoints.forEach(p => {
        const map = p[dataKey];
        if (map) {
            activeKeys.forEach(k => {
                const val = map[k] || 0;
                if (!hasData) {
                    minVal = val;
                    maxVal = val;
                    hasData = true;
                } else {
                    if (val < minVal) minVal = val;
                    if (val > maxVal) maxVal = val;
                }
            });
        }
    });

    if (maxVal === minVal) {
        maxVal += 1;
        minVal = Math.max(0, minVal - 1);
    }
    const range = maxVal - minVal;
    maxVal += range * 0.1;
    minVal -= range * 0.1;
    if (minVal < 0) minVal = 0;

    const paddingLeft = 40;
    const paddingRight = 15;
    const paddingTop = 15;
    const paddingBottom = 25;

    const chartWidth = width - paddingLeft - paddingRight;
    const chartHeight = svgHeight - paddingTop - paddingBottom;

    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('width', '100%');
    svg.setAttribute('height', '100%');
    svg.setAttribute('viewBox', `0 0 ${width} ${svgHeight}`);
    svg.style.overflow = 'visible';

    // Draw horizontal grid lines
    const gridLines = 3;
    for (let i = 0; i <= gridLines; i++) {
        const val = minVal + (maxVal - minVal) * (i / gridLines);
        const y = paddingTop + chartHeight * (1 - i / gridLines);
        
        const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
        line.setAttribute('x1', paddingLeft);
        line.setAttribute('y1', y);
        line.setAttribute('x2', width - paddingRight);
        line.setAttribute('y2', y);
        line.setAttribute('stroke', '#e2e8f0');
        line.setAttribute('stroke-dasharray', '3,3');
        svg.appendChild(line);

        const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
        text.setAttribute('x', paddingLeft - 8);
        text.setAttribute('y', y + 4);
        text.setAttribute('text-anchor', 'end');
        text.setAttribute('font-size', '10');
        text.setAttribute('fill', '#94a3b8');
        text.setAttribute('font-family', 'var(--font-primary)');
        text.textContent = Math.round(val);
        svg.appendChild(text);
    }

    // Step for X axis
    const step = dataPoints.length > 1 ? chartWidth / (dataPoints.length - 1) : chartWidth;

    // Draw each active key
    activeKeys.forEach(k => {
        const color = getColor(k);
        const coords = [];
        const selected = isSelected(k);
        const opacity = selected ? '1' : '0.15';

        dataPoints.forEach((p, idx) => {
            const map = p[dataKey];
            const val = (map && map[k] !== undefined) ? map[k] : 0;
            const x = paddingLeft + idx * step;
            const y = paddingTop + chartHeight * (1 - (val - minVal) / (maxVal - minVal));
            coords.push({ x, y });
        });

        if (coords.length > 0) {
            // Line path
            let dLine = `M ${coords[0].x} ${coords[0].y}`;
            for (let i = 1; i < coords.length; i++) {
                dLine += ` L ${coords[i].x} ${coords[i].y}`;
            }

            const linePath = document.createElementNS('http://www.w3.org/2000/svg', 'path');
            linePath.setAttribute('d', dLine);
            linePath.setAttribute('fill', 'none');
            linePath.setAttribute('stroke', color);
            linePath.setAttribute('stroke-width', '2');
            linePath.setAttribute('stroke-linecap', 'round');
            linePath.setAttribute('stroke-linejoin', 'round');
            linePath.setAttribute('opacity', opacity);
            svg.appendChild(linePath);

            // Last point marker
            const last = coords[coords.length - 1];
            const pulseInner = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
            pulseInner.setAttribute('cx', last.x);
            pulseInner.setAttribute('cy', last.y);
            pulseInner.setAttribute('r', '3');
            pulseInner.setAttribute('fill', 'white');
            pulseInner.setAttribute('stroke', color);
            pulseInner.setAttribute('stroke-width', '2');
            pulseInner.setAttribute('opacity', opacity);
            svg.appendChild(pulseInner);
        }
    });

    // Draw X-axis time labels (first, middle, last)
    if (dataPoints.length > 0) {
        const coords = [];
        dataPoints.forEach((_, idx) => {
            coords.push({ x: paddingLeft + idx * step });
        });

        const labelsToDraw = [];
        labelsToDraw.push({ idx: 0, align: 'start' });
        if (dataPoints.length > 2) {
            labelsToDraw.push({ idx: Math.floor(dataPoints.length / 2), align: 'middle' });
        }
        if (dataPoints.length > 1) {
            labelsToDraw.push({ idx: dataPoints.length - 1, align: 'end' });
        }

        labelsToDraw.forEach(item => {
            const p = dataPoints[item.idx];
            const coord = coords[item.idx];
            const date = new Date(p.time);
            const hours = date.getHours().toString().padStart(2, '0');
            const minutes = date.getMinutes().toString().padStart(2, '0');
            const timeStr = `${hours}:${minutes}`;

            const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
            text.setAttribute('x', coord.x);
            text.setAttribute('y', paddingTop + chartHeight + 16);
            text.setAttribute('text-anchor', item.align);
            text.setAttribute('font-size', '10');
            text.setAttribute('fill', '#94a3b8');
            text.setAttribute('font-family', 'var(--font-primary)');
            text.textContent = timeStr;
            svg.appendChild(text);
        });
    }

    container.appendChild(svg);

    // Build Legend
    const legendContainer = document.createElement('div');
    legendContainer.className = 'chart-legend';
    legendContainer.style.display = 'flex';
    legendContainer.style.flexWrap = 'wrap';
    legendContainer.style.justifyContent = 'center';
    legendContainer.style.gap = '12px';
    legendContainer.style.paddingTop = '12px';
    legendContainer.style.fontSize = '12px';
    legendContainer.style.fontWeight = '500';
    legendContainer.style.color = 'var(--text-muted)';
    legendContainer.style.marginBottom = '8px';

    activeKeys.forEach(k => {
        const color = getColor(k);
        const selected = isSelected(k);
        const item = document.createElement('div');
        item.style.display = 'flex';
        item.style.alignItems = 'center';
        item.style.gap = '6px';
        item.style.cursor = 'pointer';
        item.style.userSelect = 'none';
        item.style.opacity = selected ? '1' : '0.4';
        item.style.transition = 'opacity 0.2s';

        const dot = document.createElement('span');
        dot.style.display = 'inline-block';
        dot.style.width = '10px';
        dot.style.height = '10px';
        dot.style.borderRadius = '50%';
        dot.style.backgroundColor = color;

        const label = document.createElement('span');
        label.textContent = translateKey(k);

        item.appendChild(dot);
        item.appendChild(label);

        item.addEventListener('click', () => {
            container._selectedKeys[k] = !selected;
            drawMultiLineChart(containerId, container._dataPoints, container._dataKey);
        });

        legendContainer.appendChild(item);
    });

    if (card) {
        card.appendChild(legendContainer);
    }
}
