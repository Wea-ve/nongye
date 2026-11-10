package org.example.nongye.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Complaint {
    private Long id;
    private Long batchId;
    private Long consumerId;
    private String complainant;
    private String phone;
    /** 消费者查询/确认凭证（提交时生成，不暴露数据库主键） */
    private String queryCode;
    private String type;
    private String content;
    private String images;
    private Integer slaHours;
    private LocalDateTime firstReplyTime;
    private Integer overdue;
    private String status;    // PENDING/PROCESSING/CLOSED
    private String reply;     // 管理员处理方案
    private String consumerReply;    // 消费者确认回复
    private LocalDateTime consumerReplyTime;
    private LocalDateTime createTime;
    private LocalDateTime replyTime;

    // 联表
    private String cropName;
    private String batchNo;
}
