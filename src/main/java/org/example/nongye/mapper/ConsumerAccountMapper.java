package org.example.nongye.mapper;

import org.apache.ibatis.annotations.*;
import org.example.nongye.entity.ConsumerAccount;

@Mapper
public interface ConsumerAccountMapper {

    @Insert("INSERT INTO consumer_account (nickname, phone, open_id, trust_level, invite_code, " +
            "inviter_type, inviter_id, profile_json, status, last_active_time, create_time) " +
            "VALUES (#{nickname}, #{phone}, #{openId}, #{trustLevel}, #{inviteCode}, " +
            "#{inviterType}, #{inviterId}, #{profileJson}, #{status}, #{lastActiveTime}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConsumerAccount account);

    @Select("SELECT * FROM consumer_account WHERE id = #{id}")
    ConsumerAccount selectById(Long id);

    @Select("SELECT * FROM consumer_account WHERE phone = #{phone}")
    ConsumerAccount selectByPhone(String phone);

    @Select("SELECT * FROM consumer_account WHERE invite_code = #{inviteCode}")
    ConsumerAccount selectByInviteCode(String inviteCode);

    @Update("UPDATE consumer_account SET last_active_time = NOW() WHERE id = #{id}")
    int updateLastActive(Long id);
}
