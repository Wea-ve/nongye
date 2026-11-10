package org.example.nongye.mapper;

import org.apache.ibatis.annotations.*;
import org.example.nongye.entity.ConsumerEvent;

import java.util.List;

@Mapper
public interface ConsumerEventMapper {

    @Insert("INSERT INTO consumer_event (consumer_id, event_type, batch_id, payload, create_time) " +
            "VALUES (#{consumerId}, #{eventType}, #{batchId}, #{payload}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConsumerEvent event);

    /** 批量插入（最多 10 条） */
    @Insert("<script>" +
            "INSERT INTO consumer_event (consumer_id, event_type, batch_id, payload, create_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.consumerId}, #{item.eventType}, #{item.batchId}, #{item.payload}, #{item.createTime})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("list") List<ConsumerEvent> list);

    @Select("SELECT * FROM consumer_event WHERE consumer_id = #{consumerId} ORDER BY create_time DESC LIMIT 50")
    List<ConsumerEvent> selectByConsumer(Long consumerId);

    @Select("SELECT COUNT(*) FROM consumer_event WHERE event_type = #{eventType}")
    int countByType(String eventType);
}
