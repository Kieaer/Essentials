package essentials.event.feature

enum class VoteType {
    Kick{
        override fun toString(): String {
            return "강퇴"
        }
    },
    Map{
        override fun toString(): String {
            return "맵"
        }
    },
    Gameover{
        override fun toString(): String {
            return "항복"
        }
    },
    Skipwave {
        override fun toString(): String {
            return "웨이브 건너뛰기"
        }
    },
    Rollback {
        override fun toString(): String {
            return "빽섭"
        }
    },
    None{
        override fun toString(): String {
            return "없음"
        }
    }
}