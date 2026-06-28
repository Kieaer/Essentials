// Translations for the EssentialWeb module
// Supports English, Korean, and Ukrainian languages

// Default language is English
let currentLanguage = 'en';

// Language translations
const translations = {
    'en': {
        // Navigation
        'nav.title': 'Mindustry Server',
        'nav.home': 'Home',
        'nav.maps': 'Maps',
        'nav.server': 'Server Status',
        'nav.profile': 'My Info',
        'nav.login': 'Login',
        'nav.logout': 'Logout',

        'home.title': 'Mindustry Server Web Interface',
        'home.subtitle': 'Preview and Management Hub',
        'home.latest_map': 'Latest Map',
        'home.current_map': 'Current Map',
        'home.current_map.desc': 'Currently running server map.',
        'home.dashboard': 'Real-time Dashboard',

        // Maps page
        'maps.title': 'Map Management',
        'maps.description': 'Upload, download, and manage maps for your server.',
        'maps.upload': 'Upload Map',
        'maps.none': 'No maps available',
        'maps.error': 'Error loading maps: {0}',
        'maps.author': 'Author',
        'maps.planet': 'Planet',
        'maps.unknown': 'Unknown',
        'maps.no.description': 'No description available',
        'maps.download': 'Download',
        'maps.search': 'Search maps...',
        'maps.all_planets': 'All Planets',
        'maps.sort_latest': 'Latest',
        'maps.sort_downloads': 'Downloads',
        'maps.sort_popular': 'Popularity',
        'maps.votes': 'Rating: {0} votes',

        // Server status page
        'server.title': 'Server Status',
        'server.description': 'Monitor server status, players, and chat.',
        'server.info': 'Server Information',
        'server.map': 'Current Map',
        'server.tps': 'TPS',
        'server.wave': 'Wave',
        'server.time': 'Game Time',
        'server.players': 'Players',
        'server.no.players': 'No players online',
        'server.chat': 'Chat',
        'server.chat.none': 'No chat messages',
        'server.chat.loading': 'Loading chat history...',
        'server.chat.input': 'Type a message...',
        'server.chat.send': 'Send',
        'server.chat.you': 'You',
        'server.loading': 'Loading...',
        'server.active_teams': 'Active Teams',
        'server.chart.tps': 'Server TPS',
        'server.chart.players': 'Players Online',
        'server.chart.units': 'Units on Map',
        'server.chart.buildings': 'Buildings on Map',
        'server.chart.no_data': 'No data recorded yet',
        'server.contribution.title': 'Contribution',
        'server.contribution.subtitle': 'Live contribution (online players)',
        'server.contribution.player': 'Player',
        'server.contribution.current': 'Current Game',
        'server.contribution.average': 'Average',
        'server.contribution.games': 'games',
        'server.contribution.empty': 'No players online',

        // My Info / Profile page
        'profile.title': 'My Info',
        'profile.subtitle': 'Your account data and achievement progress',
        'profile.account': 'Account Information',
        'profile.stats': 'Statistics',
        'profile.uuid': 'UUID',
        'profile.joined': 'Joined',
        'profile.lastlogin': 'Last Login',
        'profile.permission': 'Permission',
        'profile.level': 'Level',
        'profile.exp': 'EXP',
        'profile.playtime': 'Total Play Time',
        'profile.attendance': 'Attendance Days',
        'profile.blocks_placed': 'Blocks Placed',
        'profile.blocks_broken': 'Blocks Broken',
        'profile.pvp_win': 'PvP Wins',
        'profile.pvp_lose': 'PvP Losses',
        'profile.pvp_winrate': 'PvP Win Rate',
        'profile.wave_clear': 'Wave Clears',
        'profile.attack_clear': 'Attack Clears',
        'profile.achievements': 'Achievements',
        'profile.achievements_progress': '{0} / {1} completed',
        'profile.target': 'Target',
        'profile.copied': 'Copied to clipboard',
        'profile.error': 'Failed to load profile',

        // Login page
        'login.title': 'Login',
        'login.username': 'Username',
        'login.password': 'Password',
        'login.button': 'Login',
        'login.required': 'Login Required',
        'login.to.view': 'Please login to view and manage maps',
        'login.error.credentials': 'Invalid username or password',
        'login.error.fields': 'Please enter both username and password',
        'login.error.forbidden': 'Access forbidden',

        // Upload dialog
        'upload.title': 'Upload Map',
        'upload.button': 'Upload',
        'upload.cancel': 'Cancel',
        'upload.browse': 'Browse File',
        'upload.select': 'Please select a file',
        'upload.only.msav': 'Only .msav files are allowed',
        'upload.size.limit': 'File size exceeds 10MB limit',
        'upload.success': 'Map uploaded successfully',
        'upload.error': 'Failed to upload map',

        // Errors
        'error.not.logged.in': 'Not logged in',
        'error.auth.required': 'Authentication required',
        'error.load.maps': 'Failed to load maps',
        'error.load.status': 'Failed to load server status',
        'error.load.chat': 'Failed to load chat history',
        'error.send.message': 'Failed to send message',

        // WebSocket
        'ws.connected': 'WebSocket connected',
        'ws.error': 'WebSocket error',
        'ws.closed': 'WebSocket closed'
    },
    'ko': {
        // Navigation
        'nav.title': '민더스트리 서버',
        'nav.home': '홈',
        'nav.maps': '맵',
        'nav.server': '서버 상태',
        'nav.profile': '내 정보',
        'nav.login': '로그인',
        'nav.logout': '로그아웃',

        'home.title': '민더스트리 서버 웹 인터페이스',
        'home.subtitle': '미리보기 및 관리 허브',
        'home.latest_map': '최신 맵',
        'home.current_map': '현재 진행중인 맵',
        'home.current_map.desc': '현재 서버에서 진행 중인 맵입니다.',
        'home.dashboard': '실시간 대시보드',

        // Maps page
        'maps.title': '맵 목록 및 관리',
        'maps.description': '서버용 맵을 업로드, 다운로드 및 관리합니다.',
        'maps.upload': '맵 업로드',
        'maps.none': '사용 가능한 맵이 없습니다',
        'maps.error': '맵 로딩 오류: {0}',
        'maps.author': '제작자',
        'maps.planet': '행성',
        'maps.unknown': '알 수 없음',
        'maps.no.description': '설명 없음',
        'maps.download': '다운로드',
        'maps.search': '맵 이름 검색...',
        'maps.all_planets': '모든 행성',
        'maps.sort_latest': '최신순',
        'maps.sort_downloads': '다운로드순',
        'maps.sort_popular': '인기순',
        'maps.votes': '평가: {0}표',

        // Server status page
        'server.title': '서버 상태',
        'server.description': '서버 상태, 플레이어 및 채팅을 모니터링합니다.',
        'server.info': '서버 정보',
        'server.map': '현재 맵',
        'server.tps': 'TPS',
        'server.wave': '웨이브',
        'server.time': '게임 시간',
        'server.players': '플레이어',
        'server.no.players': '접속 중인 플레이어가 없습니다',
        'server.chat': '채팅',
        'server.chat.none': '채팅 메시지가 없습니다',
        'server.chat.loading': '채팅 기록 로딩 중...',
        'server.chat.input': '메시지 입력...',
        'server.chat.send': '보내기',
        'server.chat.you': '나',
        'server.loading': '로딩 중...',
        'server.active_teams': '남은 팀 수',
        'server.chart.tps': '서버 TPS',
        'server.chart.players': '플레이어 인원',
        'server.chart.units': '맵 유닛 수',
        'server.chart.buildings': '맵 건물 수',
        'server.chart.no_data': '기록된 데이터 없음',
        'server.contribution.title': '기여도',
        'server.contribution.subtitle': '실시간 기여도 (접속 중인 플레이어)',
        'server.contribution.player': '플레이어',
        'server.contribution.current': '현재 게임',
        'server.contribution.average': '평균',
        'server.contribution.games': '판',
        'server.contribution.empty': '접속 중인 플레이어 없음',

        // My Info / Profile page
        'profile.title': '내 정보',
        'profile.subtitle': '내 계정 데이터 및 도전과제 진행도',
        'profile.account': '계정 정보',
        'profile.stats': '통계',
        'profile.uuid': 'UUID',
        'profile.joined': '가입일',
        'profile.lastlogin': '마지막 접속',
        'profile.permission': '권한',
        'profile.level': '레벨',
        'profile.exp': '경험치',
        'profile.playtime': '총 플레이 시간',
        'profile.attendance': '출석 일수',
        'profile.blocks_placed': '블록 설치 수',
        'profile.blocks_broken': '블록 파괴 수',
        'profile.pvp_win': 'PvP 승리',
        'profile.pvp_lose': 'PvP 패배',
        'profile.pvp_winrate': 'PvP 승률',
        'profile.wave_clear': '웨이브 클리어',
        'profile.attack_clear': '공격 클리어',
        'profile.achievements': '도전과제',
        'profile.achievements_progress': '{0} / {1} 완료',
        'profile.target': '목표',
        'profile.copied': '클립보드에 복사됨',
        'profile.error': '내 정보를 불러오는데 실패했습니다',

        // Login page
        'login.title': '로그인',
        'login.username': '사용자 이름',
        'login.password': '비밀번호',
        'login.button': '로그인',
        'login.required': '로그인 필요',
        'login.to.view': '맵을 보고 관리하려면 로그인하세요',
        'login.error.credentials': '잘못된 사용자 이름 또는 비밀번호',
        'login.error.fields': '사용자 이름과 비밀번호를 모두 입력해주세요',
        'login.error.forbidden': '접근 금지',

        // Upload dialog
        'upload.title': '맵 업로드',
        'upload.button': '업로드',
        'upload.cancel': '취소',
        'upload.browse': '파일 찾기',
        'upload.select': '파일을 선택해주세요',
        'upload.only.msav': '.msav 파일만 허용됩니다',
        'upload.size.limit': '파일 크기가 10MB 제한을 초과합니다',
        'upload.success': '맵이 성공적으로 업로드되었습니다',
        'upload.error': '맵 업로드 실패',

        // Errors
        'error.not.logged.in': '로그인되지 않음',
        'error.auth.required': '인증 필요',
        'error.load.maps': '맵을 불러오는데 실패했습니다',
        'error.load.status': '서버 상태를 불러오는데 실패했습니다',
        'error.load.chat': '채팅 기록을 불러오는데 실패했습니다',
        'error.send.message': '메시지 전송 실패',

        // WebSocket
        'ws.connected': '웹소켓 연결됨',
        'ws.error': '웹소켓 오류',
        'ws.closed': '웹소켓 연결 종료'
    },
    'uk': {
        // Navigation
        'nav.title': 'Сервер Mindustry',
        'nav.home': 'Головна',
        'nav.maps': 'Карти',
        'nav.server': 'Стан сервера',
        'nav.profile': 'Мої дані',
        'nav.login': 'Увійти',
        'nav.logout': 'Вийти',

        // Home page
        'home.title': 'Веб-інтерфейс сервера Mindustry',
        'home.subtitle': 'Панель попереднього перегляду та керування',
        'home.description': 'Цей веб-інтерфейс дозволяє керувати картами та відстежувати стан сервера.',
        'home.featured': 'Рекомендовані карти',
        'home.latest_map': 'Остання карта',
        'home.current_map': 'Поточна карта',
        'home.current_map.desc': 'Поточна запущена карта на сервері.',
        'home.dashboard': 'Панель в реальному часі',

        // Maps page
        'maps.title': 'Управління картами',
        'maps.description': 'Завантажуйте, скачуйте та керуйте картами для вашого сервера.',
        'maps.upload': 'Завантажити карту',
        'maps.none': 'Немає доступних карт',
        'maps.error': 'Помилка завантаження карт: {0}',
        'maps.author': 'Автор',
        'maps.planet': 'Планета',
        'maps.unknown': 'Невідомо',
        'maps.no.description': 'Опис відсутній',
        'maps.download': 'Скачати',
        'maps.search': 'Пошук карт...',
        'maps.all_planets': 'Всі планети',
        'maps.sort_latest': 'Найновіші',
        'maps.sort_downloads': 'Популярні',
        'maps.sort_popular': 'Популярність',
        'maps.votes': 'Рейтинг: {0} голосів',

        // Server status page
        'server.title': 'Стан сервера',
        'server.description': 'Відстежуйте стан сервера, гравців та чат.',
        'server.info': 'Інформація про сервер',
        'server.map': 'Поточна карта',
        'server.tps': 'TPS',
        'server.wave': 'Хвиля',
        'server.time': 'Час гри',
        'server.players': 'Гравці',
        'server.no.players': 'Немає гравців онлайн',
        'server.chat': 'Чат',
        'server.chat.none': 'Немає повідомлень у чаті',
        'server.chat.loading': 'Завантаження історії чату...',
        'server.chat.input': 'Введіть повідомлення...',
        'server.chat.send': 'Надіслати',
        'server.chat.you': 'Ви',
        'server.loading': 'Завантаження...',
        'server.active_teams': 'Активні команди',
        'server.chart.tps': 'TPS Сервера',
        'server.chart.players': 'Гравці онлайн',
        'server.chart.units': 'Юнітів на карті',
        'server.chart.buildings': 'Будівель на карті',
        'server.chart.no_data': 'Немає записаних даних',
        'server.contribution.title': 'Внесок',
        'server.contribution.subtitle': 'Внесок у реальному часі (гравці онлайн)',
        'server.contribution.player': 'Гравець',
        'server.contribution.current': 'Поточна гра',
        'server.contribution.average': 'Середній',
        'server.contribution.games': 'ігор',
        'server.contribution.empty': 'Немає гравців онлайн',

        // My Info / Profile page
        'profile.title': 'Мої дані',
        'profile.subtitle': 'Дані вашого облікового запису та прогрес досягнень',
        'profile.account': 'Інформація про обліковий запис',
        'profile.stats': 'Статистика',
        'profile.uuid': 'UUID',
        'profile.joined': 'Дата реєстрації',
        'profile.lastlogin': 'Останній вхід',
        'profile.permission': 'Права',
        'profile.level': 'Рівень',
        'profile.exp': 'Досвід',
        'profile.playtime': 'Загальний час гри',
        'profile.attendance': 'Днів відвідування',
        'profile.blocks_placed': 'Встановлено блоків',
        'profile.blocks_broken': 'Зруйновано блоків',
        'profile.pvp_win': 'Перемоги PvP',
        'profile.pvp_lose': 'Поразки PvP',
        'profile.pvp_winrate': 'Відсоток перемог PvP',
        'profile.wave_clear': 'Пройдено хвиль',
        'profile.attack_clear': 'Пройдено атак',
        'profile.achievements': 'Досягнення',
        'profile.achievements_progress': '{0} / {1} виконано',
        'profile.target': 'Ціль',
        'profile.copied': 'Скопійовано в буфер обміну',
        'profile.error': 'Не вдалося завантажити дані',

        // Login page
        'login.title': 'Вхід',
        'login.username': 'Ім\'я користувача',
        'login.password': 'Пароль',
        'login.button': 'Увійти',
        'login.required': 'Потрібен вхід',
        'login.to.view': 'Будь ласка, увійдіть, щоб переглядати та керувати картами',
        'login.error.credentials': 'Невірне ім\'я користувача або пароль',
        'login.error.fields': 'Будь ласка, введіть ім\'я користувача та пароль',
        'login.error.forbidden': 'Доступ заборонено',

        // Upload dialog
        'upload.title': 'Завантажити карту',
        'upload.button': 'Завантажити',
        'upload.cancel': 'Скасувати',
        'upload.browse': 'Вибрати файл',
        'upload.select': 'Будь ласка, виберіть файл',
        'upload.only.msav': 'Дозволені лише файли .msav',
        'upload.size.limit': 'Розмір файлу перевищує ліміт 10MB',
        'upload.success': 'Карту успішно завантажено',
        'upload.error': 'Не вдалося завантажити карту',

        // Errors
        'error.not.logged.in': 'Не увійшли в систему',
        'error.auth.required': 'Потрібна аутентифікація',
        'error.load.maps': 'Не вдалося завантажити карти',
        'error.load.status': 'Не вдалося завантажити стан сервера',
        'error.load.chat': 'Не вдалося завантажити історію чату',
        'error.send.message': 'Не вдалося надіслати повідомлення',

        // WebSocket
        'ws.connected': 'WebSocket підключено',
        'ws.error': 'Помилка WebSocket',
        'ws.closed': 'WebSocket закрито'
    }
};

