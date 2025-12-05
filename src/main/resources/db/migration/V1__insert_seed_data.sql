-- ============================================
-- INSERT DATA (샘플 데이터)
-- ============================================

-- 0. Apps (테넌트) - 기본 앱
INSERT INTO apps (app_id, app_name, app_key, status, created_by, modified_by)
VALUES (1, 'Default Church', 'default-church', 'ACTIVE', 1, 1)
ON CONFLICT (app_id) DO NOTHING;

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
INSERT INTO users (app_id, email, password, created_by, modified_by)
VALUES
    (1, 'admin@example.com', '$2a$10$abcdefghijklmnopqrstuv', 1, 1), -- 관리자/리더
    (1, 'member1@example.com', '$2a$10$wxyzabcdefghijklmnopq', 1, 1), -- 성도1
    (1, 'member2@example.com', '$2a$10$1234567890abcdefghijkl', 1, 1), -- 성도2
    (1, 'leader2@example.com', '$2a$10$zyxwutsrqponmlkjihgfe', 1, 1),  -- 다른 리더
    (1, 'children@example.com', '$2a$10$zyxwutsrqponmlkjihgfe', 1, 1)  -- 아이
ON CONFLICT (id) DO NOTHING;

-- 5. OAuth 계정 (oauth_accounts) - 사용자 1, 2에 연결
INSERT INTO oauth_accounts (app_id, user_id, social_type, created_by, modified_by)
VALUES
    (1, 1, 'google', 1, 1),
    (1, 2, 'kakao', 1, 1)
ON CONFLICT (id) DO NOTHING; -- Assumes id is SERIAL/BIGSERIAL

-- 6. 성도 정보 (members) - 사용자들에 연결
INSERT INTO members (app_id, user_id, name, birthdate, gender, address, phone_number, nickname, registered_by_user_id, created_by, modified_by)
VALUES
    (1, 1, '홍길동', '1995-03-15', 'MALE', '서울시 강남구', '010-1234-5678', '쾌도홍', 1, 1, 1),
    (1, 2, '성춘향', '1996-08-20', 'FEMALE', '서울시 서초구', '010-9876-5432', '춘향사랑', 1, 1, 1),
    (1, 3, '이몽룡', '1994-11-10', 'MALE', '서울시 송파구', '010-1111-2222', '몽룡이', 1, 1, 1),
    (1, 4, '김관리', '1990-01-01', 'MALE', '서울시 마포구', '010-5555-6666', '관리자킴', 1, 1, 1),
    (1, 5, '김아이', '2025-01-01', 'MALE', '서울시 노원구', '010-6666-7777', '김칠드런', 1, 1, 1)
ON CONFLICT (id) DO NOTHING;

-- 9. 출석 정보 (attendance) - 업데이트된 부서 ID 사용
INSERT INTO attendance (app_id, member_id, department_id, attendance_date, status, check_in_time, checked_by_user_id, method, created_by, modified_by)
VALUES
    (1, 1, 8, '2025-04-27', 'PRESENT', '2025-04-27 10:05:00', 1, 'LEADER', 1, 1), -- 홍길동, 청년부(8), 4/27 출석
    (1, 2, 8, '2025-04-27', 'ABSENT', NULL, 1, 'LEADER', 1, 1),                 -- 성춘향, 청년부(8), 4/27 결석
    (1, 1, 8, '2025-04-20', 'ONLINE', '2025-04-20 10:15:00', 1, 'SELF', 1, 1), -- 홍길동, 청년부(8), 4/20 온라인(본인체크)
    (1, 3, 10, '2025-04-27', 'LATE', '2025-04-27 11:30:00', 4, 'LEADER', 1, 1)   -- 이몽룡, 장년부(10), 4/27 지각 (김관리 체크)
ON CONFLICT (app_id, member_id, department_id, attendance_date) DO NOTHING;

-- 10. 가족 관계 (family_relations)
INSERT INTO family_relations (app_id, member1_id, member2_id, relationship_type, created_by, modified_by)
VALUES
    (1, 1, 2, 'SPOUSE', 1, 1), -- 홍길동 - 성춘향 (배우자)
    (1, 1, 3, 'SIBLING', 1, 1) -- 홍길동 - 이몽룡 (형제자매 - 예시)
ON CONFLICT (app_id, member1_id, member2_id, relationship_type) DO NOTHING;

-- 11. 게시글 (posts) - 여러 개 작성
INSERT INTO posts (app_id, table_type, category_nm, user_id, user_nm, email, title, content, created_by, modified_by)
VALUES
    (1, 'COMMUNITY', 'FREE', 1, '홍길동','admin@example.com','첫 번째 자유게시글', '자유게시판 내용입니다.', 1, 1),
    (1, 'HOMEPAGE', 'NOTICE', 4, '김관리','leader2@example.com','교회 대청소 안내', '5월 첫째 주 토요일 대청소를 진행합니다.', 4, 4),
    (1, 'COMMUNITY', 'COMMUNITY_FEED', 1, '홍길동','admin@example.com','피드 게시판 공유', '피드 게시판 첫 글입니다.', 1, 1)
ON CONFLICT (post_id) DO NOTHING;

-- 12. 게시글-유튜브 (post_youtube) - 게시글 3에 연결
INSERT INTO post_youtube (app_id, post_id, youtube_video_id, created_by, modified_by)
VALUES (1, 3, 'abcdef12345', 1, 1) -- Example YouTube video ID
ON CONFLICT (app_id, post_id, youtube_video_id) DO NOTHING;

-- 13. 댓글 (comments) - 여러 댓글, 대댓글 포함
INSERT INTO comments (app_id, post_id, user_id, user_nm, email, content, created_by, modified_by)
VALUES
    (1, 1, 2, '성춘향', 'member1@example.com','좋은 글 감사합니다!', 2, 2),         -- 성춘향 -> 게시글 1 댓글
    (1, 2, 3, '이몽룡','member2@example.com','확인했습니다. 참석하겠습니다.', 3, 3), -- 이몽룡 -> 게시글 2 댓글
    (1, 1, 3, '이몽룡', 'member2@example.com','저도 잘 읽었습니다.', 3, 3)          -- 이몽룡 -> 게시글 1 댓글
ON CONFLICT (comment_id) DO NOTHING;

-- 대댓글 예시 (댓글 1에 대한 대댓글)
INSERT INTO comments (app_id, post_id, user_id, user_nm, email, parent_comment_id, content, created_by, modified_by)
VALUES (1, 1, 1, '홍길동','admin@example.com',1,'읽어주셔서 감사합니다!', 1, 1) -- 홍길동 -> 댓글 1 (성춘향)에 대한 대댓글
ON CONFLICT (comment_id) DO NOTHING;

-- 14. 파일 (files) - 게시글 2 (공지사항)에 파일 첨부
INSERT INTO files (app_id, related_entity_id, related_entity_type, original_filename, stored_filename, file_path, file_size, mime_type, uploader_id, created_by, modified_by)
VALUES (1, 2, 'post', '청소구역안내.pdf', 'unique_stored_cleaning_guide.pdf', '/uploads/files', 204800, 'application/pdf', 4, 4, 4)
ON CONFLICT (file_id) DO NOTHING;
