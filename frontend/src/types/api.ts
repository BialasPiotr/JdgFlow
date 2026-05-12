export type InvoiceKind = "VAT" | "PROFORMA" | "CORRECTION" | "RECEIPT" | "ADVANCE" | "FINAL" | "OTHER";
export type InvoiceStatus = "DRAFT" | "SENT" | "PAID" | "PARTIAL" | "OVERDUE" | "CANCELLED";

export interface UserSummary {
  id: string;
  email: string;
  fullName: string;
}

export interface AuthResponse {
  token: string;
  expiresAt: string;
  user: UserSummary;
}

export interface AuthenticatedUser {
  id: string;
  email: string;
}

export interface InvoiceResponse {
  id: string;
  number: string;
  kind: InvoiceKind;
  status: InvoiceStatus;
  netAmount: string;
  vatAmount: string;
  grossAmount: string;
  currency: string;
  issueDate: string;
  saleDate: string;
  paymentDate?: string | null;
  paidDate?: string | null;
  buyerName?: string | null;
  buyerNip?: string | null;
  buyerEmail?: string | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SyncResult {
  created: number;
  updated: number;
  total: number;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  violations?: Array<{ field: string; message: string }>;
}

export interface ExpenseCategory {
  id: string;
  code: string;
  name: string;
  icon: string | null;
  color: string | null;
  deductible: boolean;
}

export interface ExpenseResponse {
  id: string;
  category: ExpenseCategory;
  amount: string;
  currency: string;
  exchangeRate: string | null;
  expenseDate: string;
  description: string;
  vendor: string | null;
  vatDeductible: boolean;
  vatAmount: string | null;
}

export interface ExpenseSummary {
  from: string;
  to: string;
  total: string;
  count: number;
  byCategory: Array<{
    category: ExpenseCategory;
    total: string;
    count: number;
  }>;
}

export type ZusMode = "ULGA_NA_START" | "PREFERENTIAL" | "MALY_ZUS_PLUS" | "STANDARD";
export type TaxForm = "SCALE" | "LINEAR" | "RYCZALT";
export type AccountingMethod = "ACCRUAL" | "CASH";
export type ObligationType = "ZUS_SOCIAL" | "ZUS_HEALTH" | "PIT_ADVANCE" | "VAT";

export interface TaxObligationResponse {
  id: string;
  periodId: string;
  type: ObligationType;
  amount: string;
  deadline: string;
  paidDate: string | null;
  paidAmount: string | null;
  isPaid: boolean;
}

export interface BreakdownStep {
  label: string;
  amount: string;
  formula: string | null;
}

export interface BreakdownPayload {
  steps: BreakdownStep[];
  total: string;
}

export interface ZusBreakdownPayload {
  mode: ZusMode;
  base: string;
  components: Partial<Record<"PENSION" | "DISABILITY" | "SICKNESS" | "ACCIDENT" | "LABOR_FUND", string>>;
  total: string;
}

export interface MonthlyBreakdownPayload {
  year: number;
  month: number;
  revenue: string;
  costs: string;
  income: string;
  zus: ZusBreakdownPayload;
  health: BreakdownPayload;
  pit: BreakdownPayload;
  vatDue: string;
}

export interface TaxPeriodResponse {
  id: string;
  year: number;
  month: number;
  revenue: string;
  costs: string;
  income: string;
  zusSocialTotal: string;
  healthAmount: string;
  pitAdvance: string;
  vatDue: string;
  breakdown: MonthlyBreakdownPayload | null;
  computedAt: string;
  obligations: TaxObligationResponse[];
}

export interface TaxProfileResponse {
  zusMode: ZusMode;
  taxForm: TaxForm;
  vatPayer: boolean;
  accountingMethod: AccountingMethod;
  voluntarySickness: boolean;
  ipBoxEligible: boolean;
  jointSettlement: boolean;
  businessStartDate: string | null;
  previousYearRevenue: string | null;
  previousYearIncome: string | null;
}

export type UpdateTaxProfileRequest = TaxProfileResponse;

