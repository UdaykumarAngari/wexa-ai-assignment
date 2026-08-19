package com.uday.rguktconnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EducationDetail {
    private Long id;
    @JsonIgnore
    private User user;
    private String institutionName;
    private String degree;
    private String fieldOfStudy;
    private String startYear;
    private String endYear;
    private String grade;
}