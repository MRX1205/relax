package com.relax.content;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.relax.common.api.BusinessException;

@Service
public class AgreementService {

    private static final Set<String> TYPES = Set.of("USER_AGREEMENT", "PRIVACY_POLICY", "TRANSACTION_RULES");
    private static final Set<String> CONTEXTS = Set.of("LOGIN", "ORDER");

    private final AgreementMapper agreementMapper;

    AgreementService(AgreementMapper agreementMapper) {
        this.agreementMapper = agreementMapper;
    }

    public AgreementMapper.Agreement current(String type) {
        if (!TYPES.contains(type)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "AGREEMENT_NOT_FOUND", "协议不存在");
        }
        return agreementMapper.findCurrent(type)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "AGREEMENT_NOT_FOUND", "协议不存在"));
    }

    @Transactional
    public AgreementMapper.Consent consent(long userId, long agreementId, String context) {
        if (!CONTEXTS.contains(context)) {
            throw new BusinessException("AGREEMENT_CONTEXT_INVALID", "协议同意场景无效");
        }
        AgreementMapper.Agreement agreement = agreementMapper.findById(agreementId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "AGREEMENT_NOT_FOUND", "协议不存在"));
        if (agreementMapper.countConsent(userId, agreementId, context) == 0) {
            agreementMapper.insertConsent(userId, agreementId, context);
        }
        return agreementMapper.findConsents(userId).stream()
                .filter(item -> item.agreementId() == agreementId && context.equals(item.context()))
                .findFirst().orElseThrow();
    }

    public List<AgreementMapper.Consent> consents(long userId) {
        return agreementMapper.findConsents(userId);
    }

    public record ConsentRequest(String context) {
    }
}
