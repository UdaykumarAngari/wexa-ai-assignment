package com.uday.rguktconnect.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Job {
    private Long id;
    private String company;
    private String role;
    private String location;
    private String salary;
    private String applyUrl;
    private String type; // e.g. "Full-time", "Internship"
    private LocalDate expiresAt;
    private User postedBy;
    private boolean referralAvailable;
    private String category;
    private LocalDateTime createdAt = LocalDateTime.now();
}
