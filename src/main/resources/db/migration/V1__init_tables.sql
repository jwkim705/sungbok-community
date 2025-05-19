-- 부서 테이블
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL
);

-- 역할 테이블
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL, -- 예: 교사, 리더, 성도, 부장, 목사
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL
);

-- 게시판 카테고리 테이블
CREATE TABLE posts_categories (
    category_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE, -- 예 : FREE, YOUTUBE, SNS
    description TEXT, -- 카테고리 설명
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL
);

-- 사용자 계정 테이블
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255), -- Form login 용. OAuth 사용자는 null일 수 있음
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL
);

-- OAuth 계정 정보 테이블
CREATE TABLE oauth_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGSERIAL NOT NULL REFERENCES users(id),
    social_type VARCHAR(50) NOT NULL, -- 예: google, kakao
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL
);

-- 교회 성도 정보 테이블
CREATE TABLE members (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGSERIAL REFERENCES users(id),
    name VARCHAR(50) NOT NULL,
    birthdate DATE,
    gender VARCHAR(10),
    address TEXT,
    phone_number VARCHAR(20),
    picture TEXT, -- Oauth 프로필
    nickname VARCHAR(50),
    is_deleted BOOLEAN DEFAULT FALSE,
    role VARCHAR(50) NOT NULL DEFAULT 'GUEST',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL,
    registered_by_user_id BIGSERIAL REFERENCES users(id) -- 등록한 관리자/리더 ID
    -- 기타 필요한 정보 (세례명, 직분 등)
);

-- 출석 정보 테이블
CREATE TABLE attendance (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGSERIAL NOT NULL REFERENCES members(id),
    department_id BIGSERIAL NOT NULL REFERENCES departments(id), -- 출석한 부서
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL, -- 예: PRESENT, ABSENT, LATE, ONLINE (실제 값 정의 필요)
    check_in_time TIMESTAMP,
    checked_by_user_id BIGSERIAL REFERENCES users(id), -- 출석 체크한 사용자 (null이면 본인 체크)
    method VARCHAR(10) NOT NULL, -- SELF, LEADER
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL,
    UNIQUE (member_id, department_id, attendance_date) -- 특정 날짜, 특정 부서에 한 성도는 하나의 출석 기록만 가짐
);

-- 가족 관계 테이블
CREATE TABLE family_relations (
    id BIGSERIAL PRIMARY KEY,
    member1_id BIGSERIAL NOT NULL REFERENCES members(id),
    member2_id BIGSERIAL NOT NULL REFERENCES members(id),
    relationship_type VARCHAR(50) NOT NULL, -- 예: SPOUSE, PARENT, CHILD, SIBLING (실제 값 정의 필요)
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL,
    UNIQUE (member1_id, member2_id, relationship_type)
);

CREATE TABLE groups (
    group_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL, -- 그룹 이름 (예: '유년부', '마을', '1교구')
    group_type VARCHAR(50) NOT NULL, -- 그룹 유형 (예: 'Department', 'Class', 'Parish', 'Village', 'SmallGroup', 'Team')
    -- 어떤 유형들이 있는지 명확히 정의하고 사용하는 것이 중요합니다.
    parent_group_id BIGSERIAL REFERENCES groups(group_id) ON DELETE SET NULL, -- 상위 그룹 ID (최상위 그룹은 NULL)
    -- 상위 그룹 삭제 시 하위 그룹의 연결을 끊음 (NULL로 설정)
    leader_person_id BIGSERIAL REFERENCES members(id) ON DELETE SET NULL, -- 그룹 리더/교사/담당자의 members 테이블 ID
    -- 리더로 지정된 멤버 삭제 시 그룹의 리더 정보를 NULL로 설정
    description TEXT NULL, -- 그룹 설명
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL
);

CREATE TABLE membership (
    membership_id BIGSERIAL PRIMARY KEY,
    person_id BIGSERIAL NOT NULL REFERENCES members(id) ON DELETE CASCADE, -- 소속된 사람의 members 테이블 ID
    -- 멤버 삭제 시 해당 멤버의 모든 멤버십 기록도 함께 삭제
    group_id BIGSERIAL NOT NULL REFERENCES groups(group_id) ON DELETE CASCADE, -- 소속된 그룹의 Groups 테이블 ID
    -- 그룹 삭제 시 해당 그룹에 속한 모든 멤버십 기록도 함께 삭제
    role VARCHAR(100) NOT NULL, -- 그룹 내에서의 역할 (예: 'Student', 'Teacher', 'Member', 'Leader', 'Assistant')
    start_date DATE NOT NULL,
    end_date DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL
);

