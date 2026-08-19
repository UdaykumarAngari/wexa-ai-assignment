package com.uday.rguktconnect.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    private Long id;
    private Post post;
    private User author;
    private String content;
    private Comment parentComment;
    private List<Comment> replies = new ArrayList<>();
    private Set<User> likedBy = new HashSet<>();
    private LocalDateTime createdAt = LocalDateTime.now();
}
