package org.example.nongye.mapper;

import org.apache.ibatis.annotations.*;
import org.example.nongye.entity.ScanLog;

import java.util.List;
import java.util.Map;

@Mapper
public interface ScanLogMapper {

    @Insert("INSERT INTO scan_log (batch_id, consumer_id, scene, trace_code, ip, user_agent) " +
            "VALUES (#{batchId}, #{consumerId}, #{scene}, #{traceCode}, #{ip}, #{userAgent})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ScanLog log);

    /** 今日扫码总数 */
    @Select("SELECT COUNT(*) FROM scan_log WHERE DATE(scan_time) = CURDATE()")
    int countToday();

    /** 累计扫码总数 */
    @Select("SELECT COUNT(*) FROM scan_log")
    int countAll();

    /** 近 7 天每日扫码量 */
    @Select("SELECT DATE(scan_time) AS day, COUNT(*) AS cnt FROM scan_log " +
            "WHERE scan_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY DATE(scan_time) ORDER BY day")
    List<Map<String, Object>> countLast7Days();

    /** 热门批次排行（扫码次数最多） */
    @Select("SELECT b.crop_name AS crop_name, b.batch_no AS batch_no, COUNT(s.id) AS cnt " +
            "FROM scan_log s LEFT JOIN crop_batch b ON s.batch_id = b.id " +
            "GROUP BY s.batch_id ORDER BY cnt DESC LIMIT 10")
    List<Map<String, Object>> hotBatches();

    /** 扫码明细列表 */
    @Select("SELECT s.*, b.crop_name AS crop_name, b.batch_no AS batch_no FROM scan_log s " +
            "LEFT JOIN crop_batch b ON s.batch_id = b.id ORDER BY s.scan_time DESC LIMIT 200")
    List<ScanLog> selectAll();

    /** 补写扫码时的消费者与场景（先 insert 后 update，兼容旧扫描链路） */
    @Update("UPDATE scan_log SET consumer_id=#{cid}, scene=#{scene} WHERE id=#{id}")
    int updateConsumerAndScene(@Param("id") Long id, @Param("cid") Long cid, @Param("scene") String scene);
}
