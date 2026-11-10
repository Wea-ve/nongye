package org.example.nongye.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConsumerFollow {
    private Long id;
    private Long consumerId;
    /** FARMER / BATCH / CROP */
    private String targetType;
    private Long targetId;
    /** DEFAULT 1 */
    private Integer notifyEnabled;
    private LocalDateTime createTime;
}
