import { api } from "./client";
import { downloadBlob } from "@/lib/download";
import type {
  ExpenseCategory,
  ExpenseResponse,
  ExpenseSummary,
  PageResponse,
} from "@/types/api";

export interface ExpenseQuery {
  categoryId?: string;
  from?: string;
  to?: string;
  vatDeductible?: boolean;
  page?: number;
  size?: number;
}

export interface ExpensePayload {
  categoryId: string;
  amount: string;
  currency: string;
  expenseDate: string;
  description: string;
  vendor?: string | null;
  vatDeductible: boolean;
  vatAmount?: string | null;
  receiptId?: string | null;
}

export const expensesApi = {
  list: (query: ExpenseQuery = {}) =>
    api
      .get<PageResponse<ExpenseResponse>>("/expenses", { params: query })
      .then((r) => r.data),

  get: (id: string) =>
    api.get<ExpenseResponse>(`/expenses/${id}`).then((r) => r.data),

  create: (payload: ExpensePayload) =>
    api.post<ExpenseResponse>("/expenses", payload).then((r) => r.data),

  update: (id: string, payload: ExpensePayload) =>
    api.put<ExpenseResponse>(`/expenses/${id}`, payload).then((r) => r.data),

  delete: (id: string) => api.delete<void>(`/expenses/${id}`).then(() => undefined),

  summary: (from: string, to: string) =>
    api
      .get<ExpenseSummary>("/expenses/summary", { params: { from, to } })
      .then((r) => r.data),

  categories: () =>
    api.get<ExpenseCategory[]>("/expense-categories").then((r) => r.data),

  exportCsv: async (query: ExpenseQuery = {}) => {
    const response = await api.get<Blob>("/expenses/export.csv", {
      params: query,
      responseType: "blob",
    });
    const today = new Date().toISOString().slice(0, 10);
    downloadBlob(response.data, `wydatki-${today}.csv`);
  },
};
