package org.example.nongye.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ConsumerSubscription {
    private Long id;
    private Long consumerId;
    private Long batchId;
    private String cropName;
    private BigDecimal qty;
    /** DEFAULT '斤' */
    private String unit;
    /** DEFAULT 7 */
    private Integer cycleDays;
    private LocalDate nextDate;
    /** DEFAULT 'ACTIVE' */
    private String status;
    private LocalDateTime createTime;
}
