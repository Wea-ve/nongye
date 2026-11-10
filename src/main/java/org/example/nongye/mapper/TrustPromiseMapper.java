package org.example.nongye.mapper;

import org.apache.ibatis.annotations.*;
import org.example.nongye.entity.TrustPromise;

import java.util.List;

@Mapper
public interface TrustPromiseMapper {

    @Insert("INSERT INTO trust_promise (batch_id, farmer_id, promise_type, promise_value, " +
            "evidence_path, status, create_time) " +
            "VALUES (#{batchId}, #{farmerId}, #{promiseType}, #{promiseValue}, " +
            "#{evidencePath}, #{status}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TrustPromise promise);

    @Select("SELECT * FROM trust_promise WHERE batch_id = #{batchId}")
    List<TrustPromise> selectByBatch(Long batchId);

    @Select("SELECT * FROM trust_promise WHERE id = #{id}")
    TrustPromise selectById(Long id);

    @Select("SELECT * FROM trust_promise WHERE farmer_id = #{farmerId}")
    List<TrustPromise> selectByFarmer(Long farmerId);

    @Delete("DELETE FROM trust_promise WHERE id = #{id}")
    int deleteById(Long id);
}
