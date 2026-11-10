package org.example.nongye.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FulfillmentRecord {
    private Long id;
    private Long batchId;
    private Long dealerId;
    private Long consumerId;
    private LocalDateTime shipTime;
    private LocalDateTime arriveTime;
    /** DEFAULT 'OK' */
    private String qualityFlag;
    private BigDecimal aftersaleHours;
    private LocalDateTime createTime;
}
