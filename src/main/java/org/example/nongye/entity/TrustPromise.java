package org.example.nongye.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TrustPromise {
    private Long id;
    private Long batchId;
    private Long farmerId;
    private String promiseType;
    private String promiseValue;
    private String evidencePath;
    /** DEFAULT 'ACTIVE' */
    private String status;
    private LocalDateTime createTime;
}
