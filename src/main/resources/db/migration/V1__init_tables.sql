-- ============================================
-- DROP & RECREATE SCHEMA (Clean Slate)
-- ============================================
DROP SCHEMA IF EXISTS public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO public;

-- ============================================
-- CREATE TABLES
-- ============================================

-- App Types 테이블 (Church, Cafe, Company, School)
CREATE TABLE app_types (
    app_type_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon_url VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE app_types IS '앱 타입 (교회, 카페, 회사, 학교 등)';
COMMENT ON COLUMN app_types.name IS '앱 타입 이름 (Church Community, Cafe Community 등)';

-- Organizations 테이블 (이전 apps 테이블)
CREATE TABLE organizations (
    org_id BIGSERIAL PRIMARY KEY,
    app_type_id BIGINT NOT NULL,
    org_name VARCHAR(255) NOT NULL,
    org_key VARCHAR(100) UNIQUE NOT NULL,
    is_public BOOLEAN DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_organizations_app_type FOREIGN KEY (app_type_id) REFERENCES app_types(app_type_id),
    CONSTRAINT chk_org_id_positive CHECK (org_id > 0),
    CONSTRAINT chk_status_valid CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

COMMENT ON TABLE organizations IS '조직 (Church A, Cafe B, Company C 등)';
COMMENT ON COLUMN organizations.is_public IS 'Guest 접근 허용 여부';

-- 부서 테이블 (공유 테이블 - org_id 없음)
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT
);

COMMENT ON TABLE departments IS '부서 테이블 (조직 간 공유)';

-- 역할 테이블 (조직별 커스텀 역할)
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT,
    name VARCHAR(100) NOT NULL,
    level INT DEFAULT 1,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_roles_organization FOREIGN KEY (org_id) REFERENCES organizations(org_id),
    CONSTRAINT roles_org_name_unique UNIQUE (org_id, name)
);

COMMENT ON TABLE roles IS '조직별 역할 (성도, 리더, 마을장 등)';
COMMENT ON COLUMN roles.org_id IS '조직 ID (NULL이면 공유 역할)';
COMMENT ON COLUMN roles.level IS '역할 레벨 (1=낮음, 3=높음)';

-- 역할 권한 테이블 (API별 세밀한 권한 제어)
CREATE TABLE role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    allowed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT uq_role_resource_action UNIQUE (role_id, resource, action)
);

COMMENT ON TABLE role_permissions IS 'API별 세밀한 권한 제어';
COMMENT ON COLUMN role_permissions.resource IS 'API 리소스 (posts, comments, users, roles 등)';
COMMENT ON COLUMN role_permissions.action IS 'CRUD 액션 (create, read, update, delete)';
COMMENT ON COLUMN role_permissions.allowed IS '권한 허용 여부';

-- 게시판 카테고리 테이블 (공유 테이블 - org_id 없음)
CREATE TABLE posts_categories (
    category_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT
);

COMMENT ON TABLE posts_categories IS '게시판 카테고리 (조직 간 공유)';

-- 사용자 계정 테이블 (플랫폼 레벨 - org_id 없음)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT
);

COMMENT ON TABLE users IS '플랫폼 레벨 사용자 (조직 독립적)';
COMMENT ON COLUMN users.password IS 'Form login용 (OAuth는 NULL)';

-- OAuth 계정 정보 테이블
CREATE TABLE oauth_accounts (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL DEFAULT 1,
    user_id BIGINT NOT NULL,
    social_type VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255),
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_oauth_accounts_org FOREIGN KEY (org_id) REFERENCES organizations(org_id),
    CONSTRAINT fk_oauth_accounts_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT oauth_accounts_org_id_unique UNIQUE (org_id, id),
    CONSTRAINT oauth_accounts_org_provider_user_unique UNIQUE (org_id, social_type, provider_user_id)
);

COMMENT ON TABLE oauth_accounts IS 'OAuth 소셜 로그인 계정';