-- 게시글 테이블
CREATE TABLE posts (
    post_id BIGSERIAL PRIMARY KEY,
    table_type VARCHAR(50) NOT NULL, -- (홈페이지,관리자,커뮤니티)
    category_nm VARCHAR(100) NOT NULL,
    user_id BIGSERIAL NOT NULL REFERENCES users(id), -- 수정됨
    user_nm VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    view_count INT DEFAULT 0 NOT NULL,
    like_count INT DEFAULT 0 NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL
);

-- 게시글-유튜브 영상 연결 테이블
CREATE TABLE post_youtube (
    post_youtube_id BIGSERIAL PRIMARY KEY,
    post_id BIGSERIAL NOT NULL REFERENCES posts(post_id) ON DELETE CASCADE,
    youtube_video_id VARCHAR(50) NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL,
    UNIQUE (post_id, youtube_video_id)
);

CREATE TABLE post_likes (
    user_id BIGSERIAL NOT NULL,                -- 좋아요 누른 사용자 ID (users 테이블 참조)
    post_id BIGSERIAL NOT NULL,                -- 좋아요 눌린 게시글 ID (posts 테이블 참조)
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP, -- 좋아요 누른 시각

    PRIMARY KEY (user_id, post_id),        -- 한 사용자가 같은 게시글에 중복 좋아요 방지 (복합 기본키)

    CONSTRAINT fk_user_like
    FOREIGN KEY(user_id)
    REFERENCES users(id)
    ON DELETE CASCADE,                  -- 사용자가 탈퇴하면 좋아요 기록도 삭제

    CONSTRAINT fk_post_like
    FOREIGN KEY(post_id)
    REFERENCES posts(post_id)
    ON DELETE CASCADE                   -- 게시글이 삭제되면 좋아요 기록도 삭제
);

-- 댓글 테이블
CREATE TABLE comments (
    comment_id BIGSERIAL PRIMARY KEY,
    post_id BIGSERIAL NOT NULL REFERENCES posts(post_id) ON DELETE CASCADE,
    user_id BIGSERIAL NOT NULL REFERENCES users(id), -- 수정됨
    user_nm VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    parent_comment_id BIGSERIAL REFERENCES comments(comment_id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL
);

-- 파일 테이블
CREATE TABLE files (
    file_id BIGSERIAL PRIMARY KEY,
    related_entity_id BIGSERIAL NOT NULL,
    related_entity_type VARCHAR(50) NOT NULL, -- 예: 'post', 'comment'
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL UNIQUE,
    file_path VARCHAR(1024) NOT NULL,
    file_size BIGSERIAL NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    uploader_id BIGSERIAL NOT NULL REFERENCES users(id), -- 수정됨
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL
);

--- 인덱스 ---
-- groups 테이블
CREATE INDEX idx_groups_parent_group_id ON groups(parent_group_id);
CREATE INDEX idx_groups_leader_person_id ON groups(leader_person_id);
CREATE INDEX idx_groups_group_type ON groups(group_type);

-- oauth_accounts 테이블
CREATE INDEX idx_oauth_accounts_user_id ON oauth_accounts(user_id);
CREATE INDEX idx_oauth_accounts_social_type ON oauth_accounts(social_type); -- 컬럼명 변경됨
CREATE INDEX idx_oauth_accounts_is_deleted ON oauth_accounts(is_deleted);

-- members 테이블
CREATE INDEX idx_members_name ON members(name);
CREATE INDEX idx_members_nickname ON members(nickname);
CREATE INDEX idx_members_is_deleted ON members(is_deleted);
CREATE INDEX idx_members_registered_by_user_id ON members(registered_by_user_id);

-- attendance 테이블
CREATE INDEX idx_attendance_department_id ON attendance(department_id);
CREATE INDEX idx_attendance_attendance_date ON attendance(attendance_date);
CREATE INDEX idx_attendance_status ON attendance(status);
CREATE INDEX idx_attendance_checked_by_user_id ON attendance(checked_by_user_id);
CREATE INDEX idx_attendance_is_deleted ON attendance(is_deleted);

-- family_relations 테이블
CREATE INDEX idx_family_relations_member2_id ON family_relations(member2_id);
CREATE INDEX idx_family_relations_relationship_type ON family_relations(relationship_type);
CREATE INDEX idx_family_relations_is_deleted ON family_relations(is_deleted);

-- posts 테이블 (테이블명 변경됨)
CREATE INDEX idx_posts_category_nm ON posts(category_nm);
CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);
CREATE INDEX idx_posts_view_count ON posts(view_count DESC);
CREATE INDEX idx_posts_like_count ON posts(like_count DESC);
CREATE INDEX idx_posts_is_deleted ON posts(is_deleted);

-- post_youtube 테이블 (테이블명 변경됨)
CREATE INDEX idx_post_youtube_post_id ON post_youtube(post_id);

