package com.sungbok.community.dto;

import com.sungbok.community.common.vo.CommonVO;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FilesDTO extends CommonVO {

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


}