-- 멤버십 테이블 (User ↔ Organization, 승인 워크플로우)
CREATE TABLE memberships (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL DEFAULT 1,
    user_id BIGINT,
    name VARCHAR(50) NOT NULL,
    birthdate DATE,
    gender VARCHAR(10),
    address TEXT,
    phone_number VARCHAR(20),
    picture TEXT,
    nickname VARCHAR(50),
    status VARCHAR(20) DEFAULT 'PENDING',
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP,
    approved_by BIGINT,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    registered_by_user_id BIGINT,
    CONSTRAINT fk_memberships_org FOREIGN KEY (org_id) REFERENCES organizations(org_id),
    CONSTRAINT fk_memberships_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_memberships_approver FOREIGN KEY (approved_by) REFERENCES users(id),
    CONSTRAINT fk_memberships_registered_by FOREIGN KEY (registered_by_user_id) REFERENCES users(id),
    CONSTRAINT memberships_org_id_unique UNIQUE (org_id, id),
    CONSTRAINT memberships_org_user_unique UNIQUE (org_id, user_id)
);

COMMENT ON TABLE memberships IS '사용자-조직 멤버십 (role은 membership_roles 테이블 사용)';
COMMENT ON COLUMN memberships.status IS 'PENDING/APPROVED/REJECTED';
COMMENT ON COLUMN memberships.approved_by IS '승인한 사용자 ID';

-- 멤버십-역할 N:M 관계 테이블
CREATE TABLE membership_roles (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    membership_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_membership_roles_org FOREIGN KEY (org_id) REFERENCES organizations(org_id),
    CONSTRAINT fk_membership_roles_membership FOREIGN KEY (org_id, membership_id)
        REFERENCES memberships(org_id, id),
    CONSTRAINT fk_membership_roles_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_membership_roles_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id),
    CONSTRAINT membership_roles_org_id_unique UNIQUE (org_id, id),
    CONSTRAINT membership_roles_unique UNIQUE (org_id, membership_id, role_id)
);

COMMENT ON TABLE membership_roles IS '멤버십-역할 N:M 관계 (한 유저가 여러 역할 가능)';
COMMENT ON COLUMN membership_roles.is_primary IS '대표 역할 여부 (첫번째 역할, UI 표시용)';

CREATE INDEX idx_membership_roles_membership ON membership_roles(org_id, membership_id);
CREATE INDEX idx_membership_roles_role ON membership_roles(role_id);
CREATE INDEX idx_membership_roles_lookup ON membership_roles(org_id, membership_id, is_primary);

-- 출석 정보 테이블
CREATE TABLE attendance (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL DEFAULT 1,
    member_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL REFERENCES departments(id),
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    check_in_time TIMESTAMP,
    checked_by_user_id BIGINT,
    method VARCHAR(10) NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_attendance_org FOREIGN KEY (org_id) REFERENCES organizations(org_id),
    CONSTRAINT fk_attendance_member FOREIGN KEY (org_id, member_id) REFERENCES memberships(org_id, id),
    CONSTRAINT fk_attendance_checked_by FOREIGN KEY (checked_by_user_id) REFERENCES users(id),
    CONSTRAINT attendance_org_id_unique UNIQUE (org_id, id),
    CONSTRAINT attendance_org_member_dept_date_unique UNIQUE (org_id, member_id, department_id, attendance_date)
);

COMMENT ON TABLE attendance IS '출석 정보';

-- 가족 관계 테이블
CREATE TABLE family_relations (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL DEFAULT 1,
    member1_id BIGINT NOT NULL,
    member2_id BIGINT NOT NULL,
    relationship_type VARCHAR(50) NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_family_relations_org FOREIGN KEY (org_id) REFERENCES organizations(org_id),
    CONSTRAINT fk_family_relations_member1 FOREIGN KEY (org_id, member1_id) REFERENCES memberships(org_id, id),
    CONSTRAINT fk_family_relations_member2 FOREIGN KEY (org_id, member2_id) REFERENCES memberships(org_id, id),
    CONSTRAINT family_relations_org_id_unique UNIQUE (org_id, id),
    CONSTRAINT family_relations_org_members_type_unique UNIQUE (org_id, member1_id, member2_id, relationship_type)
);

