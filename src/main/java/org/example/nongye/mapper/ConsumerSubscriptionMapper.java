package org.example.nongye.mapper;

import org.apache.ibatis.annotations.*;
import org.example.nongye.entity.ConsumerSubscription;

import java.util.List;

@Mapper
public interface ConsumerSubscriptionMapper {

    @Insert("INSERT INTO consumer_subscription (consumer_id, batch_id, crop_name, qty, unit, " +
            "cycle_days, next_date, status, create_time) " +
            "VALUES (#{consumerId}, #{batchId}, #{cropName}, #{qty}, #{unit}, " +
            "#{cycleDays}, #{nextDate}, #{status}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConsumerSubscription subscription);

    @Select("SELECT * FROM consumer_subscription WHERE consumer_id = #{consumerId}")
    List<ConsumerSubscription> selectByConsumer(Long consumerId);

    @Select("SELECT * FROM consumer_subscription WHERE consumer_id = #{consumerId} AND status = #{status}")
    List<ConsumerSubscription> selectByConsumerAndStatus(Long consumerId, String status);

    @Select("SELECT * FROM consumer_subscription WHERE id = #{id}")
    ConsumerSubscription selectById(Long id);

    @Update("UPDATE consumer_subscription SET consumer_id=#{consumerId}, batch_id=#{batchId}, " +
            "crop_name=#{cropName}, qty=#{qty}, unit=#{unit}, cycle_days=#{cycleDays}, " +
            "next_date=#{nextDate}, status=#{status} WHERE id=#{id}")
    int update(ConsumerSubscription subscription);

    @Select("SELECT * FROM consumer_subscription WHERE status='ACTIVE' AND next_date <= CURDATE()")
    List<ConsumerSubscription> selectDueToday();

    /** 标记"买过了"：按周期顺延下次购买日期 */
    @Update("UPDATE consumer_subscription " +
            "SET next_date = DATE_ADD(COALESCE(next_date, CURDATE()), INTERVAL COALESCE(cycle_days, 7) DAY) " +
            "WHERE id = #{id} AND consumer_id = #{consumerId}")
    int markConsumed(@Param("id") Long id, @Param("consumerId") Long consumerId);
}
