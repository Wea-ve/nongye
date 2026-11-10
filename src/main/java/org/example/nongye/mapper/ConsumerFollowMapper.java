package org.example.nongye.mapper;

import org.apache.ibatis.annotations.*;
import org.example.nongye.entity.ConsumerFollow;

import java.util.List;

@Mapper
public interface ConsumerFollowMapper {

    @Insert("INSERT INTO consumer_follow (consumer_id, target_type, target_id, notify_enabled, create_time) " +
            "VALUES (#{consumerId}, #{targetType}, #{targetId}, #{notifyEnabled}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConsumerFollow follow);

    @Select("SELECT * FROM consumer_follow " +
            "WHERE consumer_id = #{consumerId} AND target_type = #{targetType} AND target_id = #{targetId}")
    ConsumerFollow selectByConsumerAndTarget(@Param("consumerId") Long consumerId,
                                             @Param("targetType") String targetType,
                                             @Param("targetId") Long targetId);

    @Select("SELECT * FROM consumer_follow WHERE consumer_id = #{consumerId} ORDER BY create_time DESC")
    List<ConsumerFollow> selectByConsumer(Long consumerId);

    @Delete("DELETE FROM consumer_follow WHERE id = #{id}")
    int deleteById(Long id);
}