-- comments 테이블 (테이블명 변경됨)
CREATE INDEX idx_comments_post_id ON comments(post_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_comments_parent_comment_id ON comments(parent_comment_id);
CREATE INDEX idx_comments_created_at ON comments(created_at);
CREATE INDEX idx_comments_is_deleted ON comments(is_deleted);

-- files 테이블 (테이블명 변경됨)
CREATE INDEX idx_files_related_entity ON files(related_entity_id, related_entity_type);
CREATE INDEX idx_files_uploader_id ON files(uploader_id);
CREATE INDEX idx_files_is_deleted ON files(is_deleted);

-- users 테이블
CREATE INDEX idx_users_is_deleted ON users(is_deleted);

-- 1. 부서 (departments) - Updated List
INSERT INTO departments (name, description, created_by, modified_by)
VALUES
    ('영아부', '영아 대상 부서', 1, 1),
    ('유치부', '유치원생 대상 부서', 1, 1),
    ('유년부', '초등학교 저학년 대상 부서', 1, 1),
    ('초등부', '초등학교 고학년 대상 부서', 1, 1),
    ('중등부', '중학생 대상 부서', 1, 1),
    ('고등부', '고등학생 대상 부서', 1, 1),
    ('영어예배부', '영어로 진행되는 예배 부서', 1, 1),
    ('청년부', '교회 청년들을 위한 부서', 1, 1),
    ('엘림가족부', '젊은 부부들을 위한 부서', 1, 1),
    ('장년부', '교회 장년들을 위한 부서', 1, 1)
ON CONFLICT (id) DO NOTHING;

-- 2. 역할 (roles)
INSERT INTO roles (name, description, created_by, modified_by)
values
    ('성도', '일반 성도 역할입니다.', 1, 1),
    ('리더', '부서 또는 그룹의 리더 역할입니다.', 1, 1),
    ('교사', '교육을 담당하는 역할입니다.', 1, 1),
    ('마을장', '부서 또는 그룹의 마을장 역할입니다.', 1, 1),
    ('스탭장', '스탭장 역할입니다.', 1, 1),
    ('스탭', '스탭 역할입니다.', 1, 1),
    ('부장', '스탭 역할입니다.', 1, 1),
    ('부감', '스탭 역할입니다.', 1, 1),
    ('총무', '스탭 역할입니다.', 1, 1),
    ('회계', '스탭 역할입니다.', 1, 1),
    ('간사', '스탭 역할입니다.', 1, 1),
    ('목사', '스탭 역할입니다.', 1, 1),
    ('아이', '스탭 역할입니다.', 1, 1),
    ('찬양팀', '찬양팀 역할입니다.', 1, 1)
ON CONFLICT (id) DO NOTHING;

-- 3. 게시판 카테고리 (posts_categories)
INSERT INTO posts_categories (name, description, created_by, modified_by)
VALUES
    ('FREE', '자유롭게 글을 작성하는 게시판입니다.', 1, 1),
    ('NOTICE', '교회 공지사항 게시판입니다.', 1, 1),
    ('COMMUNITY_FEED', 'SNS 피드 게시판입니다.', 1, 1)
ON CONFLICT (category_id) DO NOTHING;

-- 4. 사용자 (users) - 여러 명 생성
INSERT INTO users (email, password, created_by, modified_by)
VALUES
    ('admin@example.com', '$2a$10$abcdefghijklmnopqrstuv', 1, 1), -- 관리자/리더
    ('member1@example.com', '$2a$10$wxyzabcdefghijklmnopq', 1, 1), -- 성도1
    ('member2@example.com', '$2a$10$1234567890abcdefghijkl', 1, 1), -- 성도2
    ('leader2@example.com', '$2a$10$zyxwutsrqponmlkjihgfe', 1, 1),  -- 다른 리더
    ('children@example.com', '$2a$10$zyxwutsrqponmlkjihgfe', 1, 1)  -- 아이
ON CONFLICT (id) DO NOTHING;

-- 5. OAuth 계정 (oauth_accounts) - 사용자 1, 2에 연결
INSERT INTO oauth_accounts (user_id, social_type, created_by, modified_by)
VALUES
    (1, 'google', 1, 1),
    (2, 'kakao', 1, 1)
ON CONFLICT (id) DO NOTHING; -- Assumes id is SERIAL/BIGSERIAL

-- 6. 성도 정보 (members) - 사용자들에 연결
INSERT INTO members (user_id, name, birthdate, gender, address, phone_number, nickname, registered_by_user_id, created_by, modified_by)
VALUES
    (1, '홍길동', '1995-03-15', 'MALE', '서울시 강남구', '010-1234-5678', '쾌도홍', 1, 1, 1),
    (2, '성춘향', '1996-08-20', 'FEMALE', '서울시 서초구', '010-9876-5432', '춘향사랑', 1, 1, 1),
    (3, '이몽룡', '1994-11-10', 'MALE', '서울시 송파구', '010-1111-2222', '몽룡이', 1, 1, 1),
    (4, '김관리', '1990-01-01', 'MALE', '서울시 마포구', '010-5555-6666', '관리자킴', 1, 1, 1),
    (5, '김아이', '2025-01-01', 'MALE', '서울시 노원구', '010-6666-7777', '김칠드런', 1, 1, 1)
ON CONFLICT (id) DO NOTHING;

-- 9. 출석 정보 (attendance) - 업데이트된 부서 ID 사용
INSERT INTO attendance (member_id, department_id, attendance_date, status, check_in_time, checked_by_user_id, method, created_by, modified_by)
VALUES
    (1, 8, '2025-04-27', 'PRESENT', '2025-04-27 10:05:00', 1, 'LEADER', 1, 1), -- 홍길동, 청년부(8), 4/27 출석
    (2, 8, '2025-04-27', 'ABSENT', NULL, 1, 'LEADER', 1, 1),                 -- 성춘향, 청년부(8), 4/27 결석
    (1, 8, '2025-04-20', 'ONLINE', '2025-04-20 10:15:00', 1, 'SELF', 1, 1), -- 홍길동, 청년부(8), 4/20 온라인(본인체크)
    (3, 10, '2025-04-27', 'LATE', '2025-04-27 11:30:00', 4, 'LEADER', 1, 1)   -- 이몽룡, 장년부(10), 4/27 지각 (김관리 체크)
ON CONFLICT (member_id, department_id, attendance_date) DO NOTHING;

-- 10. 가족 관계 (family_relations)
INSERT INTO family_relations (member1_id, member2_id, relationship_type, created_by, modified_by)
VALUES
    (1, 2, 'SPOUSE', 1, 1), -- 홍길동 - 성춘향 (배우자)
    (1, 3, 'SIBLING', 1, 1) -- 홍길동 - 이몽룡 (형제자매 - 예시)
ON CONFLICT (member1_id, member2_id, relationship_type) DO NOTHING;

-- 11. 게시글 (posts) - 여러 개 작성
INSERT INTO posts (category_nm, user_id, user_nm, email, title, content, created_by, modified_by)
VALUES
    ('FREE', 1, '홍길동','admin@example.com','첫 번째 자유게시글', '자유게시판 내용입니다.', 1, 1),
    ('NOTICE', 4, '김관리','leader2@example.com','교회 대청소 안내', '5월 첫째 주 토요일 대청소를 진행합니다.', 4, 4),
    ('COMMUNITY_FEED', 1, '홍길동','admin@example.com','피드 게시판 공유', '피드 게시판 첫 글입니다.', 1, 1)
ON CONFLICT (post_id) DO NOTHING;

-- 12. 게시글-유튜브 (post_youtube) - 게시글 3에 연결
INSERT INTO post_youtube (post_id, youtube_video_id, created_by, modified_by)
VALUES (3, 'abcdef12345', 1, 1) -- Example YouTube video ID
ON CONFLICT (post_id, youtube_video_id) DO NOTHING;

-- 13. 댓글 (comments) - 여러 댓글, 대댓글 포함
INSERT INTO comments (post_id, user_id, user_nm, email, content, created_by, modified_by)
VALUES
    (1, 2, '성춘향', 'member1@example.com','좋은 글 감사합니다!', 2, 2),         -- 성춘향 -> 게시글 1 댓글
    (2, 3, '이몽룡','member2@example.com','확인했습니다. 참석하겠습니다.', 3, 3), -- 이몽룡 -> 게시글 2 댓글
    (1, 3, '이몽룡', 'member2@example.com','저도 잘 읽었습니다.', 3, 3)          -- 이몽룡 -> 게시글 1 댓글
ON CONFLICT (comment_id) DO NOTHING;

-- 대댓글 예시 (댓글 1에 대한 대댓글)
INSERT INTO comments (post_id, user_id, user_nm, email, parent_comment_id, content, created_by, modified_by)
VALUES (1, 1, '홍길동','admin@example.com',1,'읽어주셔서 감사합니다!', 1, 1) -- 홍길동 -> 댓글 1 (성춘향)에 대한 대댓글
ON CONFLICT (comment_id) DO NOTHING;

-- 14. 파일 (files) - 게시글 2 (공지사항)에 파일 첨부
INSERT INTO files (related_entity_id, related_entity_type, original_filename, stored_filename, file_path, file_size, mime_type, uploader_id, created_by, modified_by)
VALUES (2, 'post', '청소구역안내.pdf', 'unique_stored_cleaning_guide.pdf', '/uploads/files', 204800, 'application/pdf', 4, 4, 4)
ON CONFLICT (file_id) DO NOTHING;
