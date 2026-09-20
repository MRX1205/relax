package com.relax.region;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RegionMapper {

    @Select("SELECT a.id, r.id AS regionId, r.code AS regionCode, r.name, a.status "
            + "FROM service_area a JOIN service_region r ON r.id = a.region_id "
            + "WHERE r.parent_id = 441900000 ORDER BY r.code")
    List<ServiceArea> findServiceAreas();

    @Select("SELECT a.id, r.id AS regionId, r.code AS regionCode, r.name, a.status "
            + "FROM service_area a JOIN service_region r ON r.id = a.region_id WHERE r.code = #{regionCode}")
    Optional<ServiceArea> findServiceAreaByRegionCode(@Param("regionCode") String regionCode);

    @Update("UPDATE service_area SET status = #{status}, updated_by = #{operatorId}, "
            + "updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateServiceArea(@Param("id") long id, @Param("status") String status, @Param("operatorId") long operatorId);

    @Select("SELECT a.id, a.user_id AS userId, a.contact_name AS contactName, "
            + "a.contact_phone AS contactPhone, r.code AS regionCode, r.name AS regionName, a.detail, "
            + "a.longitude, a.latitude, a.label, a.is_default AS isDefault, a.created_at AS createdAt "
            + "FROM user_address a JOIN service_region r ON r.id = a.region_id "
            + "WHERE a.user_id = #{userId} ORDER BY a.is_default DESC, a.id DESC")
    List<UserAddress> findAddresses(@Param("userId") long userId);

    @Select("SELECT a.id, a.user_id AS userId, a.contact_name AS contactName, "
            + "a.contact_phone AS contactPhone, r.code AS regionCode, r.name AS regionName, a.detail, "
            + "a.longitude, a.latitude, a.label, a.is_default AS isDefault, a.created_at AS createdAt "
            + "FROM user_address a JOIN service_region r ON r.id = a.region_id "
            + "WHERE a.id = #{id} AND a.user_id = #{userId}")
    Optional<UserAddress> findAddress(@Param("userId") long userId, @Param("id") long id);

    @Select("SELECT COUNT(*) FROM user_address WHERE user_id = #{userId}")
    int countAddresses(@Param("userId") long userId);

    @Insert("INSERT INTO user_address (id, user_id, contact_name, contact_phone, region_id, detail, "
            + "longitude, latitude, label, is_default) VALUES (#{id}, #{userId}, #{contactName}, #{contactPhone}, "
            + "#{regionId}, #{detail}, #{longitude}, #{latitude}, #{label}, #{isDefault})")
    void insertAddress(@Param("id") long id, @Param("userId") long userId,
            @Param("contactName") String contactName, @Param("contactPhone") String contactPhone,
            @Param("regionId") long regionId, @Param("detail") String detail,
            @Param("longitude") BigDecimal longitude, @Param("latitude") BigDecimal latitude,
            @Param("label") String label, @Param("isDefault") boolean isDefault);

    @Update("UPDATE user_address SET contact_name = #{contactName}, contact_phone = #{contactPhone}, "
            + "region_id = #{regionId}, detail = #{detail}, longitude = #{longitude}, latitude = #{latitude}, "
            + "label = #{label}, is_default = #{isDefault}, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND user_id = #{userId}")
    int updateAddress(@Param("id") long id, @Param("userId") long userId,
            @Param("contactName") String contactName, @Param("contactPhone") String contactPhone,
            @Param("regionId") long regionId, @Param("detail") String detail,
            @Param("longitude") BigDecimal longitude, @Param("latitude") BigDecimal latitude,
            @Param("label") String label, @Param("isDefault") boolean isDefault);

    @Update("UPDATE user_address SET is_default = 0, updated_at = CURRENT_TIMESTAMP WHERE user_id = #{userId}")
    void clearDefault(@Param("userId") long userId);

    @Update("UPDATE user_address SET is_default = 1, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = (SELECT selected.id FROM (SELECT id FROM user_address WHERE user_id = #{userId} "
            + "ORDER BY id DESC LIMIT 1) selected)")
    void makeLatestDefault(@Param("userId") long userId);

    @Delete("DELETE FROM user_address WHERE id = #{id} AND user_id = #{userId}")
    int deleteAddress(@Param("userId") long userId, @Param("id") long id);

    record ServiceArea(long id, long regionId, String regionCode, String name, String status) {
    }

    record UserAddress(long id, long userId, String contactName, String contactPhone,
            String regionCode, String regionName, String detail, BigDecimal longitude, BigDecimal latitude,
            String label, boolean isDefault, LocalDateTime createdAt) {
    }
}
