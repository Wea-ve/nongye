package org.example.nongye.mapper;

import org.apache.ibatis.annotations.*;
import org.example.nongye.entity.TraceRecord;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface TraceRecordMapper {

    @Select("SELECT t.*, f.real_name AS from_user_name, u.real_name AS to_user_name, " +
            "b.batch_no AS batch_no, b.crop_name AS crop_name " +
            "FROM trace_record t " +
            "LEFT JOIN sys_user f ON t.from_user = f.id " +
            "LEFT JOIN sys_user u ON t.to_user = u.id " +
            "LEFT JOIN crop_batch b ON t.batch_id = b.id " +
            "WHERE t.batch_id = #{batchId} ORDER BY t.operate_time ASC")
    List<TraceRecord> selectByBatchId(Long batchId);

    /**
     * 全平台流转记录（管理员）
     */
    @Select("SELECT t.*, f.real_name AS from_user_name, u.real_name AS to_user_name, " +
            "b.batch_no AS batch_no, b.crop_name AS crop_name " +
            "FROM trace_record t " +
            "LEFT JOIN sys_user f ON t.from_user = f.id " +
            "LEFT JOIN sys_user u ON t.to_user = u.id " +
            "LEFT JOIN crop_batch b ON t.batch_id = b.id " +
            "ORDER BY t.operate_time DESC")
    List<TraceRecord> selectAllWithBatch();

    /**
     * 某农户所有批次的发货（被经销商入库）记录
     */
    @Select("SELECT t.*, u.real_name AS to_user_name, b.batch_no AS batch_no, b.crop_name AS crop_name " +
            "FROM trace_record t " +
            "LEFT JOIN sys_user u ON t.to_user = u.id " +
            "LEFT JOIN crop_batch b ON t.batch_id = b.id " +
            "WHERE b.farmer_id = #{farmerId} AND t.type IN ('INBOUND','OUTBOUND','SALE') " +
            "ORDER BY t.operate_time DESC")
    List<TraceRecord> selectSalesByFarmer(Long farmerId);

    /**
     * 某经销商指定类型的流转记录（入库/出库）
     */
    @Select("SELECT t.*, f.real_name AS from_user_name, u.real_name AS to_user_name, " +
            "b.batch_no AS batch_no, b.crop_name AS crop_name " +
            "FROM trace_record t " +
            "LEFT JOIN sys_user f ON t.from_user = f.id " +
            "LEFT JOIN sys_user u ON t.to_user = u.id " +
            "LEFT JOIN crop_batch b ON t.batch_id = b.id " +
            "WHERE (t.from_user = #{userId} OR t.to_user = #{userId}) AND t.type = #{type} " +
            "ORDER BY t.operate_time DESC")
    List<TraceRecord> selectByUserAndType(@Param("userId") Long userId, @Param("type") String type);

    @Insert("INSERT INTO trace_record (batch_id, from_user, to_user, type, quantity, location, remark) " +
            "VALUES (#{batchId}, #{fromUser}, #{toUser}, #{type}, #{quantity}, #{location}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TraceRecord record);

    /**
     * 按批次号/作物名模糊搜索流转记录
     */
    @Select("<script>SELECT t.*, f.real_name AS from_user_name, u.real_name AS to_user_name, " +
            "b.batch_no AS batch_no, b.crop_name AS crop_name " +
            "FROM trace_record t " +
            "LEFT JOIN sys_user f ON t.from_user = f.id " +
            "LEFT JOIN sys_user u ON t.to_user = u.id " +
            "LEFT JOIN crop_batch b ON t.batch_id = b.id " +
            "<where> " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "(b.batch_no LIKE CONCAT('%', #{keyword}, '%') OR b.crop_name LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if> " +
            "</where> ORDER BY t.operate_time DESC</script>")
    List<TraceRecord> search(@Param("keyword") String keyword);

    /**
     * 近 N 天每天的流转次数（用于扫码量折线图）
     */
    @Select("SELECT DATE(operate_time) AS day, COUNT(*) AS cnt FROM trace_record " +
            "WHERE operate_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) GROUP BY DATE(operate_time) ORDER BY day")
    List<java.util.Map<String, Object>> countByDay(@Param("days") int days);

    /**
     * 某批次的某类流转总数量（入库/出库）
     */
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM trace_record WHERE batch_id = #{batchId} AND type = #{type}")
    BigDecimal sumQuantityByBatchAndType(@Param("batchId") Long batchId, @Param("type") String type);

    /**
     * 某经销商在某批次的入库总量
     */
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM trace_record WHERE batch_id = #{batchId} AND type = 'INBOUND' AND to_user = #{userId}")
    BigDecimal sumInboundByDealer(@Param("batchId") Long batchId, @Param("userId") Long userId);

    /**
     * 某经销商在某批次的出库总量
     */
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM trace_record WHERE batch_id = #{batchId} AND type = 'OUTBOUND' AND from_user = #{userId}")
    BigDecimal sumOutboundByDealer(@Param("batchId") Long batchId, @Param("userId") Long userId);

    /**
     * 某批次的流转节点数（用于可信徽章）
     */
    @Select("SELECT COUNT(*) FROM trace_record WHERE batch_id = #{batchId}")
    int countByBatchId(Long batchId);
}
