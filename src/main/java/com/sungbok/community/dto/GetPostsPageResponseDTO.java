package com.sungbok.community.dto;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GetPostsPageResponseDTO {

    private int currentPage;

    private int currentElementCount;

    private int totalElementCount;

    private int totalPages;

    private boolean isLast;

    private List<GetPostResponseDTO> data;

    public static GetPostsPageResponseDTO of(List<GetPostResponseDTO> data, Pageable pageable, int totalCount) {
        int totalPages = (int) Math.ceil((double) totalCount / pageable.getPageSize());
        boolean isLast = (long) (pageable.getPageNumber() + 1) * pageable.getPageSize() >= totalCount;

        return GetPostsPageResponseDTO.builder()
                .currentPage(pageable.getPageNumber() + 1)
                .currentElementCount(data.size())
                .totalPages(totalPages)
                .totalElementCount(totalCount)
                .isLast(isLast)
                .data(data)
                .build();
    }

}
