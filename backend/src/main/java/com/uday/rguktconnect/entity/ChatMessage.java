package com.uday.rguktconnect.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    private Long id;
    private User sender;
    private User receiver;
    private String content;
    private boolean isRead = false;
    private LocalDateTime timestamp = LocalDateTime.now();
}