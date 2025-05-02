package com.sungbok.community.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRoleInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 8599077715557683229L;

    private Long departmentId;

    private String departmentName;

    private Long roleId;

    private String roleName;

    private LocalDate assignmentDate;

}
