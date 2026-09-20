package com.relax.catalog;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CategoryMapper {

    @Select("SELECT id, name, sort, status, created_at AS createdAt FROM service_category ORDER BY sort, id")
    List<CategoryView> findAll();

    @Select("SELECT id, name, sort, status, created_at AS createdAt FROM service_category WHERE id = #{id}")
    Optional<CategoryView> findById(@Param("id") long id);

    @Insert("INSERT INTO service_category (id, name, sort) VALUES (#{id}, #{name}, #{sort})")
    void insert(@Param("id") long id, @Param("name") String name, @Param("sort") int sort);

    @Update("UPDATE service_category SET name = #{name}, sort = #{sort}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int update(@Param("id") long id, @Param("name") String name, @Param("sort") int sort);

    @Update("UPDATE service_category SET status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") long id, @Param("status") String status);

    record CategoryView(long id, String name, int sort, String status, java.time.LocalDateTime createdAt) {
    }
}
