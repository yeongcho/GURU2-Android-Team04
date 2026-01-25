package com.example.guru2_android_team04_android.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// AppDbHelper : SQLite 데이터베이스 생성/업그레이드(마이그레이션)를 담당하는 클래스
// - 앱 최초 실행 시 onCreate()에서 테이블을 만든다.
// - DB_VERSION이 올라가면 onUpgrade()에서 기존 사용자 DB를 스키마 변경한다.
// - users: 회원 정보
// - diary_entries: 일기(회원/비회원 포함)
// - ai_analysis: 일기별 AI 분석 결과(캐시)
// - monthly_summaries: 월간 요약
// - badges / user_badges: 배지 시스템
// - settings: 프로필 사진 URI 등 설정
class AppDbHelper(context: Context) :
    SQLiteOpenHelper(context, AppDb.DB_NAME, null, AppDb.DB_VERSION) {

    // DB 연결 설정 단계에서 Foreign Key 제약을 활성화한다.
    // 예: ai_analysis가 diary_entries(entry_id)를 참조하는 FK를 실제로 동작하게 함.
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    // 앱 최초 설치/실행 시 호출된다.
    // 여기서 모든 테이블과 인덱스를 생성하고, 초기 데이터(배지 마스터)를 시드한
    override fun onCreate(db: SQLiteDatabase) {

        // users 테이블
        // - 이메일은 UNIQUE로 중복 가입 방지
        // - 비밀번호는 평문 저장 금지 → password_hash로 저장(PBKDF2)
        db.execSQL(
            """
            CREATE TABLE ${AppDb.T.USERS} (
                user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                nickname TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                created_at INTEGER NOT NULL
            );
            """.trimIndent()
        )

        // diary_entries 테이블
        // - owner_id는 "USER_xxx" 또는 "ANON_xxx" 형태로 사용자 구분에 사용
        // - tags_json은 태그 리스트를 JSON 문자열로 저장
        // - is_favorite: 하트(마음 카드 보관함) 여부
        // - is_temporary: 비회원 저장 데이터(목록/캘린더에서 숨김 처리)
        // UNIQUE(owner_id, date_ymd):
        // - 이 앱은 "하루에 일기 1개" 컨셉을 가정하고 같은 날짜에 같은 사용자가 중복 저장되지 않도록 제약을 건다.
        db.execSQL(
            """
            CREATE TABLE ${AppDb.T.ENTRIES} (
                entry_id INTEGER PRIMARY KEY AUTOINCREMENT,
                owner_id TEXT NOT NULL,
                date_ymd TEXT NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                mood INTEGER NOT NULL,
                tags_json TEXT NOT NULL,
                is_favorite INTEGER NOT NULL DEFAULT 0,
                is_temporary INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                UNIQUE(owner_id, date_ymd)
            );
            """.trimIndent()
        )

        // ai_analysis 테이블
        // - entry_id(일기 1개)당 분석 1개만 유지하도록 UNIQUE
        // - hashtags_json / actions_json은 List<String>을 JSON 문자열로 저장
        // - FOREIGN KEY + ON DELETE CASCADE:
        //   일기를 삭제하면 해당 분석도 자동으로 삭제된다.
        db.execSQL(
            """
            CREATE TABLE ${AppDb.T.ANALYSIS} (
                analysis_id INTEGER PRIMARY KEY AUTOINCREMENT,
                entry_id INTEGER NOT NULL UNIQUE,
                summary TEXT NOT NULL,
                trigger_pattern TEXT NOT NULL,
                actions_json TEXT NOT NULL,
                hashtags_json TEXT NOT NULL DEFAULT '[]',
                mission_summary TEXT NOT NULL DEFAULT '',
                full_text TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(entry_id) REFERENCES ${AppDb.T.ENTRIES}(entry_id) ON DELETE CASCADE
            );
            """.trimIndent()
        )

        // monthly_summaries 테이블
        // - owner_id + year_month(YYYY-MM) 기준으로 월별 1개 요약만 존재하도록 PK 설정
        // - dominant_mood: 월간 최빈 감정
        // - top_tag: 월간 최다 태그(없으면 빈 문자열)
        // - one_line_summary: 한 줄 요약(강조 박스)
        // - detail_summary: 상세 요약 본문(긴 문단)
        // - emotion_flow: 감정 흐름 한 줄 (예: "안정 → 지침 → 회복")
        // - keywords_json: 주요 키워드(칩) 0~3개를 JSON 문자열로 저장
        db.execSQL(
            """
            CREATE TABLE ${AppDb.T.MONTHLY} (
                owner_id TEXT NOT NULL,
                year_month TEXT NOT NULL,
                dominant_mood INTEGER NOT NULL,
                one_line_summary TEXT NOT NULL DEFAULT '',
                detail_summary TEXT NOT NULL DEFAULT '',
                emotion_flow TEXT NOT NULL DEFAULT '',
                keywords_json TEXT NOT NULL DEFAULT '[]',
                updated_at INTEGER NOT NULL,
                PRIMARY KEY(owner_id, year_month)
            );
            """.trimIndent()
        )

        // badges 테이블 (배지 마스터)
        // - rule_type / rule_value로 배지 획득 조건을 정의한다.
        // - badge_id는 고정된 값(마스터 데이터)이므로 AUTOINCREMENT를 쓰지 않는다.
        db.execSQL(
            """
            CREATE TABLE ${AppDb.T.BADGES} (
                badge_id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                rule_type TEXT NOT NULL,
                rule_value INTEGER NOT NULL
            );
            """.trimIndent()
        )

        // user_badges 테이블 (사용자별 배지 획득 기록)
        // - PRIMARY KEY(owner_id, badge_id): 같은 배지를 중복 획득하지 않도록 함
        // - is_selected: 프로필에서 대표 배지로 선택한 상태
        // - badge_id는 badges 테이블을 참조(FK)
        db.execSQL(
            """
            CREATE TABLE ${AppDb.T.USER_BADGES} (
                owner_id TEXT NOT NULL,
                badge_id INTEGER NOT NULL,
                earned_at INTEGER NOT NULL,
                is_selected INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(owner_id, badge_id),
                FOREIGN KEY(badge_id) REFERENCES ${AppDb.T.BADGES}(badge_id)
            );
            """.trimIndent()
        )

        // settings 테이블
        // - 사용자(owner_id)별 설정값을 저장한다.
        // - 예: 프로필 이미지 URI
        db.execSQL(
            """
            CREATE TABLE ${AppDb.T.SETTINGS} (
                owner_id TEXT NOT NULL,
                setting_key TEXT NOT NULL,
                value TEXT NOT NULL,
                PRIMARY KEY(owner_id, setting_key)
            );
            """.trimIndent()
        )

        // 인덱스 생성
        // - 조회 성능 개선 목적 (목록/캘린더/즐겨찾기/비회원 숨김 조회가 빠르게 동작)
        db.execSQL("CREATE INDEX idx_entries_owner_date ON ${AppDb.T.ENTRIES}(owner_id, date_ymd);")

        // 예외처리) 이름은 month지만 실제로는 owner_id + date_ymd 기반 범위조회에 도움
        // (YYYY-MM-DD 문자열을 BETWEEN으로 조회하므로 date_ymd 인덱스가 효과적)
        db.execSQL("CREATE INDEX idx_entries_owner_month ON ${AppDb.T.ENTRIES}(owner_id, date_ymd);")

        // 즐겨찾기(하트) 목록 조회 최적화
        db.execSQL("CREATE INDEX idx_entries_owner_fav ON ${AppDb.T.ENTRIES}(owner_id, is_favorite, date_ymd);")

        // 비회원 임시 저장 데이터 숨김/조회 최적화
        db.execSQL("CREATE INDEX idx_entries_owner_temp ON ${AppDb.T.ENTRIES}(owner_id, is_temporary, date_ymd);")

        // 배지 마스터 데이터 초기 삽입
        // - 앱 설치 직후 배지 목록이 비어 있지 않도록 한다.
        seedBadges(db)
    }

    // DB_VERSION이 변경되면 호출된다.
    // - 기존 사용자 DB를 삭제하지 않고, 필요한 컬럼/인덱스를 추가한다.
    // - oldVersion 조건을 단계적으로 체크하여 어떤 버전에서 오더라도 업그레이드 가능하게 한다.
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

        // v1 -> v2
        // - 즐겨찾기(하트) 기능 추가를 위해 is_favorite 컬럼 및 인덱스 추가
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE ${AppDb.T.ENTRIES} ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0;")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_entries_owner_fav ON ${AppDb.T.ENTRIES}(owner_id, is_favorite, date_ymd);")
        }

        // v2 -> v3
        // - 비회원 임시 저장 기능을 위해 is_temporary 컬럼 및 인덱스 추가
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE ${AppDb.T.ENTRIES} ADD COLUMN is_temporary INTEGER NOT NULL DEFAULT 0;")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_entries_owner_temp ON ${AppDb.T.ENTRIES}(owner_id, is_temporary, date_ymd);")
        }

        // v3 -> v4
        // - AI 분석 결과 확장: 해시태그 / 미션 요약 컬럼 추가
        // - 월간 요약에 top_tag 컬럼 추가
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE ${AppDb.T.ANALYSIS} ADD COLUMN hashtags_json TEXT NOT NULL DEFAULT '[]';")
            db.execSQL("ALTER TABLE ${AppDb.T.ANALYSIS} ADD COLUMN mission_summary TEXT NOT NULL DEFAULT '';")

            // 예외처리) monthly_summaries에 top_tag 컬럼이 이미 존재할 수도 있으므로 try/catch로 방어한다.
            try {
                db.execSQL("ALTER TABLE ${AppDb.T.MONTHLY} ADD COLUMN top_tag TEXT NOT NULL DEFAULT '';")
            } catch (_: Exception) {
                // 예외처리) 이미 컬럼이 있으면 ALTER TABLE이 실패한다.
                // 이 경우는 정상적인 상황이므로 무시한다.
            }
        }

        // settings: key(예약어) -> setting_key 로 컬럼명 변경
        if (oldVersion < 5) {
            // 예외처리) SQLite 버전에 따라 RENAME COLUMN 지원 여부가 달라서 2단계로 방어
            try {
                db.execSQL("ALTER TABLE ${AppDb.T.SETTINGS} RENAME COLUMN key TO setting_key;")
            } catch (_: Exception) {
                // 예외처리) RENAME COLUMN이 불가능한 환경이면 테이블 재생성 + 데이터 이관 방식으로 처리
                db.execSQL(
                    """
                    CREATE TABLE settings_new (
                        owner_id TEXT NOT NULL,
                        setting_key TEXT NOT NULL,
                        value TEXT NOT NULL,
                        PRIMARY KEY(owner_id, setting_key)
                    );
                    """.trimIndent()
                )

                // 기존 key 컬럼 값을 setting_key로 복사
                db.execSQL(
                    """
                    INSERT INTO settings_new(owner_id, setting_key, value)
                    SELECT owner_id, key, value
                    FROM ${AppDb.T.SETTINGS};
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE ${AppDb.T.SETTINGS};")
                db.execSQL("ALTER TABLE settings_new RENAME TO ${AppDb.T.SETTINGS};")
            }
        }

        // v5 -> v6
        // 월간 요약 화면(UI)에 맞게 monthly_summaries 스키마 변경
        // - 기존: dominant_mood, top_tag, summary_text
        // - 신규: dominant_mood, one_line_summary, detail_summary, emotion_flow, keywords_json
        if (oldVersion < 6) {
            db.execSQL(
                """
                CREATE TABLE monthly_summaries_new (
                    owner_id TEXT NOT NULL,
                    year_month TEXT NOT NULL,
                    dominant_mood INTEGER NOT NULL,
                    one_line_summary TEXT NOT NULL DEFAULT '',
                    detail_summary TEXT NOT NULL DEFAULT '',
                    emotion_flow TEXT NOT NULL DEFAULT '',
                    keywords_json TEXT NOT NULL DEFAULT '[]',
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY(owner_id, year_month)
                );
                """.trimIndent()
            )

            // 기존 데이터 이관:
            // - summary_text → detail_summary로 이전
            // - one_line_summary / emotion_flow / keywords_json은 기본값으로 초기화
            db.execSQL(
                """
                INSERT INTO monthly_summaries_new(
                    owner_id, year_month, dominant_mood, one_line_summary, detail_summary, emotion_flow, keywords_json, updated_at
                )
                SELECT
                    owner_id, year_month, dominant_mood,
                    '' AS one_line_summary,
                    summary_text AS detail_summary,
                    '' AS emotion_flow,
                    '[]' AS keywords_json,
                    updated_at
                FROM ${AppDb.T.MONTHLY};
                """.trimIndent()
            )

            db.execSQL("DROP TABLE ${AppDb.T.MONTHLY};")
            db.execSQL("ALTER TABLE monthly_summaries_new RENAME TO ${AppDb.T.MONTHLY};")
        }
    }

    // badges 테이블에 배지 "마스터 데이터"를 초기 삽입한다.
    // - INSERT OR IGNORE를 사용하여 이미 존재하는 id는 중복 삽입하지 않는다.
    private fun seedBadges(db: SQLiteDatabase) {

        // 바인딩(?, ?, ?, ?, ?)을 사용하는 PreparedStatement 형태로 만들어
        // SQL 인젝션 위험을 줄이고 실행 비용을 낮춘다.
        val insert = """
            INSERT OR IGNORE INTO ${AppDb.T.BADGES}(badge_id, name, description, rule_type, rule_value)
            VALUES(?, ?, ?, ?, ?);
        """.trimIndent()

        db.compileStatement(insert).apply {

            // 배지 1개를 삽입하는 헬퍼 함수
            // - clearBindings(): 이전 바인딩 값 제거
            // - bindXxx(): 파라미터 바인딩
            // - executeInsert(): insert 실행
            fun add(id: Int, name: String, desc: String, ruleType: String, ruleVal: Int) {
                clearBindings()
                bindLong(1, id.toLong())
                bindString(2, name)
                bindString(3, desc)
                bindString(4, ruleType)
                bindLong(5, ruleVal.toLong())
                executeInsert()
            }

            // 배지 정의
            // - rule_type과 rule_value는 BadgeEngine에서 해석하여 지급 여부를 판단한다.
            add(1, "시작과 꾸준함", "첫 번째 일기 작성", "ENTRY_COUNT_AT_LEAST", 1)
            add(2, "한 달의 조각들", "누적 일기 30개 작성", "ENTRY_COUNT_AT_LEAST", 30)
            add(3, "감정 로그 수집가", "누적 일기 100개 작성", "ENTRY_COUNT_AT_LEAST", 100)
            add(4, "작심삼일 마스터", "3일 연속 일기 작성", "STREAK_AT_LEAST", 3)
            add(5, "감정 소믈리에", "모든 종류의 감정 태그 사용", "DISTINCT_MOOD_AT_LEAST", 7)
        }
    }
}
