
--user
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)     NOT NULL,  -- 로그인 아이디 (고유)
    name        VARCHAR(50)     NOT NULL,  -- 표시 이름(닉네임)
    password    VARCHAR(255)    NOT NULL,  -- BCrypt 암호화
    role        VARCHAR(20)     NOT NULL,  -- CREATOR | CLASSMATE
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_users         PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username)
    );

-- LECTURES (강의)
CREATE TABLE IF NOT EXISTS lectures (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    creator_id      BIGINT          NOT NULL,
    title           VARCHAR(200)    NOT NULL,
    description     TEXT,
    price           INT             NOT NULL DEFAULT 0,
    capacity        INT             NOT NULL, -- 최대 수강 인원
    current_count   INT             NOT NULL DEFAULT 0, -- 현재 신청 인원 (캐시)
    start_date      DATE            NOT NULL,
    end_date        DATE            NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT', -- DRAFT|OPEN|CLOSED
    like_count      INT             NOT NULL DEFAULT 0, -- 찜 수 (인기순 정렬용)
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_lectures          PRIMARY KEY (id),
    CONSTRAINT fk_lectures_creator  FOREIGN KEY (creator_id) REFERENCES users (id),
    CONSTRAINT chk_capacity         CHECK (capacity > 0),
    CONSTRAINT chk_current_count    CHECK (current_count >= 0),
    CONSTRAINT chk_price            CHECK (price >= 0),
    CONSTRAINT chk_dates            CHECK (end_date >= start_date)
    );

-- ENROLLMENTS(등록)
CREATE TABLE IF NOT EXISTS enrollments (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    lecture_id      BIGINT      NOT NULL,
    user_id         BIGINT      NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    --PENDING    : 신청 완료, 결제 대기 (24시간 이내 결제 필요..)
    --CONFIRMED  : 결제 완료, 수강 확정
    --CANCELLED  : 취소됨 (본인 취소 or 24h 미결제 배치 처리)
    --WAITLISTED : 정원 초과로 대기 중
    waitlist_order  INT, -- WAITLISTED 일 때만 값 존재
    enrolled_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at         TIMESTAMP, -- CONFIRMED 전환 시각 (취소 기간 계산 기준)
    cancelled_at    TIMESTAMP, -- CANCELLED 전환 시각

    CONSTRAINT pk_enrollments           PRIMARY KEY (id),
    CONSTRAINT fk_enrollments_lecture   FOREIGN KEY (lecture_id) REFERENCES lectures (id),
    CONSTRAINT fk_enrollments_user      FOREIGN KEY (user_id)    REFERENCES users (id),
    -- 같은 강의에 중복 신청 방지
    CONSTRAINT uk_enrollment_lecture_user UNIQUE (lecture_id, user_id)
    );

--LIKES(찜)
CREATE TABLE IF NOT EXISTS likes (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    lecture_id  BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_likes             PRIMARY KEY (id),
    CONSTRAINT fk_likes_lecture     FOREIGN KEY (lecture_id) REFERENCES lectures (id),
    CONSTRAINT fk_likes_user        FOREIGN KEY (user_id)    REFERENCES users (id),
    -- 같은 강의에 중복 찜 방지
    CONSTRAINT uk_likes_lecture_user UNIQUE (lecture_id, user_id)
    );

-- 강의 목록: 상태 필터 + 정렬
CREATE INDEX IF NOT EXISTS idx_lectures_status ON lectures (status);
CREATE INDEX IF NOT EXISTS idx_lectures_like_count ON lectures (like_count DESC);
CREATE INDEX IF NOT EXISTS idx_lectures_created_at ON lectures (created_at DESC);

-- 강사 본인 강의 목록
CREATE INDEX IF NOT EXISTS idx_lectures_creator_id ON lectures (creator_id);

-- 수강생 신청 내역 조회 (마이페이지)
CREATE INDEX IF NOT EXISTS idx_enrollments_user_status ON enrollments (user_id, status);

-- 강의별 수강생 목록 조회 (크리에이터 전용)
CREATE INDEX IF NOT EXISTS idx_enrollments_lecture_id ON enrollments (lecture_id);

-- 대기열 순번 처리
CREATE INDEX IF NOT EXISTS idx_enrollments_waitlist ON enrollments (lecture_id, waitlist_order);