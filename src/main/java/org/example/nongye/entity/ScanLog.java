package org.example.nongye.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ScanLog {
    private Long id;
    private Long batchId;
    private Long consumerId;
    private String scene;
    private String traceCode;
    private String ip;
    private String userAgent;
    private LocalDateTime scanTime;
    // 联表字段
    private String cropName;
    private String batchNo;
}
