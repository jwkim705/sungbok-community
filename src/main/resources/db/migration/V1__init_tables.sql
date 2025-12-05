-- ============================================
-- DROP & RECREATE SCHEMA (Clean Slate)
-- ============================================
DROP SCHEMA IF EXISTS public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO public;

-- ============================================
-- CREATE TABLES
-- ============================================

-- Apps (테넌트) 테이블
CREATE TABLE apps (
    app_id BIGSERIAL PRIMARY KEY,
    app_name VARCHAR(255) NOT NULL,
    app_key VARCHAR(100) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT chk_app_id_positive CHECK (app_id > 0),
    CONSTRAINT chk_status_valid CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

-- 부서 테이블
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT
);

-- 역할 테이블
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL, -- 예: 교사, 리더, 성도, 부장, 목사
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT
);

-- 게시판 카테고리 테이블
CREATE TABLE posts_categories (
    category_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE, -- 예 : FREE, YOUTUBE, SNS
    description TEXT, -- 카테고리 설명
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT
);

-- 사용자 계정 테이블
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL DEFAULT 1,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255), -- Form login 용. OAuth 사용자는 null일 수 있음
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_users_app FOREIGN KEY (app_id) REFERENCES apps(app_id),
    CONSTRAINT users_app_email_unique UNIQUE (app_id, email),
    CONSTRAINT users_app_id_unique UNIQUE (app_id, id)
);

-- OAuth 계정 정보 테이블
CREATE TABLE oauth_accounts (
    id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL DEFAULT 1,
    user_id BIGINT NOT NULL,
    social_type VARCHAR(50) NOT NULL, -- 예: google, kakao
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_oauth_accounts_app FOREIGN KEY (app_id) REFERENCES apps(app_id),
    CONSTRAINT fk_oauth_accounts_user FOREIGN KEY (app_id, user_id) REFERENCES users(app_id, id),
    CONSTRAINT oauth_accounts_app_id_unique UNIQUE (app_id, id),
    CONSTRAINT oauth_accounts_app_user_social_unique UNIQUE (app_id, user_id, social_type)
);

-- 교회 성도 정보 테이블
CREATE TABLE members (
    id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL DEFAULT 1,
    user_id BIGINT,
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
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    registered_by_user_id BIGINT, -- 등록한 관리자/리더 ID
    CONSTRAINT fk_members_app FOREIGN KEY (app_id) REFERENCES apps(app_id),
    CONSTRAINT fk_members_user FOREIGN KEY (app_id, user_id) REFERENCES users(app_id, id),
    CONSTRAINT fk_members_registered_by FOREIGN KEY (app_id, registered_by_user_id) REFERENCES users(app_id, id),
    CONSTRAINT members_app_id_unique UNIQUE (app_id, id),
    CONSTRAINT members_app_user_unique UNIQUE (app_id, user_id)
    -- 기타 필요한 정보 (세례명, 직분 등)
);

-- 출석 정보 테이블
CREATE TABLE attendance (
    id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL DEFAULT 1,
    member_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL REFERENCES departments(id), -- 출석한 부서 (공유 테이블)
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL, -- 예: PRESENT, ABSENT, LATE, ONLINE (실제 값 정의 필요)
    check_in_time TIMESTAMP,
    checked_by_user_id BIGINT, -- 출석 체크한 사용자 (null이면 본인 체크)
    method VARCHAR(10) NOT NULL, -- SELF, LEADER
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_attendance_app FOREIGN KEY (app_id) REFERENCES apps(app_id),
    CONSTRAINT fk_attendance_member FOREIGN KEY (app_id, member_id) REFERENCES members(app_id, id),
    CONSTRAINT fk_attendance_checked_by FOREIGN KEY (app_id, checked_by_user_id) REFERENCES users(app_id, id),
    CONSTRAINT attendance_app_id_unique UNIQUE (app_id, id),
    CONSTRAINT attendance_app_member_dept_date_unique UNIQUE (app_id, member_id, department_id, attendance_date) -- 특정 날짜, 특정 부서에 한 성도는 하나의 출석 기록만 가짐
);

-- 가족 관계 테이블
CREATE TABLE family_relations (
    id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL DEFAULT 1,
    member1_id BIGINT NOT NULL,
    member2_id BIGINT NOT NULL,
    relationship_type VARCHAR(50) NOT NULL, -- 예: SPOUSE, PARENT, CHILD, SIBLING (실제 값 정의 필요)
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_family_relations_app FOREIGN KEY (app_id) REFERENCES apps(app_id),
    CONSTRAINT fk_family_relations_member1 FOREIGN KEY (app_id, member1_id) REFERENCES members(app_id, id),
    CONSTRAINT fk_family_relations_member2 FOREIGN KEY (app_id, member2_id) REFERENCES members(app_id, id),
    CONSTRAINT family_relations_app_id_unique UNIQUE (app_id, id),
    CONSTRAINT family_relations_app_members_type_unique UNIQUE (app_id, member1_id, member2_id, relationship_type)
);

