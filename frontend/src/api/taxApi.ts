import { api } from "./client";
import type {
  TaxObligationResponse,
  TaxPeriodResponse,
  TaxProfileResponse,
  UpdateTaxProfileRequest,
} from "@/types/api";

export const taxApi = {
  recompute: (year: number) =>
    api.post<TaxPeriodResponse[]>(`/tax/recompute/${year}`).then((r) => r.data),

  year: (year: number) =>
    api.get<TaxPeriodResponse[]>(`/tax/year/${year}`).then((r) => r.data),

  month: (year: number, month: number) =>
    api.get<TaxPeriodResponse>(`/tax/months/${year}/${month}`).then((r) => r.data),

  upcomingObligations: (until?: string) =>
    api
      .get<TaxObligationResponse[]>("/tax/obligations/upcoming", {
        params: until ? { until } : {},
      })
      .then((r) => r.data),

  markPaid: (id: string, paidDate?: string, paidAmount?: string) =>
    api
      .post<TaxObligationResponse>(`/tax/obligations/${id}/paid`, { paidDate, paidAmount })
      .then((r) => r.data),

  unmarkPaid: (id: string) =>
    api.delete<void>(`/tax/obligations/${id}/paid`).then(() => undefined),

  profile: () => api.get<TaxProfileResponse>("/tax/profile").then((r) => r.data),

  updateProfile: (payload: UpdateTaxProfileRequest) =>
    api.put<TaxProfileResponse>("/tax/profile", payload).then((r) => r.data),
};
