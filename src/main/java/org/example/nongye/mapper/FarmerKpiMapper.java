package org.example.nongye.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 农户信任经营 KPI（只读聚合查询）
 * - 信任者数：关注该农户或其批次的消费者去重数
 * - 复购率：在本农户批次上有 2 次以上订阅的消费者占比
 * - 闭环时长：本农户批次投诉的平均首次回复耗时（小时）
 */
@Mapper
public interface FarmerKpiMapper {

    @Select("SELECT COUNT(DISTINCT cf.consumer_id) FROM consumer_follow cf " +
            "WHERE (cf.target_type = 'FARMER' AND cf.target_id = #{farmerId}) " +
            "   OR (cf.target_type = 'BATCH' AND cf.target_id IN " +
            "       (SELECT id FROM crop_batch WHERE farmer_id = #{farmerId} AND COALESCE(deleted, 0) = 0))")
    int countFollowers(@Param("farmerId") Long farmerId);

    @Select("SELECT " +
            "  COALESCE(SUM(CASE WHEN cnt >= 2 THEN 1 ELSE 0 END), 0) AS repeatCnt, " +
            "  COUNT(*) AS totalCnt " +
            "FROM (SELECT consumer_id, COUNT(*) AS cnt FROM consumer_subscription " +
            "      WHERE batch_id IN (SELECT id FROM crop_batch WHERE farmer_id = #{farmerId} " +
            "                         AND COALESCE(deleted, 0) = 0) " +
            "      GROUP BY consumer_id) t")
    Map<String, Object> repurchaseStats(@Param("farmerId") Long farmerId);

    @Select("SELECT COALESCE(AVG(TIMESTAMPDIFF(HOUR, c.create_time, c.first_reply_time)), 0) " +
            "FROM complaint c JOIN crop_batch b ON c.batch_id = b.id " +
            "WHERE b.farmer_id = #{farmerId} AND c.first_reply_time IS NOT NULL")
    double avgFirstReplyHours(@Param("farmerId") Long farmerId);
}
