package com.uday.rguktconnect.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private Long id;
    private User recipient;
    private User sender;  
    private String type; 
    private Long relatedId;  
    @Builder.Default
    private boolean isRead = false;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
