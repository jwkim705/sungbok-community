CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL
);

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
CREATE TABLE BoardCategories (
    category_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE, -- 예 : 자유게시판, 유튜브 공유, SNS,
    description TEXT, -- 카테고리 설명
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

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

CREATE TABLE oauth_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGSERIAL NOT NULL REFERENCES users(id),
    socialType VARCHAR(50) NOT NULL, -- 예: google, kakao
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL
);

CREATE TABLE members ( -- 교회 성도 정보
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

CREATE TABLE user_department_roles ( -- 사용자의 부서 내 역할
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

CREATE TABLE attendance (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGSERIAL NOT NULL REFERENCES members(id),
    department_id BIGSERIAL NOT NULL REFERENCES departments(id), -- 출석한 부서
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL, -- 예: PRESENT, ABSENT, LATE, ONLINE (**어떤 상태값들이 필요할까요?**)
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

CREATE TABLE family_relations (
    id BIGSERIAL PRIMARY KEY,
    member1_id BIGSERIAL NOT NULL REFERENCES members(id),
    member2_id BIGSERIAL NOT NULL REFERENCES members(id),
    relationship_type VARCHAR(50) NOT NULL, -- 예: SPOUSE, PARENT, CHILD, SIBLING (**관계 유형 정의 필요**)
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL,
    UNIQUE (member1_id, member2_id, relationship_type)
);

-- 게시글 테이블
CREATE TABLE Posts (
    post_id BIGSERIAL PRIMARY KEY,
    category_id BIGSERIAL NOT NULL REFERENCES BoardCategories(category_id),
    user_id BIGSERIAL NOT NULL REFERENCES Users(user_id),
    title VARCHAR(255) NOT NULL, -- 게시글 제목 (길이 조정 가능)
    content TEXT, -- 게시글 내용
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 수정 시 업데이트 필요 (이름 변경됨)
);

-- 게시글-유튜브 영상 연결 테이블
CREATE TABLE PostYoutube (
    post_youtube_id BIGSERIAL PRIMARY KEY, -- 연결 테이블 고유 ID
    post_id BIGINT NOT NULL REFERENCES Posts(post_id) ON DELETE CASCADE, -- 게시글 삭제 시 함께 삭제
    youtube_video_id VARCHAR(50) NOT NULL, -- 유튜브 영상 ID
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    UNIQUE (post_id, youtube_video_id) -- 한 게시글에 동일 영상 중복 방지 (선택 사항)
);

-- 댓글 테이블
CREATE TABLE Comments (
    comment_id BIGSERIAL PRIMARY KEY,
    post_id BIGSERIAL NOT NULL REFERENCES Posts(post_id) ON DELETE CASCADE, -- 게시글 삭제 시 댓글도 삭제
    user_id BIGSERIAL NOT NULL REFERENCES Users(user_id),
    parent_comment_id BIGSERIAL REFERENCES Comments(comment_id) ON DELETE CASCADE, -- 부모 댓글 삭제 시 자식 댓글도 삭제 (옵션)
    content TEXT NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 수정 시 업데이트 필요 (이름 변경됨)
);

-- 파일 테이블
CREATE TABLE Files (
    file_id BIGSERIAL PRIMARY KEY,
    related_entity_id BIGSERIAL NOT NULL, -- 관련 엔티티 ID (예: post_id)
    related_entity_type VARCHAR(50) NOT NULL, -- 관련 엔티티 타입 (예: 'post', 'comment')
    original_filename VARCHAR(255) NOT NULL, -- 원본 파일명
    stored_filename VARCHAR(255) NOT NULL UNIQUE, -- 서버 저장 파일명 (고유해야 함)
    file_path VARCHAR(1024) NOT NULL, -- 파일 저장 경로
    file_size BIGSERIAL NOT NULL, -- 파일 크기 (Bytes)
    mime_type VARCHAR(100) NOT NULL, -- 파일 MIME 타입
    uploader_id BIGSERIAL NOT NULL REFERENCES Users(user_id),
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 수정 시 업데이트 필요 (이름 변경됨)
    modified_by BIGSERIAL
);

-- oauth_accounts 테이블
CREATE INDEX idx_oauth_accounts_user_id ON oauth_accounts(user_id);
CREATE INDEX idx_oauth_accounts_social_type ON oauth_accounts(socialType); -- 소셜 타입으로 검색 시
CREATE INDEX idx_oauth_accounts_is_deleted ON oauth_accounts(is_deleted); -- 삭제 여부로 검색 시

-- members 테이블
CREATE INDEX idx_members_name ON members(name); -- 이름으로 검색 시
CREATE INDEX idx_members_nickname ON members(nickname); -- 닉네임으로 검색 시

CREATE INDEX idx_members_is_deleted ON members(is_deleted);
CREATE INDEX idx_members_registered_by_user_id ON members(registered_by_user_id);

-- member_departments 테이블 (PK가 복합키이므로 개별 FK 인덱스는 선택적이나 추가하면 유용할 수 있음)
CREATE INDEX idx_member_departments_department_id ON member_departments(department_id); -- 부서 기준으로 멤버 찾을 때
CREATE INDEX idx_member_departments_is_deleted ON member_departments(is_deleted);

-- user_department_roles 테이블 (PK가 복합키)
CREATE INDEX idx_user_department_roles_department_id ON user_department_roles(department_id); -- 부서 기준으로 역할 찾을 때
CREATE INDEX idx_user_department_roles_role_id ON user_department_roles(role_id); -- 역할 기준으로 사용자/부서 찾을 때
CREATE INDEX idx_user_department_roles_is_deleted ON user_department_roles(is_deleted);

-- attendance 테이블 (UNIQUE 제약조건 컬럼 외 개별 인덱스)
CREATE INDEX idx_attendance_department_id ON attendance(department_id); -- 부서별 출석 조회 시
CREATE INDEX idx_attendance_attendance_date ON attendance(attendance_date); -- 날짜별 출석 조회 시
CREATE INDEX idx_attendance_status ON attendance(status); -- 출석 상태별 조회 시
CREATE INDEX idx_attendance_checked_by_user_id ON attendance(checked_by_user_id);
CREATE INDEX idx_attendance_is_deleted ON attendance(is_deleted);

-- family_relations 테이블 (UNIQUE 제약조건 컬럼 외 개별 인덱스)
CREATE INDEX idx_family_relations_member2_id ON family_relations(member2_id); -- member2 기준으로 관계 찾을 때
CREATE INDEX idx_family_relations_relationship_type ON family_relations(relationship_type); -- 관계 유형으로 검색 시
CREATE INDEX idx_family_relations_is_deleted ON family_relations(is_deleted);

-- Posts 테이블
CREATE INDEX idx_posts_category_id ON Posts(category_id);
CREATE INDEX idx_posts_user_id ON Posts(user_id);
CREATE INDEX idx_posts_created_at ON Posts(created_at); -- 정렬 및 기간 검색 시
CREATE INDEX idx_posts_is_deleted ON Posts(is_deleted);

CREATE INDEX idx_post_youtube_post_id ON PostYoutube(post_id); -- 신규 인덱스

-- Comments 테이블
CREATE INDEX idx_comments_post_id ON Comments(post_id);
CREATE INDEX idx_comments_user_id ON Comments(user_id);
CREATE INDEX idx_comments_parent_comment_id ON Comments(parent_comment_id); -- 대댓글 조회 시
CREATE INDEX idx_comments_created_at ON Comments(created_at); -- 정렬 및 기간 검색 시
CREATE INDEX idx_comments_is_deleted ON Comments(is_deleted);

-- Files 테이블
CREATE INDEX idx_files_related_entity ON Files(related_entity_id, related_entity_type); -- 복합 인덱스
CREATE INDEX idx_files_uploader_id ON Files(uploader_id);
CREATE INDEX idx_files_is_deleted ON Files(is_deleted);

-- users 테이블 (필요시)
CREATE INDEX idx_users_is_deleted ON users(is_deleted);