// Function to get the browser language
function getBrowserLanguage() {
    const browserLang = navigator.language || navigator.userLanguage;
    // Extract the language code (e.g., 'en-US' -> 'en')
    const langCode = browserLang.split('-')[0];

    // Check if we support this language
    if (translations[langCode]) {
        return langCode;
    }

    // Special case for Ukrainian (uk-UA)
    if (browserLang.toLowerCase() === 'uk-ua' && translations['uk']) {
        return 'uk';
    }

    // Default to English if language is not supported
    return 'en';
}

// Initialize language based on browser settings
function initializeLanguage() {
    currentLanguage = getBrowserLanguage();
    console.log(`Initialized language: ${currentLanguage}`);
    return currentLanguage;
}

// Get translation for a key
function translate(key, ...params) {
    // Get the translation object for the current language
    const langTranslations = translations[currentLanguage] || translations['en'];

    // Get the translation string for the key
    let translation = langTranslations[key];

    // If translation doesn't exist, try English, then fall back to the key itself
    if (!translation) {
        translation = translations['en'][key] || key;
    }

    // Replace parameters if any
    if (params && params.length > 0) {
        params.forEach((param, index) => {
            translation = translation.replace(`{${index}}`, param);
        });
    }

    return translation;
}

// Function to translate the entire UI
function translateUI() {
    // Get all elements with data-i18n attribute
    const elements = document.querySelectorAll('[data-i18n]');

    elements.forEach(element => {
        const key = element.getAttribute('data-i18n');

        // Check if this is an input with placeholder
        if (element.hasAttribute('placeholder')) {
            element.setAttribute('placeholder', translate(key));
        } 
        // Check if this is an element with innerHTML
        else {
            element.innerHTML = translate(key);
        }
    });

    // Update the html lang attribute
    document.documentElement.lang = currentLanguage;
}

// Export functions
window.i18n = {
    init: initializeLanguage,
    translate: translate,
    translateUI: translateUI,
    getCurrentLanguage: () => currentLanguage,
    setLanguage: (lang) => {
        if (translations[lang]) {
            currentLanguage = lang;
            translateUI();
            return true;
        }
        return false;
    }
};
