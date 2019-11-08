# Essentials
서버에 더 많은 명령어를 추가 해 줍니다.

플러그인에 대한 기능 추가 제안을 받고 있습니다!<br>
여러분의 아이디어를 Github issues 또는 Mindustry 공식 Discord 에 올려주세요!

## 이 플러그인을 실행하는데 필요한 사양
이 플러그인은 기능 사용에 따라 많은 디스크 읽기/쓰기 작업을 합니다.

### 최소 사양
CPU: Ryzen 3 2200G 또는 Intel i3 8100<br>
RAM: 256MB<br>
저장장치: 적어도 랜덤 읽기/쓰기 속도가 10MB 이상인 디스크

### 권장 사양
CPU: Ryzen 5 2600x 또는 Intel i5 8500<br>
RAM: 500MB<br>
저장장치: SSD 또는 읽기/쓰기 속도가 30MB 이상인 디스크

## 설치 방법
jar 파일 확장자로 된 플러그인을 ``<서버 폴더위치>/config/mods`` 에 넣으면 됩니다.

## 6.0 버전 계획
- [x] 플레이어 DB를 사용하여 RPG 만들기
  - [x] 블록 잠금 기능
- [ ] 자원 소비 알림
  - [x] 자원 소모가 너무 빠를경우 알림
  - [ ] 자원 창고가 꽉 찼을경우 알림
- [ ] AI
  - [ ] 길찾기
  - [ ] 자원
  - [ ] 플레이어
- [x] 테러감지 향상
  - [x] 토륨 원자로
    - [ ] ~~냉각수 파괴 감지~~
    - [ ] ~~토륨이 더이상 들어가지 않는것을 감지~~
    - [ ] ~~원자로가 코어 가까이 있는것을 감지~~
    - [x] 원자로가 과부하 되어 폭발 직전일때 즉시 파괴
  - [ ] 비 파괴/건설 테러 감지 (게임 소스에다 Pull 요청함.)
    - [ ] 매스 드라이버 경로 변경
    - [ ] 메타 컨베이어 경로 변경
    - [ ] 터널 경로 변경
    - [ ] 올바르지 않은 아이템을 직접 블록에다 가져다 넣는 행위
      - [ ] 컨베이어
      - [ ] 메타/터널/매스 드라이버
      - [ ] 코어
      - [ ] 컨테이너/창고
    - [ ] 냉각수
    - [x] 모든 직접 운반 이벤트 감지
    - [ ] 모든 아이템 수집 이벤트 감지
- [ ] 로비 기능 만들기
  - [ ] 모든 서버 플레이어 인원 보이기
    - [ ] 메세지 블럭
    - [ ] 서버 정보
    - [x] 블록으로 표시
  - [ ] 모든 클라이언트 인원 보이기
    - [ ] 메세지 블럭
    - [x] 블록으로 표시
- [x] 메세지 블럭으로 감시
  - [ ] 코어
  - [x] 전력
- [ ] PvP 모드 규칙
  - [ ] 인화성 아이템 자폭감지
- [ ] 채팅 기능 강화 <img src="https://preloaders.evidweb.com/d_file.php?file=images/preloaders/squares.gif">
  - [ ] 욕설 차단
  - [ ] 닉네임 머릿/꼬릿말 기능
  - [ ] 번역 이전 대화 삭제
  - [ ] 명령어를 사용하지 않고 PvP 팀챗
- [ ] 플레이어 인원에 따라 자동으로 난이도 조절
- [x] 플레이어 개인섭 기능 (Kyan 가 제안함)
  - [x] /event host [방 제목] [맵 이름] [게임 모드] 추가
  - [x] /event stop (자동으로 함) 추가
  - [x] /event join [방 제목] 추가
- [ ] 랭킹 시스템 강화
  - [ ] 개인 기록 표시
- [ ] 로그인 시스템 강화
  - [ ] 비밀번호 초기화 기능

## 클라이언트 명령어

| 명령어 | 옵션 | 설명 |
|:---|:---|:--- |
| ch | &lt;message&gt; | Send chat to another server () <br> You must modify the settings in ``config/plugins/Essentials/config.txt`` |
| color |  | Enable animated rainbow nickname. <br> Must enable 'realname' and can use admin. |
| difficulty | &lt;difficulty&gt; | Set server difficulty |
| event | &lt;host/join&gt; &lt;roomname&gt; [map] [gamemode] | Host your own server |
| getpos |  | Show your current position position |
| info |  | Show player information |
| jump | &lt;serverip&gt; &lt;port&gt; &lt;range&gt; &lt;block-type&gt; | Create a server-to-server jumping zone. |
| jumpcount | &lt;serverip&gt; &lt;port&gt; | Add server player counting |
| jumptotal |  | Counting all server players |
| kickall |  | Kick all players without you. |
| kill | &lt;name&gt; | Kill other players |
| login | &lt;account id&gt; &lt;password&gt; | Login to account. |
| me | &lt;msg&gt; | Show special chat format |
| motd |  | Show server motd <br> Can modify from ``config/plugins/Essentials/motd.txt`` |
| register | &lt;account id&gt; &lt;new password&gt; &lt;new password repeat&gt; | Register accoun<br>Example - ``/register test test123 test123`` |
| save |  | Save current map |
| spawn | &lt;mob name&gt; &lt;count&gt; &lt;team name&gt; [name] | Spawn mob in player location |
| status |  | Show currently server status (TPS, RAM, Players/ban count) |
| suicide |  | Kill yourself |
| tempban | &lt;name&gt; &lt;time&gt; | Temporarily ban player. time unit: 1 hours |
| time |  | Show server local time |
| tpp | &lt;name&gt; &lt;another player name&gt; | Teleport player to other players |
| tp | &lt;name&gt; | Teleport to players |
| tr |  | Enable/disable auto translate <br> Currently only support Korean to English. |
| vote | &lt;gameover/skipwave/kick/rollback&gt; [name] | Enable animated rainbow nickname. <br> Must enable 'realname' and can use admin. |

## Console commands

| Command | Parameter | Description |
|:---|:---|:---|
| admin | &lt;name&gt; | Set admin status to player |
| allinfo | &lt;name&gt; | Show player information |
| ban | &lt;uuid/name/ip&gt; &lt;username/ip/uuid&gt; | Ban a person. |
| bansync |  | Ban list synchronization from main server |
| blacklist | &lt;add/remove&gt; &lt;name&gt; | Block special nickname. |
| reset | &lt;zone/count/total&gt; | Clear a server-to-server jumping zone data. |
| kickall |  | Kick all players |
| kill | &lt;name&gt; | Kill target player |
| nick | &lt;name&gt; &lt;new_name&gt; | Show player information |
| pvp | &lt;anticoal/timer&gt; [time...] | Set gamerule with PvP mode |
| sync | &lt;name&gt; | Force sync request from the target player |
| team | &lt;name&gt; | Change target player team |
| tempban | &lt;uuid/name/ip&gt; &lt;username/ip/uuid&gt; | Temporarily ban player. time unit: 1 hours |
