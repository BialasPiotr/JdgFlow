import { api } from "./client";
import { downloadBlob } from "@/lib/download";
import type { InvoiceResponse, InvoiceStatus, PageResponse, SyncResult } from "@/types/api";

export interface InvoiceQuery {
  status?: InvoiceStatus;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export const invoicesApi = {
  list: (query: InvoiceQuery = {}) =>
    api
      .get<PageResponse<InvoiceResponse>>("/invoices", { params: query })
      .then((r) => r.data),

  get: (id: string) =>
    api.get<InvoiceResponse>(`/invoices/${id}`).then((r) => r.data),

  sync: () => api.post<SyncResult>("/invoices/sync").then((r) => r.data),

  exportCsv: async (query: InvoiceQuery = {}) => {
    const response = await api.get<Blob>("/invoices/export.csv", {
      params: query,
      responseType: "blob",
    });
    const today = new Date().toISOString().slice(0, 10);
    downloadBlob(response.data, `faktury-${today}.csv`);
  },
};