COMMENT ON TABLE family_relations IS '가족 관계';

-- 그룹 테이블
CREATE TABLE groups (
    group_id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL DEFAULT 1,
    name VARCHAR(255) NOT NULL,
    group_type VARCHAR(50) NOT NULL,
    parent_group_id BIGINT,
    leader_person_id BIGINT,
    description TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_groups_org FOREIGN KEY (org_id) REFERENCES organizations(org_id),
    CONSTRAINT fk_groups_parent FOREIGN KEY (org_id, parent_group_id) REFERENCES groups(org_id, group_id) ON DELETE SET NULL,
    CONSTRAINT fk_groups_leader FOREIGN KEY (org_id, leader_person_id) REFERENCES memberships(org_id, id) ON DELETE SET NULL,
    CONSTRAINT groups_org_id_unique UNIQUE (org_id, group_id)
);

COMMENT ON TABLE groups IS '그룹 (마을, 교구, 소그룹 등)';

-- 그룹 멤버십 테이블
CREATE TABLE group_membership (
    membership_id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL DEFAULT 1,
    person_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    role VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_group_membership_org FOREIGN KEY (org_id) REFERENCES organizations(org_id),
    CONSTRAINT fk_group_membership_person FOREIGN KEY (org_id, person_id) REFERENCES memberships(org_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_group_membership_group FOREIGN KEY (org_id, group_id) REFERENCES groups(org_id, group_id) ON DELETE CASCADE,
    CONSTRAINT group_membership_org_id_unique UNIQUE (org_id, membership_id)
);

COMMENT ON TABLE group_membership IS '그룹 소속 정보';

-- 게시글 테이블
CREATE TABLE posts (
    post_id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL DEFAULT 1,
    table_type VARCHAR(50) NOT NULL,
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
    CONSTRAINT fk_posts_org FOREIGN KEY (org_id) REFERENCES organizations(org_id),
    CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT posts_org_id_unique UNIQUE (org_id, post_id)
);

COMMENT ON TABLE posts IS '게시글';

-- 게시글-유튜브 영상 연결 테이블
CREATE TABLE post_youtube (
    post_youtube_id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL DEFAULT 1,
    post_id BIGINT NOT NULL,
    youtube_video_id VARCHAR(50) NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_post_youtube_org FOREIGN KEY (org_id) REFERENCES organizations(org_id),
    CONSTRAINT fk_post_youtube_post FOREIGN KEY (org_id, post_id) REFERENCES posts(org_id, post_id) ON DELETE CASCADE,
    CONSTRAINT post_youtube_org_id_unique UNIQUE (org_id, post_youtube_id),
    CONSTRAINT post_youtube_org_post_video_unique UNIQUE (org_id, post_id, youtube_video_id)
);

COMMENT ON TABLE post_youtube IS '게시글-유튜브 연결';

-- 게시글 좋아요 테이블
CREATE TABLE post_likes (
    org_id BIGINT NOT NULL DEFAULT 1,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (org_id, user_id, post_id),
    CONSTRAINT fk_post_likes_org FOREIGN KEY(org_id) REFERENCES organizations(org_id) ON DELETE CASCADE,
    CONSTRAINT fk_post_likes_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_likes_post FOREIGN KEY(org_id, post_id) REFERENCES posts(org_id, post_id) ON DELETE CASCADE
);

COMMENT ON TABLE post_likes IS '게시글 좋아요';

-- 댓글 테이블
CREATE TABLE comments (
    comment_id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL DEFAULT 1,
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
    CONSTRAINT fk_comments_org FOREIGN KEY (org_id) REFERENCES organizations(org_id),
    CONSTRAINT fk_comments_post FOREIGN KEY (org_id, post_id) REFERENCES posts(org_id, post_id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_comments_parent FOREIGN KEY (org_id, parent_comment_id) REFERENCES comments(org_id, comment_id) ON DELETE CASCADE,
    CONSTRAINT comments_org_id_unique UNIQUE (org_id, comment_id)
);

COMMENT ON TABLE comments IS '댓글';

-- 파일 테이블
CREATE TABLE files (
    file_id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL DEFAULT 1,
    related_entity_id BIGINT NOT NULL,
    related_entity_type VARCHAR(50) NOT NULL,
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
    CONSTRAINT fk_files_org FOREIGN KEY (org_id) REFERENCES organizations(org_id),
    CONSTRAINT fk_files_uploader FOREIGN KEY (uploader_id) REFERENCES users(id),
    CONSTRAINT files_org_id_unique UNIQUE (org_id, file_id),
    CONSTRAINT files_org_stored_filename_unique UNIQUE (org_id, stored_filename)
);

COMMENT ON TABLE files IS '파일';

-- ============================================
-- INSERT INITIAL DATA
-- ============================================

-- App Types
INSERT INTO app_types (app_type_id, name, description) VALUES
(1, 'Church Community', 'Community platform for churches'),
(2, 'Company Community', 'Team collaboration for companies'),
(3, 'Cafe Community', 'Community for cafes and hobby groups'),
(4, 'School Community', 'Platform for schools');

-- Default Organization (Church A)
INSERT INTO organizations (org_id, app_type_id, org_name, org_key, is_public, status) VALUES
(1, 1, 'Church A', 'church-a', TRUE, 'ACTIVE');

-- Example Roles for Church A
INSERT INTO roles (id, org_id, name, level, description) VALUES
(1, 1, '성도', 1, '일반 교인'),
(2, 1, '리더', 2, '소그룹 리더'),
(3, 1, '마을장', 3, '마을 책임자');

-- Example Permissions for Church A Roles
-- 성도: 게시글/댓글 읽기, 작성만 가능
INSERT INTO role_permissions (role_id, resource, action, allowed) VALUES
(1, 'posts', 'read', TRUE),
(1, 'posts', 'create', TRUE),
(1, 'posts', 'update', FALSE),
(1, 'posts', 'delete', FALSE),
(1, 'comments', 'read', TRUE),
(1, 'comments', 'create', TRUE),
(1, 'comments', 'update', FALSE),
(1, 'comments', 'delete', FALSE);

-- 리더: 게시글/댓글 읽기, 작성, 수정 가능
INSERT INTO role_permissions (role_id, resource, action, allowed) VALUES
(2, 'posts', 'read', TRUE),
(2, 'posts', 'create', TRUE),
(2, 'posts', 'update', TRUE),
(2, 'posts', 'delete', FALSE),
(2, 'comments', 'read', TRUE),
(2, 'comments', 'create', TRUE),
(2, 'comments', 'update', TRUE),
(2, 'comments', 'delete', TRUE);

-- 마을장: 모든 권한
INSERT INTO role_permissions (role_id, resource, action, allowed) VALUES
(3, 'posts', 'read', TRUE),
(3, 'posts', 'create', TRUE),
(3, 'posts', 'update', TRUE),
(3, 'posts', 'delete', TRUE),
(3, 'comments', 'read', TRUE),
(3, 'comments', 'create', TRUE),
(3, 'comments', 'update', TRUE),
(3, 'comments', 'delete', TRUE),
(3, 'users', 'read', TRUE),
(3, 'users', 'update', TRUE),
(3, 'roles', 'read', TRUE),
(3, 'roles', 'update', TRUE);

-- ============================================
-- CREATE INDEXES
-- ============================================

-- app_types 테이블
CREATE INDEX idx_app_types_name ON app_types(name);
CREATE INDEX idx_app_types_active ON app_types(is_active);

-- organizations 테이블
CREATE INDEX idx_organizations_org_key ON organizations(org_key);
CREATE INDEX idx_organizations_app_type ON organizations(app_type_id);
CREATE INDEX idx_organizations_status ON organizations(status);
CREATE INDEX idx_organizations_public ON organizations(is_public);

-- roles 테이블
CREATE INDEX idx_roles_org_id ON roles(org_id);
CREATE INDEX idx_roles_level ON roles(level);

-- role_permissions 테이블
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_resource_action ON role_permissions(resource, action);

-- users 테이블
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_deleted ON users(is_deleted);

-- oauth_accounts 테이블
CREATE INDEX idx_oauth_accounts_org_user ON oauth_accounts(org_id, user_id);
CREATE INDEX idx_oauth_accounts_org_social ON oauth_accounts(org_id, social_type);
CREATE INDEX idx_oauth_accounts_org_provider ON oauth_accounts(org_id, social_type, provider_user_id);
CREATE INDEX idx_oauth_accounts_org_deleted ON oauth_accounts(org_id, is_deleted);

-- memberships 테이블
CREATE INDEX idx_memberships_user_id ON memberships(user_id);
CREATE INDEX idx_memberships_org_id ON memberships(org_id);
CREATE INDEX idx_memberships_role_id ON memberships(role_id);
CREATE INDEX idx_memberships_org_name ON memberships(org_id, name);
CREATE INDEX idx_memberships_org_nickname ON memberships(org_id, nickname);
CREATE INDEX idx_memberships_org_user ON memberships(org_id, user_id);
CREATE INDEX idx_memberships_org_deleted ON memberships(org_id, is_deleted);
CREATE INDEX idx_memberships_status ON memberships(status);

-- attendance 테이블
CREATE INDEX idx_attendance_org_dept ON attendance(org_id, department_id);
CREATE INDEX idx_attendance_org_date ON attendance(org_id, attendance_date);
CREATE INDEX idx_attendance_org_member ON attendance(org_id, member_id);
CREATE INDEX idx_attendance_org_status ON attendance(org_id, status);
CREATE INDEX idx_attendance_org_deleted ON attendance(org_id, is_deleted);

-- family_relations 테이블
CREATE INDEX idx_family_relations_org_member1 ON family_relations(org_id, member1_id);
CREATE INDEX idx_family_relations_org_member2 ON family_relations(org_id, member2_id);
CREATE INDEX idx_family_relations_org_type ON family_relations(org_id, relationship_type);
CREATE INDEX idx_family_relations_org_deleted ON family_relations(org_id, is_deleted);

-- groups 테이블
CREATE INDEX idx_groups_org_parent ON groups(org_id, parent_group_id);
CREATE INDEX idx_groups_org_leader ON groups(org_id, leader_person_id);
CREATE INDEX idx_groups_org_type ON groups(org_id, group_type);

-- group_membership 테이블
CREATE INDEX idx_group_membership_org_person ON group_membership(org_id, person_id);
CREATE INDEX idx_group_membership_org_group ON group_membership(org_id, group_id);

-- posts 테이블
CREATE INDEX idx_posts_org_id ON posts(org_id);
CREATE INDEX idx_posts_org_category ON posts(org_id, category_nm);
CREATE INDEX idx_posts_org_user ON posts(org_id, user_id);
CREATE INDEX idx_posts_org_created ON posts(org_id, created_at DESC);
CREATE INDEX idx_posts_org_view ON posts(org_id, view_count DESC);
CREATE INDEX idx_posts_org_like ON posts(org_id, like_count DESC);
CREATE INDEX idx_posts_org_deleted ON posts(org_id, is_deleted);

-- post_youtube 테이블
CREATE INDEX idx_post_youtube_org_post ON post_youtube(org_id, post_id);

-- comments 테이블
CREATE INDEX idx_comments_org_post ON comments(org_id, post_id);
CREATE INDEX idx_comments_org_user ON comments(org_id, user_id);
CREATE INDEX idx_comments_org_parent ON comments(org_id, parent_comment_id);
CREATE INDEX idx_comments_org_created ON comments(org_id, created_at);
CREATE INDEX idx_comments_org_deleted ON comments(org_id, is_deleted);

-- files 테이블
CREATE INDEX idx_files_org_entity ON files(org_id, related_entity_id, related_entity_type);
CREATE INDEX idx_files_org_uploader ON files(org_id, uploader_id);
CREATE INDEX idx_files_org_deleted ON files(org_id, is_deleted);
