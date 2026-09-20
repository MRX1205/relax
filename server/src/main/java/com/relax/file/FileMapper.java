package com.relax.file;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FileMapper {

    String COLUMNS = "id, owner_user_id AS ownerUserId, purpose, storage_provider AS storageProvider, "
            + "object_key AS objectKey, original_name AS originalName, mime_type AS mimeType, "
            + "expected_size AS expectedSize, actual_size AS actualSize, status, "
            + "upload_token_digest AS uploadTokenDigest, upload_expires_at AS uploadExpiresAt, "
            + "created_at AS createdAt, completed_at AS completedAt";

    @Insert("INSERT INTO file_asset (id, owner_user_id, purpose, storage_provider, object_key, original_name, "
            + "mime_type, expected_size, upload_token_digest, upload_expires_at) VALUES "
            + "(#{id}, #{ownerUserId}, #{purpose}, #{storageProvider}, #{objectKey}, #{originalName}, "
            + "#{mimeType}, #{expectedSize}, #{uploadTokenDigest}, #{uploadExpiresAt})")
    void insert(FileAsset asset);

    @Select("SELECT " + COLUMNS + " FROM file_asset WHERE id = #{id}")
    Optional<FileAsset> findById(@Param("id") long id);

    @Select("SELECT " + COLUMNS + " FROM file_asset WHERE owner_user_id = #{ownerUserId} ORDER BY id DESC")
    List<FileAsset> findByOwner(@Param("ownerUserId") long ownerUserId);

    @Update("UPDATE file_asset SET actual_size = #{actualSize}, status = 'UPLOADED' "
            + "WHERE id = #{id} AND status = 'PENDING'")
    int markUploaded(@Param("id") long id, @Param("actualSize") long actualSize);

    @Update("UPDATE file_asset SET actual_size = #{actualSize}, status = 'READY', completed_at = CURRENT_TIMESTAMP, "
            + "upload_token_digest = NULL, upload_expires_at = NULL WHERE id = #{id} AND status IN ('PENDING', 'UPLOADED')")
    int markReady(@Param("id") long id, @Param("actualSize") long actualSize);

    @Update("UPDATE file_asset SET status = 'REJECTED', upload_token_digest = NULL, upload_expires_at = NULL "
            + "WHERE id = #{id}")
    void markRejected(@Param("id") long id);
}
