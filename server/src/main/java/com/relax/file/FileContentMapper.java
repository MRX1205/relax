package com.relax.file;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FileContentMapper {

    @Update("UPDATE file_asset SET content = #{content} WHERE id = #{id}")
    void saveContent(@Param("id") long id, @Param("content") byte[] content);

    @Select("SELECT content FROM file_asset WHERE id = #{id}")
    byte[] findContent(@Param("id") long id);
}
