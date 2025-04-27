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

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255), -- Form login 용. OAuth 사용자는 null일 수 있음
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL
);

CREATE TABLE oauth_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGSERIAL NOT NULL REFERENCES users(id),
    socialType VARCHAR(50) NOT NULL, -- 예: google, kakao
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL,
    PRIMARY KEY (member_id, department_id)
);

CREATE TABLE user_department_roles ( -- 사용자의 부서 내 역할
   user_id BIGSERIAL NOT NULL REFERENCES users(id),
   department_id BIGSERIAL NOT NULL REFERENCES departments(id),
   role_id BIGSERIAL NOT NULL REFERENCES roles(id),
   assignment_date DATE DEFAULT CURRENT_DATE,
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGSERIAL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGSERIAL,
    UNIQUE (member1_id, member2_id, relationship_type)
);