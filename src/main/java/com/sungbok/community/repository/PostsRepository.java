package com.sungbok.community.repository;

import static org.jooq.generated.Tables.COMMENTS;
import static org.jooq.generated.Tables.FILES;
import static org.jooq.generated.Tables.POSTS;
import static org.jooq.generated.Tables.POST_YOUTUBE;
import static org.jooq.impl.DSL.multiset;

import com.sungbok.community.dto.GetPostResponseDTO;
import com.sungbok.community.dto.GetPostsPageResponseDTO;
import com.sungbok.community.dto.PostSearchVO;
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

@Repository
public class PostsRepository {

    private final DSLContext dslContext;
    private final PostsDao postsDao;

    public PostsRepository(Configuration configuration, DSLContext dslContext) {
        this.dslContext = dslContext;
        this.postsDao = new PostsDao(configuration);
    }

    public GetPostsPageResponseDTO findAllPosts(PostSearchVO searchVO) {

        Pageable pageable = searchVO.toPageable();

        // 검색 조건 설정
        Condition searchCondition = createSearchCondition(searchVO.getSearch());

        // 카테고리 필터링
        if (StringUtils.hasText(searchVO.getCategory())) {
            searchCondition = searchCondition.and(POSTS.CATEGORY_NM.eq(searchVO.getCategory().toUpperCase()));
        }

        // 정렬 설정
        SortField<?> sortField = createSortField(searchVO.getSort(), searchVO.getDirection());

        // 전체 게시글 수 조회
        int totalCount = dslContext.fetchCount(POSTS, searchCondition);

        // 게시글 조회 (댓글, 파일, 유튜브 멀티셋 포함)
        List<GetPostResponseDTO> postList =
            dslContext.select(
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
                        dslContext.select(
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
                        .where(FILES.RELATED_ENTITY_ID.eq(POSTS.POST_ID))
                        .and(FILES.RELATED_ENTITY_TYPE.eq("post"))
                        .and(FILES.IS_DELETED.eq(false))
                    ).as("files"),
                    // 유튜브 멀티셋
                    multiset(
                        dslContext.select(
                            POST_YOUTUBE.POST_YOUTUBE_ID
                            ,POST_YOUTUBE.YOUTUBE_VIDEO_ID
                            ,POST_YOUTUBE.IS_DELETED
                            ,POST_YOUTUBE.CREATED_AT
                            ,POST_YOUTUBE.CREATED_BY
                            ,POST_YOUTUBE.MODIFIED_AT
                            ,POST_YOUTUBE.MODIFIED_BY
                        )
                        .from(POST_YOUTUBE)
                        .where(POST_YOUTUBE.POST_ID.eq(POSTS.POST_ID))
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

    public GetPostResponseDTO findPostById(Long postId) {
        return
            dslContext.select(
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
                        dslContext.select(
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
                        .where(COMMENTS.POST_ID.eq(POSTS.POST_ID))
                        .and(COMMENTS.IS_DELETED.eq(false))
                        .orderBy(COMMENTS.CREATED_AT.asc())
                    ).as("comments"),
                    // 파일 멀티셋
                    multiset(
                        dslContext.select(
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
                        .where(FILES.RELATED_ENTITY_ID.eq(POSTS.POST_ID))
                        .and(FILES.RELATED_ENTITY_TYPE.eq("post"))
                        .and(FILES.IS_DELETED.eq(false))
                    ).as("files"),
                    // 유튜브 멀티셋
                    multiset(
                        dslContext.select(
                            POST_YOUTUBE.POST_YOUTUBE_ID
                            ,POST_YOUTUBE.YOUTUBE_VIDEO_ID
                            ,POST_YOUTUBE.IS_DELETED
                            ,POST_YOUTUBE.CREATED_AT
                            ,POST_YOUTUBE.CREATED_BY
                            ,POST_YOUTUBE.MODIFIED_AT
                            ,POST_YOUTUBE.MODIFIED_BY
                        )
                        .from(POST_YOUTUBE)
                        .where(POST_YOUTUBE.POST_ID.eq(POSTS.POST_ID))
                        .and(POST_YOUTUBE.IS_DELETED.eq(false))
                    ).as("youtube")
        )
        .from(POSTS)
        .where(POSTS.POST_ID.eq(postId))
        .and(POSTS.IS_DELETED.eq(false))
        .fetchOneInto(GetPostResponseDTO.class);
    }

    public Posts savePost(Posts post) {
        // 게시글의 기본 값들 설정
        if (post.getPostId() == null) {
            post.setViewCount(0);
            post.setLikeCount(0);
            post.setIsDeleted(false);
            post.setCreatedAt(LocalDateTime.now());
            post.setModifiedAt(LocalDateTime.now());
        }

        // JOOQ DAO를 사용하여 게시글 저장
        if (post.getPostId() == null) {
            postsDao.insert(post);
        } else {
            postsDao.update(post);
        }

        return post;
    }

    public boolean updatePost(Posts post) {
        // 기존 게시글 조회
        Posts existingPost = postsDao.findById(post.getPostId());
        if (existingPost == null || existingPost.getIsDeleted()) {
            return false;
        }

        // 변경 필드만 업데이트
        existingPost.setTitle(post.getTitle());
        existingPost.setContent(post.getContent());
        existingPost.setCategoryNm(post.getCategoryNm());
        existingPost.setModifiedAt(LocalDateTime.now());
        existingPost.setModifiedBy(post.getModifiedBy());

        postsDao.update(existingPost);

        return true;
    }

    public boolean deletePost(Long postId, Long userId) {
        // 기존 게시글 조회
        Posts existingPost = postsDao.findById(postId);
        if (existingPost == null || existingPost.getIsDeleted()) {
            return false;
        }

        existingPost.setIsDeleted(true);
        existingPost.setModifiedAt(LocalDateTime.now());
        existingPost.setModifiedBy(userId);

        postsDao.update(existingPost);
        return true;
    }

    public boolean increaseViewCount(Long postId) {
        int updated = dslContext.update(POSTS)
                .set(POSTS.VIEW_COUNT, POSTS.VIEW_COUNT.add(1))
                .where(POSTS.POST_ID.eq(postId))
                .and(POSTS.IS_DELETED.eq(false))
                .execute();

        return updated > 0;
    }

    public boolean isPostOwnedByUser(Long postId, Long userId) {

        Condition condition = POSTS.POST_ID.eq(postId);
        condition.and(POSTS.USER_ID.eq(userId));
        condition.and(POSTS.IS_DELETED.eq(false));

        return dslContext.fetchCount(POSTS,condition) > 0;
    }

    private Condition createSearchCondition(String search) {
        return StringUtils.hasText(search)
                ? POSTS.TITLE.containsIgnoreCase(search)
                .or(POSTS.CONTENT.containsIgnoreCase(search))
                : DSL.trueCondition();
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