package com.relax.payment;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PaymentConfigMapper {

    @Select("SELECT config_value FROM payment_config WHERE config_key = #{key}")
    Optional<String> findValue(@Param("key") String key);

    @Select("SELECT config_key, config_value FROM payment_config")
    java.util.List<java.util.Map<String, String>> findAll();

    @Update("UPDATE payment_config SET config_value = #{value}, updated_at = CURRENT_TIMESTAMP WHERE config_key = #{key}")
    int updateValue(@Param("key") String key, @Param("value") String value);

    @org.apache.ibatis.annotations.Insert("INSERT INTO payment_config (id, config_key, config_value, updated_at) "
            + "VALUES (#{id}, #{key}, #{value}, CURRENT_TIMESTAMP) "
            + "ON DUPLICATE KEY UPDATE config_value = #{value}, updated_at = CURRENT_TIMESTAMP")
    int upsertValue(@Param("id") long id, @Param("key") String key, @Param("value") String value);
}