CREATE TABLE groups (
    group_id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL DEFAULT 1,
    name VARCHAR(255) NOT NULL, -- 그룹 이름 (예: '유년부', '마을', '1교구')
    group_type VARCHAR(50) NOT NULL, -- 그룹 유형 (예: 'Department', 'Class', 'Parish', 'Village', 'SmallGroup', 'Team')
    -- 어떤 유형들이 있는지 명확히 정의하고 사용하는 것이 중요합니다.
    parent_group_id BIGINT, -- 상위 그룹 ID (최상위 그룹은 NULL)
    -- 상위 그룹 삭제 시 하위 그룹의 연결을 끊음 (NULL로 설정)
    leader_person_id BIGINT, -- 그룹 리더/교사/담당자의 members 테이블 ID
    -- 리더로 지정된 멤버 삭제 시 그룹의 리더 정보를 NULL로 설정
    description TEXT NULL, -- 그룹 설명
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_groups_app FOREIGN KEY (app_id) REFERENCES apps(app_id),
    CONSTRAINT fk_groups_parent FOREIGN KEY (app_id, parent_group_id) REFERENCES groups(app_id, group_id) ON DELETE SET NULL,
    CONSTRAINT fk_groups_leader FOREIGN KEY (app_id, leader_person_id) REFERENCES members(app_id, id) ON DELETE SET NULL,
    CONSTRAINT groups_app_id_unique UNIQUE (app_id, group_id)
);

