package org.example.nongye.mapper;

import org.apache.ibatis.annotations.*;
import org.example.nongye.entity.CropBatch;

import java.util.List;

@Mapper
public interface CropBatchMapper {

    @Select("SELECT b.*, u.real_name AS farmer_name, u.phone AS farmer_phone, u.address AS farmer_address " +
            "FROM crop_batch b LEFT JOIN sys_user u ON b.farmer_id = u.id " +
            "WHERE b.id = #{id} AND b.deleted = 0")
    CropBatch selectById(Long id);

    @Select("SELECT b.*, u.real_name AS farmer_name FROM crop_batch b " +
            "LEFT JOIN sys_user u ON b.farmer_id = u.id " +
            "WHERE b.farmer_id = #{farmerId} AND b.deleted = 0 ORDER BY b.id DESC")
    List<CropBatch> selectByFarmerId(Long farmerId);

    /**
     * 农户按批次号/作物名模糊搜索自己的批次
     */
    @Select("<script>SELECT b.*, u.real_name AS farmer_name FROM crop_batch b " +
            "LEFT JOIN sys_user u ON b.farmer_id = u.id " +
            "WHERE b.farmer_id = #{farmerId} AND b.deleted = 0 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (b.batch_no LIKE CONCAT('%', #{keyword}, '%') OR b.crop_name LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if> ORDER BY b.id DESC</script>")
    List<CropBatch> searchByFarmer(@Param("farmerId") Long farmerId, @Param("keyword") String keyword);

    @Select("SELECT b.*, u.real_name AS farmer_name FROM crop_batch b " +
            "LEFT JOIN sys_user u ON b.farmer_id = u.id " +
            "WHERE b.deleted = 0 ORDER BY b.id DESC")
    List<CropBatch> selectAll();

    /**
     * 查询指定经销商经手过的批次（在 trace_record 中作为 from_user 或 to_user）
     */
    @Select("SELECT DISTINCT b.*, u.real_name AS farmer_name FROM crop_batch b " +
            "LEFT JOIN sys_user u ON b.farmer_id = u.id " +
            "WHERE b.deleted = 0 AND b.id IN (SELECT batch_id FROM trace_record WHERE from_user = #{userId} OR to_user = #{userId}) " +
            "ORDER BY b.id DESC")
    List<CropBatch> selectByDealerInvolved(Long userId);

    /**
     * 经销商按批次号/作物名模糊搜索经手批次
     */
    @Select("<script>SELECT DISTINCT b.*, u.real_name AS farmer_name FROM crop_batch b " +
            "LEFT JOIN sys_user u ON b.farmer_id = u.id " +
            "WHERE b.deleted = 0 AND b.id IN (SELECT batch_id FROM trace_record WHERE from_user = #{userId} OR to_user = #{userId}) " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (b.batch_no LIKE CONCAT('%', #{keyword}, '%') OR b.crop_name LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if> ORDER BY b.id DESC</script>")
    List<CropBatch> searchByDealerInvolved(@Param("userId") Long userId, @Param("keyword") String keyword);

    @Select("SELECT * FROM crop_batch WHERE trace_code = #{traceCode} AND deleted = 0")
    CropBatch selectByTraceCode(String traceCode);

    @Insert("INSERT INTO crop_batch (batch_no, farmer_id, crop_name, variety, land_location, plant_date, expect_harvest_date, status) " +
            "VALUES (#{batchNo}, #{farmerId}, #{cropName}, #{variety}, #{landLocation}, #{plantDate}, #{expectHarvestDate}, 'PLANTING')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CropBatch batch);

    @Update("UPDATE crop_batch SET status='HARVESTED', actual_harvest_date=#{actualHarvestDate}, " +
            "harvest_weight=#{harvestWeight}, trace_code=#{traceCode}, qr_code_path=#{qrCodePath} WHERE id=#{id}")
    int harvest(CropBatch batch);

    @Update("UPDATE crop_batch SET status='SOLD' WHERE id=#{id}")
    int markSold(Long id);

    /**
     * 软删除批次
     */
    @Update("UPDATE crop_batch SET deleted = 1 WHERE id = #{id}")
    int softDelete(Long id);

    // 统计：各作物产量
    @Select("SELECT crop_name, SUM(harvest_weight) AS total FROM crop_batch WHERE status != 'PLANTING' AND deleted = 0 GROUP BY crop_name")
    List<java.util.Map<String, Object>> countByCrop();

    /**
     * 按状态查询批次（IN 查询）
     */
    @Select("<script>SELECT b.*, u.real_name AS farmer_name FROM crop_batch b " +
            "LEFT JOIN sys_user u ON b.farmer_id = u.id " +
            "WHERE b.deleted = 0 AND b.status IN " +
            "<foreach collection='statuses' item='s' open='(' separator=',' close=')'>#{s}</foreach> " +
            "ORDER BY b.id DESC</script>")
    List<CropBatch> selectByStatus(@Param("statuses") String... statuses);

    /**
     * 按批次号/作物名模糊搜索（管理员）
     */
    @Select("<script>SELECT b.*, u.real_name AS farmer_name FROM crop_batch b " +
            "LEFT JOIN sys_user u ON b.farmer_id = u.id WHERE b.deleted = 0 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (b.batch_no LIKE CONCAT('%', #{keyword}, '%') OR b.crop_name LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if> ORDER BY b.id DESC</script>")
    List<CropBatch> search(@Param("keyword") String keyword);
}
