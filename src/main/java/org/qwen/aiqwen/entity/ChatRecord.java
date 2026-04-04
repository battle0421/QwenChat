
package org.qwen.aiqwen.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "chat_records")
@Data
public class ChatRecord extends BaseEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(length = 500)
    private String model;

    @Column(nullable = false, length = 2000)
    private String response;

    @Column(length = 50)
    private String status;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "role", nullable = false, length = 16)
    private String role;


}