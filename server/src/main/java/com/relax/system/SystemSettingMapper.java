package com.relax.system;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SystemSettingMapper {

    @Select("SELECT setting_value FROM app_setting WHERE setting_key = #{key}")
    String getSetting(@Param("key") String key);

    @Insert("INSERT INTO app_setting (setting_key, setting_value, description) "
            + "VALUES (#{key}, #{value}, #{description}) "
            + "ON DUPLICATE KEY UPDATE setting_value = #{value}, updated_at = CURRENT_TIMESTAMP")
    void upsertSetting(@Param("key") String key, @Param("value") String value, 
                       @Param("description") String description);
}
