package org.example.nongye.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConsumerEvent {
    private Long id;
    private Long consumerId;
    /** SCAN / ASK / FOLLOW / SUBSCRIBE / AFTERSALE / REPURCHASE */
    private String eventType;
    private Long batchId;
    private String payload;
    private LocalDateTime createTime;
}
