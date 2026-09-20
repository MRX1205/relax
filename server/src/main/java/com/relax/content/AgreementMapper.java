package com.relax.content;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AgreementMapper {

    @Select("SELECT id, type, version, title, content, effective_at AS effectiveAt "
            + "FROM content_agreement WHERE type = #{type} AND status = 'ACTIVE' "
            + "AND effective_at <= CURRENT_TIMESTAMP ORDER BY effective_at DESC, id DESC LIMIT 1")
    Optional<Agreement> findCurrent(@Param("type") String type);

    @Select("SELECT id, type, version, title, content, effective_at AS effectiveAt "
            + "FROM content_agreement WHERE id = #{id} AND status = 'ACTIVE'")
    Optional<Agreement> findById(@Param("id") long id);

    @Select("SELECT COUNT(*) FROM user_agreement_consent "
            + "WHERE user_id = #{userId} AND agreement_id = #{agreementId} AND context = #{context}")
    int countConsent(@Param("userId") long userId, @Param("agreementId") long agreementId,
            @Param("context") String context);

    @Insert("INSERT INTO user_agreement_consent (user_id, agreement_id, context) "
            + "VALUES (#{userId}, #{agreementId}, #{context})")
    void insertConsent(@Param("userId") long userId, @Param("agreementId") long agreementId,
            @Param("context") String context);

    @Select("SELECT a.id AS agreementId, a.type, a.version, c.context, c.agreed_at AS agreedAt "
            + "FROM user_agreement_consent c JOIN content_agreement a ON a.id = c.agreement_id "
            + "WHERE c.user_id = #{userId} ORDER BY c.agreed_at DESC")
    List<Consent> findConsents(@Param("userId") long userId);

    record Agreement(long id, String type, String version, String title, String content, LocalDateTime effectiveAt) {
    }

    record Consent(long agreementId, String type, String version, String context, LocalDateTime agreedAt) {
    }
}
