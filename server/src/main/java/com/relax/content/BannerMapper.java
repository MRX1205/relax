package com.relax.content;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BannerMapper {

    @Select("SELECT id, title, image_file_id AS imageFileId, link_type AS linkType, "
            + "link_value AS linkValue, sort, status FROM banner WHERE status = 'ENABLED' ORDER BY sort, id")
    List<BannerView> findActive();

    @Select("SELECT id, title, image_file_id AS imageFileId, link_type AS linkType, "
            + "link_value AS linkValue, sort, status FROM banner ORDER BY sort, id")
    List<BannerView> findAll();

    @Insert("INSERT INTO banner (id, title, image_file_id, link_type, link_value, sort) "
            + "VALUES (#{id}, #{title}, #{imageFileId}, #{linkType}, #{linkValue}, #{sort})")
    void insert(@Param("id") long id, @Param("title") String title, @Param("imageFileId") Long imageFileId,
            @Param("linkType") String linkType, @Param("linkValue") String linkValue, @Param("sort") int sort);

    @Update("UPDATE banner SET title = #{title}, image_file_id = #{imageFileId}, "
            + "link_type = #{linkType}, link_value = #{linkValue}, sort = #{sort} WHERE id = #{id}")
    int update(@Param("id") long id, @Param("title") String title, @Param("imageFileId") Long imageFileId,
            @Param("linkType") String linkType, @Param("linkValue") String linkValue, @Param("sort") int sort);

    @Update("UPDATE banner SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") long id, @Param("status") String status);

    record BannerView(long id, String title, Long imageFileId, String linkType, String linkValue,
            int sort, String status) {}
}
