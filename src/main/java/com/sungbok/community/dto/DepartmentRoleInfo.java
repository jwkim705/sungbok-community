package com.sungbok.community.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRoleInfo {

    private Long departmentId;

    private String departmentName;

    private Long roleId;

    private String roleName;

    private LocalDate assignmentDate;

}
