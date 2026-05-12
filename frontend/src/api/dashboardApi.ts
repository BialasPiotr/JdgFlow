import { api } from "./client";
import { downloadBlob } from "@/lib/download";

export interface CashflowMonthEntry {
  month: number;
  revenue: string;
  costs: string;
  income: string;
}

export interface CashflowTotal {
  revenue: string;
  costs: string;
  income: string;
}

export interface CashflowResponse {
  year: number;
  months: CashflowMonthEntry[];
  total: CashflowTotal;
  previousYear: CashflowResponse | null;
}

export const dashboardApi = {
  cashflow: (year?: number) =>
    api
      .get<CashflowResponse>("/dashboard/cashflow", {
        params: year != null ? { year } : undefined,
      })
      .then((r) => r.data),

  cashflowPdf: async (year: number) => {
    const response = await api.get<Blob>("/dashboard/cashflow.pdf", {
      params: { year },
      responseType: "blob",
    });
    downloadBlob(response.data, `cashflow-${year}.pdf`);
  },
};
