package com.sungbok.community.repository;

import static com.sungbok.community.repository.util.JooqTenantConditionUtils.appIdCondition;
import static org.jooq.generated.Tables.COMMENTS;
import static org.jooq.generated.Tables.FILES;
import static org.jooq.generated.Tables.POSTS;
import static org.jooq.generated.Tables.POST_YOUTUBE;
import static org.jooq.impl.DSL.multiset;

import com.sungbok.community.dto.GetPostResponseDTO;
import com.sungbok.community.dto.GetPostsPageResponseDTO;
import com.sungbok.community.dto.PostSearchVO;
import com.sungbok.community.security.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import org.jooq.Condition;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SortField;
import org.jooq.generated.tables.daos.PostsDao;
import org.jooq.generated.tables.pojos.Posts;
import org.jooq.impl.DSL;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 게시글 데이터 접근 Repository
 * 하이브리드 DAO + DSL 패턴 사용
 * MULTISET을 사용한 중첩 컬렉션 조회 패턴 적용
 */
@Repository
public class PostsRepository {

    private final DSLContext dsl;
    private final PostsDao dao;

    public PostsRepository(DSLContext dsl, Configuration configuration) {
        this.dsl = dsl;
        this.dao = new PostsDao(configuration);
    }

    public GetPostsPageResponseDTO fetchAllPosts(PostSearchVO searchVO) {

        Pageable pageable = searchVO.toPageable();

        // 검색 조건 설정 (app_id 자동 필터링 포함)
        Condition searchCondition = createSearchCondition(searchVO.getSearch());

        // 카테고리 필터링
        if (StringUtils.hasText(searchVO.getCategory())) {
            searchCondition = searchCondition.and(POSTS.CATEGORY_NM.eq(searchVO.getCategory().toUpperCase()));
        }

        // 정렬 설정
        SortField<?> sortField = createSortField(searchVO.getSort(), searchVO.getDirection());

        // 전체 게시글 수 조회 (app_id 자동 필터링)
        int totalCount = dsl.fetchCount(POSTS, searchCondition);

        // 게시글 조회 (댓글, 파일, 유튜브 멀티셋 포함)
        List<GetPostResponseDTO> postList =
            dsl.select(
                    POSTS.APP_ID,
                    POSTS.POST_ID,
                    POSTS.TITLE,
                    POSTS.CONTENT,
                    POSTS.CATEGORY_NM,
                    POSTS.USER_ID,
                    POSTS.EMAIL,
                    POSTS.USER_NM,
                    POSTS.VIEW_COUNT,
                    POSTS.LIKE_COUNT,
                    POSTS.IS_DELETED,
                    POSTS.CREATED_AT,
                    POSTS.CREATED_BY,
                    POSTS.MODIFIED_AT,
                    POSTS.MODIFIED_BY,
                    // 파일 멀티셋
                    multiset(
                        dsl.select(
                            FILES.FILE_ID
                            ,FILES.RELATED_ENTITY_ID
                            ,FILES.RELATED_ENTITY_TYPE
                            ,FILES.ORIGINAL_FILENAME
                            ,FILES.STORED_FILENAME
                            ,FILES.FILE_PATH
                            ,FILES.FILE_SIZE
                            ,FILES.MIME_TYPE
                            ,FILES.UPLOADER_ID
                            ,FILES.IS_DELETED
                            ,FILES.CREATED_AT
                            ,FILES.CREATED_BY
                            ,FILES.MODIFIED_AT
                            ,FILES.MODIFIED_BY
                        )
                        .from(FILES)
                        .where(FILES.APP_ID.eq(POSTS.APP_ID))
                        .and(FILES.RELATED_ENTITY_ID.eq(POSTS.POST_ID))
                        .and(FILES.RELATED_ENTITY_TYPE.eq("post"))
                        .and(FILES.IS_DELETED.eq(false))
                    ).as("files"),
                    // 유튜브 멀티셋
                    multiset(
                        dsl.select(
                            POST_YOUTUBE.POST_YOUTUBE_ID
                            ,POST_YOUTUBE.YOUTUBE_VIDEO_ID
                            ,POST_YOUTUBE.IS_DELETED
                            ,POST_YOUTUBE.CREATED_AT
                            ,POST_YOUTUBE.CREATED_BY
                            ,POST_YOUTUBE.MODIFIED_AT
                            ,POST_YOUTUBE.MODIFIED_BY
                        )
                        .from(POST_YOUTUBE)
                        .where(POST_YOUTUBE.APP_ID.eq(POSTS.APP_ID))
                        .and(POST_YOUTUBE.POST_ID.eq(POSTS.POST_ID))
                        .and(POST_YOUTUBE.IS_DELETED.eq(false))
                    ).as("youtube")
        )
        .from(POSTS)
        .where(searchCondition)
        .orderBy(sortField)
        .limit(pageable.getPageSize())
        .offset(pageable.getOffset())
        .fetchInto(GetPostResponseDTO.class);

        return GetPostsPageResponseDTO.of(postList, pageable, totalCount);
    }

