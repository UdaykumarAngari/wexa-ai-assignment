package com.uday.rguktconnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Project {
    private Long id;
    @JsonIgnore
    private User user;
    private String title; 
    private String description;
    private String projectUrl; 
    private String repoUrl; 
}