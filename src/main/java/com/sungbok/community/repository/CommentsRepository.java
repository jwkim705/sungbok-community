package com.sungbok.community.repository;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.CommentsDao;
import org.jooq.generated.tables.pojos.Comments;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.jooq.generated.Tables.COMMENTS;
import static com.sungbok.community.repository.util.JooqTenantConditionUtils.orgIdCondition;

/**
 * Comments (댓글) 데이터 접근 Repository
 * 하이브리드 DAO + DSL 패턴 사용
 *
 * @since 0.0.1
 */
@Repository
public class CommentsRepository {

    private final DSLContext dsl;
    private final CommentsDao dao;

    public CommentsRepository(DSLContext dsl, Configuration configuration) {
        this.dsl = dsl;
        this.dao = new CommentsDao(configuration);
    }

    /**
     * 게시글의 모든 댓글 조회 (org_id 필터링 적용)
     *
     * @param postId 게시글 ID
     * @return 댓글 리스트
     */
    public List<Comments> fetchByPostId(Long postId) {
        return dsl.selectFrom(COMMENTS)
                .where(orgIdCondition(COMMENTS.ORG_ID))
                .and(COMMENTS.POST_ID.eq(postId))
                .and(COMMENTS.IS_DELETED.eq(false))
                .orderBy(COMMENTS.CREATED_AT.asc())
                .fetchInto(Comments.class);
    }
}