    public GetPostResponseDTO fetchPostById(Long postId) {
        return
            dsl.select(
                    POSTS.APP_ID,
                    POSTS.POST_ID,
                    POSTS.TITLE,
                    POSTS.CONTENT,
                    POSTS.CATEGORY_NM,
                    POSTS.USER_ID,
                    POSTS.EMAIL,
                    POSTS.USER_NM,
                    POSTS.VIEW_COUNT,
                    POSTS.LIKE_COUNT,
                    POSTS.IS_DELETED,
                    POSTS.CREATED_AT,
                    POSTS.CREATED_BY,
                    POSTS.MODIFIED_AT,
                    POSTS.MODIFIED_BY,
                    // 댓글 멀티셋
                    multiset(
                        dsl.select(
                            COMMENTS.COMMENT_ID
                            ,COMMENTS.POST_ID
                            ,COMMENTS.USER_ID
                            ,COMMENTS.USER_NM
                            ,COMMENTS.CONTENT
                            ,COMMENTS.PARENT_COMMENT_ID
                            ,COMMENTS.IS_DELETED
                            ,COMMENTS.CREATED_AT
                            ,COMMENTS.CREATED_BY
                            ,COMMENTS.MODIFIED_AT
                            ,COMMENTS.MODIFIED_BY
                        ).from(COMMENTS)
                        .where(COMMENTS.APP_ID.eq(POSTS.APP_ID))
                        .and(COMMENTS.POST_ID.eq(POSTS.POST_ID))
                        .and(COMMENTS.IS_DELETED.eq(false))
                        .orderBy(COMMENTS.CREATED_AT.asc())
                    ).as("comments"),
                    // 파일 멀티셋
                    multiset(
                        dsl.select(
                            FILES.FILE_ID
                            ,FILES.RELATED_ENTITY_ID
                            ,FILES.RELATED_ENTITY_TYPE
                            ,FILES.ORIGINAL_FILENAME
                            ,FILES.STORED_FILENAME
                            ,FILES.FILE_PATH
                            ,FILES.FILE_SIZE
                            ,FILES.MIME_TYPE
                            ,FILES.UPLOADER_ID
                            ,FILES.IS_DELETED
                            ,FILES.CREATED_AT
                            ,FILES.CREATED_BY
                            ,FILES.MODIFIED_AT
                            ,FILES.MODIFIED_BY
                        )
                        .from(FILES)
                        .where(FILES.APP_ID.eq(POSTS.APP_ID))
                        .and(FILES.RELATED_ENTITY_ID.eq(POSTS.POST_ID))
                        .and(FILES.RELATED_ENTITY_TYPE.eq("post"))
                        .and(FILES.IS_DELETED.eq(false))
                    ).as("files"),
                    // 유튜브 멀티셋
                    multiset(
                        dsl.select(
                            POST_YOUTUBE.POST_YOUTUBE_ID
                            ,POST_YOUTUBE.YOUTUBE_VIDEO_ID
                            ,POST_YOUTUBE.IS_DELETED
                            ,POST_YOUTUBE.CREATED_AT
                            ,POST_YOUTUBE.CREATED_BY
                            ,POST_YOUTUBE.MODIFIED_AT
                            ,POST_YOUTUBE.MODIFIED_BY
                        )
                        .from(POST_YOUTUBE)
                        .where(POST_YOUTUBE.APP_ID.eq(POSTS.APP_ID))
                        .and(POST_YOUTUBE.POST_ID.eq(POSTS.POST_ID))
                        .and(POST_YOUTUBE.IS_DELETED.eq(false))
                    ).as("youtube")
        )
        .from(POSTS)
        .where(appIdCondition(POSTS.APP_ID))
        .and(POSTS.POST_ID.eq(postId))
        .and(POSTS.IS_DELETED.eq(false))
        .fetchOneInto(GetPostResponseDTO.class);
    }

