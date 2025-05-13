package com.sungbok.community.dto;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FilesDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -7971881140782238236L;

    private Long fileId;

    private Long relatedEntityId;

    private String relatedEntityType;

    private String originalFilename;

    private String storedFilename;

    private String filePath;

    private Long fileSize;

    private String mimeType;

    private Long uploaderId;

    private boolean isDeleted;

    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime modifiedAt;
    private Long modifiedBy;


}
