package org.example.nongye.mapper;

import org.apache.ibatis.annotations.*;
import org.example.nongye.entity.Complaint;

import java.util.List;

@Mapper
public interface ComplaintMapper {

    @Select("SELECT c.*, b.crop_name, b.batch_no FROM complaint c " +
            "LEFT JOIN crop_batch b ON c.batch_id = b.id ORDER BY c.id DESC")
    List<Complaint> selectAll();

    @Select("SELECT * FROM complaint WHERE id = #{id}")
    Complaint selectById(Long id);

    @Insert("INSERT INTO complaint (batch_id, consumer_id, complainant, phone, query_code, type, content, images, sla_hours) " +
            "VALUES (#{batchId}, #{consumerId}, #{complainant}, #{phone}, #{queryCode}, #{type}, #{content}, #{images}, #{slaHours})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Complaint complaint);

    @Update("UPDATE complaint SET status=#{status}, reply=#{reply}, reply_time=NOW(), first_reply_time=COALESCE(first_reply_time, NOW()) WHERE id=#{id}")
    int reply(Complaint complaint);

    /**
     * 扫描需自动升级管理员的投诉：还没管理员回复(first_reply_time IS NULL) + 还没标记超时(overdue=0) + SLA 已到(create_time + sla_hours 小时 < NOW())
     * sla_hours 为 NULL 时按 24 小时算
     */
    @Select("SELECT * FROM complaint WHERE first_reply_time IS NULL AND COALESCE(overdue, 0) = 0 AND status IN ('PENDING','PROCESSING') " +
            "AND TIMESTAMPDIFF(HOUR, create_time, NOW()) >= COALESCE(sla_hours, 24)")
    List<Complaint> selectPendingForEscalation();

    /** 标记为已超时升级（overdue=1），并追加一条系统日志到 reply 列开头（不影响管理员后续回复） */
    @Update("UPDATE complaint SET overdue = 1 WHERE id = #{id}")
    int markOverdue(@Param("id") Long id);

    /** 管理员首次回复时写 first_reply_time（只写一次，后续再回复不覆盖） */
    @Update("UPDATE complaint SET first_reply_time = COALESCE(first_reply_time, NOW()) WHERE id = #{id} AND first_reply_time IS NULL")
    int markFirstReply(@Param("id") Long id);

    /** 已超时升级但仍为 PENDING 的数量 */
    @Select("SELECT COUNT(*) FROM complaint WHERE status='PENDING' AND COALESCE(overdue,0)=1")
    int countOverduePending();

    /**
     * 按投诉人/手机号/作物名模糊搜索
     */
    @Select("<script>SELECT c.*, b.crop_name, b.batch_no FROM complaint c " +
            "LEFT JOIN crop_batch b ON c.batch_id = b.id " +
            "<where> " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "(c.complainant LIKE CONCAT('%', #{keyword}, '%') OR c.phone LIKE CONCAT('%', #{keyword}, '%') OR b.crop_name LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if> " +
            "</where> ORDER BY c.id DESC</script>")
    List<Complaint> search(@Param("keyword") String keyword);

    /**
     * 投诉状态分布统计（饼图）
     */
    @Select("SELECT status, COUNT(*) AS cnt FROM complaint GROUP BY status")
    List<java.util.Map<String, Object>> countByStatus();

    /**
     * 待处理投诉数量（顶部铃铛角标）
     */
    @Select("SELECT COUNT(*) FROM complaint WHERE status = 'PENDING'")
    int countPending();

    /**
     * 消费者确认回复：提交后直接关闭工单
     */
    @Update("UPDATE complaint SET status='CLOSED', consumer_reply=#{consumerReply}, consumer_reply_time=NOW() WHERE id=#{id}")
    int consumerReply(@Param("id") Long id, @Param("consumerReply") String consumerReply);

    /**
     * 消费者查询自己的投诉：仅按查询码或手机号精确匹配（不再暴露可枚举的自增 ID）
     */
    @Select("SELECT c.*, b.crop_name, b.batch_no FROM complaint c " +
            "LEFT JOIN crop_batch b ON c.batch_id = b.id " +
            "WHERE c.query_code = #{keyword} OR c.phone = #{keyword} " +
            "ORDER BY c.id DESC")
    List<Complaint> selectByPhoneOrCode(@Param("keyword") String keyword);

    /**
     * 新版消费者查询自己的投诉：按 consumer_id（新表字段）精确匹配
     */
    @Select("SELECT c.*, b.crop_name, b.batch_no FROM complaint c " +
            "LEFT JOIN crop_batch b ON c.batch_id = b.id " +
            "WHERE c.consumer_id = #{consumerId} ORDER BY c.id DESC")
    List<Complaint> selectByConsumerId(Long consumerId);

    /** 别名：消费者自己的投诉列表（联表 crop_batch 带 crop_name/batch_no） */
    @Select("SELECT c.*, b.crop_name, b.batch_no FROM complaint c " +
            "LEFT JOIN crop_batch b ON c.batch_id = b.id " +
            "WHERE c.consumer_id = #{consumerId} ORDER BY c.id DESC")
    List<Complaint> selectByConsumer(@Param("consumerId") Long consumerId);

    /**
     * 农户端：本农户名下批次的投诉工单
     */
    @Select("SELECT c.*, b.crop_name, b.batch_no FROM complaint c " +
            "JOIN crop_batch b ON c.batch_id = b.id " +
            "WHERE b.farmer_id = #{farmerId} AND COALESCE(b.deleted, 0) = 0 " +
            "ORDER BY c.id DESC")
    List<Complaint> selectByFarmer(@Param("farmerId") Long farmerId);

    /**
     * 消费者确认/关闭工单前的凭证校验
     */
    @Select("SELECT COUNT(*) FROM complaint WHERE id = #{id} AND query_code = #{queryCode}")
    int countByIdAndQueryCode(@Param("id") Long id, @Param("queryCode") String queryCode);

    /** 设置投诉 SLA 时效（小时） */
    @Update("UPDATE complaint SET sla_hours=#{hours} WHERE id=#{id}")
    int updateSla(@Param("id") Long complaintId, @Param("hours") int slaHours);
}
