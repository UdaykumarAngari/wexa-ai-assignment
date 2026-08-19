package com.uday.rguktconnect.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDetails {
    private Long id;
    private User user;
    private String mobileNumber;
    private String personalEmail;
    private String branch;
    private String batch;
    private String profilePhoto; 
    private String description;
    private String githubUrl;
    private String linkedinUrl;
    private Integer mentoredStudentsCount = 0;
    private LocalDateTime updatedAt = LocalDateTime.now();
}