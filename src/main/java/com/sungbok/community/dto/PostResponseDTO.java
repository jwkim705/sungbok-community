package com.sungbok.community.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jooq.generated.tables.pojos.Files;
import org.jooq.generated.tables.pojos.PostYoutube;
import org.jooq.generated.tables.pojos.Posts;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Accessors(chain = true)
public class PostResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -5952493649912957634L;

    private final Long postId;

    private final String title;

    private final String content;

    private final String boardCategory;

    private final Long userId;

    private final int viewCnt;

    private final int likeCnt;

    private final boolean isDeleted;

    private final PostYoutube youtube;

    private final Files files;


    @Builder
    public PostResponseDTO(Posts post, String boardCategory, PostYoutube youtube, Files files) {
        this.userId = post.getUserId();
        this.postId = post.getPostId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.boardCategory = boardCategory;
        this.viewCnt = post.getViewCount();
        this.likeCnt = post.getLikeCount();
        this.isDeleted = post.getIsDeleted();
        this.youtube = youtube;
        this.files = files;
    }

    public static PostResponseDTO of(Posts post, String boardCategory, PostYoutube youtube, Files files) {
        return PostResponseDTO
                .builder()
                .post(post)
                .boardCategory(boardCategory)
                .youtube(youtube)
                .files(files)
                .build();
    }
}
