package org.example.nongye.mapper;

import org.apache.ibatis.annotations.*;
import org.example.nongye.entity.FulfillmentRecord;

import java.util.List;

@Mapper
public interface FulfillmentRecordMapper {

    @Insert("INSERT INTO fulfillment_record (batch_id, dealer_id, consumer_id, ship_time, " +
            "arrive_time, quality_flag, aftersale_hours, create_time) " +
            "VALUES (#{batchId}, #{dealerId}, #{consumerId}, #{shipTime}, " +
            "#{arriveTime}, #{qualityFlag}, #{aftersaleHours}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FulfillmentRecord record);

    @Select("SELECT * FROM fulfillment_record WHERE batch_id = #{batchId}")
    List<FulfillmentRecord> selectByBatch(Long batchId);

    @Select("SELECT * FROM fulfillment_record WHERE consumer_id = #{consumerId}")
    List<FulfillmentRecord> selectByConsumer(Long consumerId);
}
