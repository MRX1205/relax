import { request } from "./http";

export function getAgreement(type: AgreementType): Promise<Agreement> {
  return request<Agreement>({ url: `/api/v1/agreements/${type}` });
}

export function consentToAgreement(agreementId: string, context: "LOGIN" | "ORDER"): Promise<AgreementConsent> {
  return request<AgreementConsent>({
    url: `/api/v1/me/agreements/${agreementId}/consent`,
    method: "POST",
    data: { context },
  });
}

export function getMyConsents(): Promise<AgreementConsent[]> {
  return request<AgreementConsent[]>({ url: "/api/v1/me/agreements" });
}
