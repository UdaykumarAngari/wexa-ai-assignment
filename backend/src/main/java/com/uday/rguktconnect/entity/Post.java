package com.uday.rguktconnect.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Post {
    private Long id;
    private User author;
    private String type; 
    private String content;
    private String codeSnippet;
    private String company;
    private String role;
    private String mediaUrl;
    private LocalDateTime createdAt = LocalDateTime.now();
    private Set<User> likedBy = new HashSet<>();
    private Set<Comment> comments = new HashSet<>();
}