CREATE TABLE membership (
    membership_id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL DEFAULT 1,
    person_id BIGINT NOT NULL, -- 소속된 사람의 members 테이블 ID
    -- 멤버 삭제 시 해당 멤버의 모든 멤버십 기록도 함께 삭제
    group_id BIGINT NOT NULL, -- 소속된 그룹의 Groups 테이블 ID
    -- 그룹 삭제 시 해당 그룹에 속한 모든 멤버십 기록도 함께 삭제
    role VARCHAR(100) NOT NULL, -- 그룹 내에서의 역할 (예: 'Student', 'Teacher', 'Member', 'Leader', 'Assistant')
    start_date DATE NOT NULL,
    end_date DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_membership_app FOREIGN KEY (app_id) REFERENCES apps(app_id),
    CONSTRAINT fk_membership_person FOREIGN KEY (app_id, person_id) REFERENCES members(app_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_membership_group FOREIGN KEY (app_id, group_id) REFERENCES groups(app_id, group_id) ON DELETE CASCADE,
    CONSTRAINT membership_app_id_unique UNIQUE (app_id, membership_id)
);

-- 게시글 테이블
CREATE TABLE posts (
    post_id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL DEFAULT 1,
    table_type VARCHAR(50) NOT NULL, -- (홈페이지,관리자,커뮤니티)
    category_nm VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL,
    user_nm VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    view_count INT DEFAULT 0 NOT NULL,
    like_count INT DEFAULT 0 NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_posts_app FOREIGN KEY (app_id) REFERENCES apps(app_id),
    CONSTRAINT fk_posts_user FOREIGN KEY (app_id, user_id) REFERENCES users(app_id, id),
    CONSTRAINT posts_app_id_unique UNIQUE (app_id, post_id)
);

-- 게시글-유튜브 영상 연결 테이블
CREATE TABLE post_youtube (
    post_youtube_id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL DEFAULT 1,
    post_id BIGINT NOT NULL,
    youtube_video_id VARCHAR(50) NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_post_youtube_app FOREIGN KEY (app_id) REFERENCES apps(app_id),
    CONSTRAINT fk_post_youtube_post FOREIGN KEY (app_id, post_id) REFERENCES posts(app_id, post_id) ON DELETE CASCADE,
    CONSTRAINT post_youtube_app_id_unique UNIQUE (app_id, post_youtube_id),
    CONSTRAINT post_youtube_app_post_video_unique UNIQUE (app_id, post_id, youtube_video_id)
);

CREATE TABLE post_likes (
    app_id BIGINT NOT NULL DEFAULT 1,
    user_id BIGINT NOT NULL,                -- 좋아요 누른 사용자 ID (users 테이블 참조)
    post_id BIGINT NOT NULL,                -- 좋아요 눌린 게시글 ID (posts 테이블 참조)
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP, -- 좋아요 누른 시각

    PRIMARY KEY (app_id, user_id, post_id),        -- 한 사용자가 같은 게시글에 중복 좋아요 방지 (복합 기본키)

    CONSTRAINT fk_post_likes_app
    FOREIGN KEY(app_id)
    REFERENCES apps(app_id)
    ON DELETE CASCADE,

    CONSTRAINT fk_post_likes_user
    FOREIGN KEY(app_id, user_id)
    REFERENCES users(app_id, id)
    ON DELETE CASCADE,                  -- 사용자가 탈퇴하면 좋아요 기록도 삭제

    CONSTRAINT fk_post_likes_post
    FOREIGN KEY(app_id, post_id)
    REFERENCES posts(app_id, post_id)
    ON DELETE CASCADE                   -- 게시글이 삭제되면 좋아요 기록도 삭제
);

-- 댓글 테이블
CREATE TABLE comments (
    comment_id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL DEFAULT 1,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    user_nm VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    parent_comment_id BIGINT,
    content TEXT NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_comments_app FOREIGN KEY (app_id) REFERENCES apps(app_id),
    CONSTRAINT fk_comments_post FOREIGN KEY (app_id, post_id) REFERENCES posts(app_id, post_id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user FOREIGN KEY (app_id, user_id) REFERENCES users(app_id, id),
    CONSTRAINT fk_comments_parent FOREIGN KEY (app_id, parent_comment_id) REFERENCES comments(app_id, comment_id) ON DELETE CASCADE,
    CONSTRAINT comments_app_id_unique UNIQUE (app_id, comment_id)
);

-- 파일 테이블
CREATE TABLE files (
    file_id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL DEFAULT 1,
    related_entity_id BIGINT NOT NULL,
    related_entity_type VARCHAR(50) NOT NULL, -- 예: 'post', 'comment'
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(1024) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    uploader_id BIGINT NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_files_app FOREIGN KEY (app_id) REFERENCES apps(app_id),
    CONSTRAINT fk_files_uploader FOREIGN KEY (app_id, uploader_id) REFERENCES users(app_id, id),
    CONSTRAINT files_app_id_unique UNIQUE (app_id, file_id),
    CONSTRAINT files_app_stored_filename_unique UNIQUE (app_id, stored_filename)
);

--- 인덱스 ---
-- apps 테이블
CREATE INDEX idx_apps_app_key ON apps(app_key);
CREATE INDEX idx_apps_status ON apps(status);

-- users 테이블
CREATE INDEX idx_users_app_email ON users(app_id, email);
CREATE INDEX idx_users_app_deleted ON users(app_id, is_deleted);

-- oauth_accounts 테이블
CREATE INDEX idx_oauth_accounts_app_user ON oauth_accounts(app_id, user_id);
CREATE INDEX idx_oauth_accounts_app_social ON oauth_accounts(app_id, social_type);
CREATE INDEX idx_oauth_accounts_app_deleted ON oauth_accounts(app_id, is_deleted);

-- members 테이블
CREATE INDEX idx_members_app_name ON members(app_id, name);
CREATE INDEX idx_members_app_nickname ON members(app_id, nickname);
CREATE INDEX idx_members_app_user ON members(app_id, user_id);
CREATE INDEX idx_members_app_deleted ON members(app_id, is_deleted);

-- attendance 테이블
CREATE INDEX idx_attendance_app_dept ON attendance(app_id, department_id);
CREATE INDEX idx_attendance_app_date ON attendance(app_id, attendance_date);
CREATE INDEX idx_attendance_app_member ON attendance(app_id, member_id);
CREATE INDEX idx_attendance_app_status ON attendance(app_id, status);
CREATE INDEX idx_attendance_app_deleted ON attendance(app_id, is_deleted);

-- family_relations 테이블
CREATE INDEX idx_family_relations_app_member1 ON family_relations(app_id, member1_id);
CREATE INDEX idx_family_relations_app_member2 ON family_relations(app_id, member2_id);
CREATE INDEX idx_family_relations_app_type ON family_relations(app_id, relationship_type);
CREATE INDEX idx_family_relations_app_deleted ON family_relations(app_id, is_deleted);

-- groups 테이블
CREATE INDEX idx_groups_app_parent ON groups(app_id, parent_group_id);
CREATE INDEX idx_groups_app_leader ON groups(app_id, leader_person_id);
CREATE INDEX idx_groups_app_type ON groups(app_id, group_type);

-- membership 테이블
CREATE INDEX idx_membership_app_person ON membership(app_id, person_id);
CREATE INDEX idx_membership_app_group ON membership(app_id, group_id);

-- posts 테이블
CREATE INDEX idx_posts_app_category ON posts(app_id, category_nm);
CREATE INDEX idx_posts_app_user ON posts(app_id, user_id);
CREATE INDEX idx_posts_app_created ON posts(app_id, created_at DESC);
CREATE INDEX idx_posts_app_view ON posts(app_id, view_count DESC);
CREATE INDEX idx_posts_app_like ON posts(app_id, like_count DESC);
CREATE INDEX idx_posts_app_deleted ON posts(app_id, is_deleted);

-- post_youtube 테이블
CREATE INDEX idx_post_youtube_app_post ON post_youtube(app_id, post_id);

-- comments 테이블
CREATE INDEX idx_comments_app_post ON comments(app_id, post_id);
CREATE INDEX idx_comments_app_user ON comments(app_id, user_id);
CREATE INDEX idx_comments_app_parent ON comments(app_id, parent_comment_id);
CREATE INDEX idx_comments_app_created ON comments(app_id, created_at);
CREATE INDEX idx_comments_app_deleted ON comments(app_id, is_deleted);

-- files 테이블
CREATE INDEX idx_files_app_entity ON files(app_id, related_entity_id, related_entity_type);
CREATE INDEX idx_files_app_uploader ON files(app_id, uploader_id);
CREATE INDEX idx_files_app_deleted ON files(app_id, is_deleted);

