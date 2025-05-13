package com.sungbok.community.repository;

import com.sungbok.community.dto.AddPostRequestDTO;
import com.sungbok.community.dto.AddPostResponseDTO;
import com.sungbok.community.dto.GetPostResponseDTO;
import org.jooq.Condition;
import org.jooq.Configuration;
import org.jooq.SortField;
import org.jooq.generated.tables.daos.PostsDao;
import org.jooq.impl.DSL;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PostsRepository {

    private final PostsDao dao;

    public PostsRepository(Configuration configuration) {
        this.dao = new PostsDao(configuration);
    }

    public AddPostResponseDTO addPost(AddPostRequestDTO requestDTO, Long userId) {
        return null;
    }

    public Page<GetPostResponseDTO> searchMultisetBoard(Pageable pageable) {

        // 검색 조건 설정
        Condition searchCondition = createSearchCondition(boardPageRequest.getSearch());

        // 정렬 설정
        SortField<?> sortField = createSortField(boardPageRequest.getSort(), boardPageRequest.getDirection());

        // 전체 카운트 조회
        Long totalCount = dslContext
                .selectCount()
                .from(BOARDS)
                .where(searchCondition)
                .fetchOneInto(Long.class);

        // 페이지네이션 데이터 조회
        List<BoardDtoV3> boardList = dslContext
                .select(
                        BOARDS.ID,
                        BOARDS.TITLE,
                        BOARDS.CONTENT,
                        BOARDS.CREATED_AT,
                        BOARDS.UPDATED_AT,

                        // 댓글
                        DSL.multiset(
                                        dslContext.select(
                                                        COMMENTS.ID,
                                                        COMMENTS.AUTHOR,
                                                        COMMENTS.CONTENT,
                                                        COMMENTS.CREATED_AT
                                                )
                                                .from(COMMENTS)
                                                .where(COMMENTS.BOARD_ID.eq(BOARDS.ID))
                                                .orderBy(COMMENTS.CREATED_AT)
                                )
                                .convertFrom(r -> r.into(CommentDto.class))
                                .as("comments"),

                        // 첨부파일
                        DSL.multiset(
                                        dslContext.select(
                                                        ATTACHMENTS.ID,
                                                        ATTACHMENTS.FILE_URL
                                                )
                                                .from(ATTACHMENTS)
                                                .where(ATTACHMENTS.BOARD_ID.eq(BOARDS.ID))
                                                .orderBy(ATTACHMENTS.ID.desc())
                                )
                                .convertFrom(r -> r.into(AttachmentDto.class))
                                .as("attachments")
                )
                .from(BOARDS)
                .where(searchCondition)
                .orderBy(sortField)
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .fetchInto(BoardDtoV3.class);

        return BoardPageResponseV3.of(boardList, pageable, totalCount);
    }

    private Condition createSearchCondition(String search) {
        return StringUtils.hasText(search)
                ? BOARDS.TITLE.containsIgnoreCase(search)
                .or(BOARDS.CONTENT.containsIgnoreCase(search))
                : DSL.trueCondition();
    }

    private SortField<?> createSortField(String sort, String direction) {
        Field<?> field = switch (sort) {
            case "title" -> BOARDS.TITLE;
            case "content" -> BOARDS.CONTENT;
            default -> BOARDS.CREATED_AT;
        };

        return direction.equals("asc") ? field.asc() : field.desc();
    }

}