    /**
     * RETURNING 절로 새 게시글을 삽입합니다.
     * app_id는 TenantContext에서 자동 설정
     *
     * @param post 삽입할 게시글 엔티티
     * @return 생성된 ID가 포함된 삽입된 게시글
     */
    public Posts insert(Posts post) {
        // TenantContext에서 app_id 가져오기
        Long appId = TenantContext.getRequiredAppId();
        post.setAppId(appId);  // 강제로 현재 테넌트 설정

        return dsl.insertInto(POSTS)
                .set(dsl.newRecord(POSTS, post))
                .returning()
                .fetchOneInto(Posts.class);
    }

    /**
     * DSLContext.update() 패턴으로 게시글을 업데이트합니다.
     * 지정된 필드만 업데이트 (app_id로 격리)
     *
     * @param post 업데이트할 값이 포함된 게시글 엔티티
     * @return 영향받은 행 수
     */
    public int update(Posts post) {
        return dsl.update(POSTS)
                .set(POSTS.TITLE, post.getTitle())
                .set(POSTS.CONTENT, post.getContent())
                .set(POSTS.CATEGORY_NM, post.getCategoryNm())
                .set(POSTS.MODIFIED_AT, LocalDateTime.now())
                .set(POSTS.MODIFIED_BY, post.getModifiedBy())
                .where(appIdCondition(POSTS.APP_ID))
                .and(POSTS.POST_ID.eq(post.getPostId()))
                .and(POSTS.IS_DELETED.eq(false))
                .execute();
    }

    /**
     * DSLContext.update() 패턴으로 게시글을 소프트 삭제합니다.
     * app_id로 격리
     *
     * @param postId 삭제할 게시글 ID
     * @param userId 삭제를 수행하는 사용자 ID
     * @return 영향받은 행 수
     */
    public int softDelete(Long postId, Long userId) {
        return dsl.update(POSTS)
                .set(POSTS.IS_DELETED, true)
                .set(POSTS.MODIFIED_AT, LocalDateTime.now())
                .set(POSTS.MODIFIED_BY, userId)
                .where(appIdCondition(POSTS.APP_ID))
                .and(POSTS.POST_ID.eq(postId))
                .execute();
    }

    /**
     * 조회수를 원자적으로 증가시킵니다.
     * DSLContext.update()를 사용한 단일 필드 업데이트 (app_id로 격리)
     *
     * @param postId 게시글 ID
     * @return 영향받은 행 수
     */
    public int incrementViewCount(Long postId) {
        return dsl.update(POSTS)
                .set(POSTS.VIEW_COUNT, POSTS.VIEW_COUNT.add(1))
                .where(appIdCondition(POSTS.APP_ID))
                .and(POSTS.POST_ID.eq(postId))
                .and(POSTS.IS_DELETED.eq(false))
                .execute();
    }

    /**
     * 게시글이 사용자 소유인지 확인합니다.
     * app_id로 격리
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 게시글이 사용자 소유이면 true
     */
    public boolean isPostOwnedByUser(Long postId, Long userId) {
        return dsl.fetchExists(
                POSTS,
                appIdCondition(POSTS.APP_ID)
                        .and(POSTS.POST_ID.eq(postId))
                        .and(POSTS.USER_ID.eq(userId))
                        .and(POSTS.IS_DELETED.eq(false))
        );
    }

    private Condition createSearchCondition(String search) {
        // 기본 조건: app_id 필터링 + 소프트 삭제되지 않은 게시글
        Condition baseCondition = appIdCondition(POSTS.APP_ID)
                .and(POSTS.IS_DELETED.eq(false));

        // 검색어가 있으면 제목 또는 내용에서 검색
        if (StringUtils.hasText(search)) {
            return baseCondition.and(
                    POSTS.TITLE.containsIgnoreCase(search)
                            .or(POSTS.CONTENT.containsIgnoreCase(search))
            );
        }

        return baseCondition;
    }

    private SortField<?> createSortField(String sort, String direction) {

        Field<?> field = switch (sort) {
            case "postId" -> POSTS.POST_ID;
            case "title" -> POSTS.TITLE;
            case "content" -> POSTS.CONTENT;
            case "createdAt" -> POSTS.CREATED_AT;
            case "viewCount" -> POSTS.VIEW_COUNT;
            case "likeCount" -> POSTS.LIKE_COUNT;
            default -> POSTS.CREATED_AT;
        };

        return direction.equalsIgnoreCase("ASC") ? field.asc() : field.desc();
    }

}