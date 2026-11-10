package org.example.nongye.config;

import org.example.nongye.entity.Complaint;
import org.example.nongye.mapper.ComplaintMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 投诉 SLA 定时任务：每 5 分钟扫描一次，未回复 + SLA 已到 → 自动标记 overdue=1
 */
@Component
public class ComplaintScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(ComplaintScheduledTask.class);

    @Autowired
    private ComplaintMapper complaintMapper;

    /** 每 5 分钟扫描一次：未回复 + SLA 已到 → 自动升级 overdue=1 */
    @Scheduled(cron = "0 */5 * * * ?")
    public void autoEscalateOverdueComplaints() {
        List<Complaint> list = complaintMapper.selectPendingForEscalation();
        if (list == null || list.isEmpty()) {
            return;
        }
        for (Complaint c : list) {
            complaintMapper.markOverdue(c.getId());
            log.warn("投诉自动升级 SLA 超时：id={}, 提交时间={}, sla_hours={}, overdue=1",
                    c.getId(), c.getCreateTime(), c.getSlaHours());
        }
    }
}
