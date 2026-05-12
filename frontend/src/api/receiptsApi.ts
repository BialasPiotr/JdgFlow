import { api } from "./client";

export type ReceiptStatus = "PENDING" | "PROCESSED" | "FAILED";

export interface SuggestedExpense {
  amount: string | number | null;
  currency: string | null;
  vatAmount: string | number | null;
  vatDeductible: boolean | null;
  expenseDate: string | null;
  vendor: string | null;
  description: string | null;
  suggestedCategoryCode: string | null;
  suggestedCategoryId: string | null;
  confidence: number | null;
}

export interface ReceiptUploadResponse {
  id: string;
  s3Key: string;
  originalFilename: string | null;
  mimeType: string;
  fileSizeBytes: number;
  status: ReceiptStatus;
  errorMessage: string | null;
  suggested: SuggestedExpense | null;
}

export const receiptsApi = {
  upload: (file: File) => {
    const form = new FormData();
    form.append("file", file);
    return api
      .post<ReceiptUploadResponse>("/receipts/upload", form, {
        headers: { "Content-Type": "multipart/form-data" },
      })
      .then((r) => r.data);
  },

  imageUrl: (id: string) => `/api/receipts/${id}/image`,
};
