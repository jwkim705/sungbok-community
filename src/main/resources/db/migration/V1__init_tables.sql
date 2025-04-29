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
CREATE TABLE board_categories (
    category_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE, -- 예 : 자유게시판, 유튜브 공유, SNS
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
    user_id BIGSERIAL NOT NULL REFERENCES users(id) UNIQUE,
    name VARCHAR(50) NOT NULL,
    birthdate DATE,
    gender VARCHAR(10), -- MALE, FEMALE
    address TEXT,
    phone_number VARCHAR(20) UNIQUE,
    picture TEXT, -- Oauth 프로필
    nickname VARCHAR(50),
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL,
    registered_by_user_id BIGSERIAL REFERENCES users(id) -- 등록한 관리자/리더 ID
    -- 기타 필요한 정보 (세례명, 직분 등)
);

-- 성도-부서 소속 정보 테이블
CREATE TABLE member_departments (
    member_id BIGSERIAL NOT NULL REFERENCES members(id),
    department_id BIGSERIAL NOT NULL REFERENCES departments(id),
    start_date DATE DEFAULT CURRENT_DATE,
    end_date DATE, -- 부서 이동 시 기록
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL,
    PRIMARY KEY (member_id, department_id)
);

-- 사용자의 부서 내 역할 테이블
CREATE TABLE user_department_roles (
    user_id BIGSERIAL NOT NULL REFERENCES users(id),
    department_id BIGSERIAL NOT NULL REFERENCES departments(id),
    department_name VARCHAR(100) NOT NULL,
    role_id BIGSERIAL NOT NULL REFERENCES roles(id),
    role_name VARCHAR(100) NOT NULL, -- 예: 교사, 리더, 성도, 부장, 목사
    assignment_date DATE DEFAULT CURRENT_DATE,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL,
    PRIMARY KEY (user_id, department_id, role_id)
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

-- 게시글 테이블
CREATE TABLE posts (
    post_id BIGSERIAL PRIMARY KEY,
    category_id BIGSERIAL NOT NULL REFERENCES board_categories(category_id),
    user_id BIGSERIAL NOT NULL REFERENCES users(id), -- 수정됨
    title VARCHAR(255) NOT NULL,
    content TEXT,
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

-- 댓글 테이블
CREATE TABLE comments (
    comment_id BIGSERIAL PRIMARY KEY,
    post_id BIGSERIAL NOT NULL REFERENCES posts(post_id) ON DELETE CASCADE,
    user_id BIGSERIAL NOT NULL REFERENCES users(id), -- 수정됨
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

-- oauth_accounts 테이블
CREATE INDEX idx_oauth_accounts_user_id ON oauth_accounts(user_id);
CREATE INDEX idx_oauth_accounts_social_type ON oauth_accounts(social_type); -- 컬럼명 변경됨
CREATE INDEX idx_oauth_accounts_is_deleted ON oauth_accounts(is_deleted);

-- members 테이블
CREATE INDEX idx_members_name ON members(name);
CREATE INDEX idx_members_nickname ON members(nickname);
CREATE INDEX idx_members_is_deleted ON members(is_deleted);
CREATE INDEX idx_members_registered_by_user_id ON members(registered_by_user_id);

-- member_departments 테이블
CREATE INDEX idx_member_departments_department_id ON member_departments(department_id);
CREATE INDEX idx_member_departments_is_deleted ON member_departments(is_deleted);

-- user_department_roles 테이블
CREATE INDEX idx_user_department_roles_department_id ON user_department_roles(department_id);
CREATE INDEX idx_user_department_roles_role_id ON user_department_roles(role_id);
CREATE INDEX idx_user_department_roles_is_deleted ON user_department_roles(is_deleted);

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
CREATE INDEX idx_posts_category_id ON posts(category_id);
CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_created_at ON posts(created_at);
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

