import { useMemo, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  TrendingUp,
  FileText,
  Wallet,
  Receipt,
  ChevronLeft,
  ChevronRight,
  ArrowUp,
  ArrowDown,
  FileDown,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { invoicesApi } from "@/api/invoicesApi";
import { expensesApi } from "@/api/expensesApi";
import { dashboardApi, type CashflowResponse } from "@/api/dashboardApi";
import { formatMoney } from "@/lib/format";
import { useAuthStore } from "@/auth/authStore";

const monthRange = () => {
  const now = new Date();
  const from = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10);
  const to = now.toISOString().slice(0, 10);
  return { from, to };
};

export default function DashboardPage() {
  const fullName = useAuthStore((s) => s.user?.fullName);
  const firstName = fullName?.split(" ")[0] ?? null;

  const { data } = useQuery({
    queryKey: ["invoices", "dashboard-summary"],
    queryFn: () => invoicesApi.list({ size: 100 }),
  });

  const { from, to } = monthRange();
  const expensesSummary = useQuery({
    queryKey: ["expenses", "summary", from, to],
    queryFn: () => expensesApi.summary(from, to),
  });

  const totalInvoices = data?.totalElements ?? 0;
  const paidGross = data?.content
    .filter((i) => i.status === "PAID")
    .reduce((sum, i) => sum + parseFloat(i.grossAmount), 0) ?? 0;
  const sentGross = data?.content
    .filter((i) => i.status === "SENT" || i.status === "PARTIAL")
    .reduce((sum, i) => sum + parseFloat(i.grossAmount), 0) ?? 0;
  const expensesMtd = parseFloat(expensesSummary.data?.total ?? "0");

  const monthName = new Date().toLocaleDateString("pl-PL", { month: "long", year: "numeric" });

  return (
    <div className="p-8 max-w-7xl mx-auto animate-fade-in">
      <header className="mb-8">
        <p className="text-xs font-semibold text-brand-600 uppercase tracking-wider mb-1 dark:text-brand-400">Pulpit</p>
        <h1 className="text-3xl font-bold text-slate-900 dark:text-white">
          {firstName ? `Witaj, ${firstName}` : "Witaj"}
        </h1>
        <p className="text-sm text-slate-500 mt-1.5 dark:text-slate-400">
          Podsumowanie działalności — {monthName}
        </p>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <Card
          icon={<FileText size={18} />}
          iconClass="bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300"
          label="Faktury w systemie"
          value={totalInvoices.toString()}
          hint="łącznie"
        />
        <Card
          icon={<TrendingUp size={18} />}
          iconClass="bg-emerald-50 text-emerald-600"
          label="Zapłacone"
          value={formatMoney(paidGross)}
          hint="suma brutto"
          valueClass="text-emerald-600"
        />
        <Card
          icon={<Wallet size={18} />}
          iconClass="bg-amber-50 text-amber-600"
          label="Do zapłaty"
          value={formatMoney(sentGross)}
          hint="oczekuje na płatność"
          valueClass="text-amber-600"
        />
        <Card
          icon={<Receipt size={18} />}
          iconClass="bg-rose-50 text-rose-600"
          label="Wydatki MTD"
          value={formatMoney(expensesMtd)}
          hint={monthName}
          valueClass="text-rose-600"
        />
      </div>

      <CashflowWidget />
    </div>
  );
}

const MONTH_LABELS = ["Sty", "Lut", "Mar", "Kwi", "Maj", "Cze", "Lip", "Sie", "Wrz", "Paź", "Lis", "Gru"];

type ViewMode = "current" | "previous" | "compare";

function CashflowWidget() {
  const [year, setYear] = useState(() => new Date().getFullYear());
  const [mode, setMode] = useState<ViewMode>("current");

  const { data, isLoading, isError } = useQuery({
    queryKey: ["dashboard", "cashflow", year],
    queryFn: () => dashboardApi.cashflow(year),
  });

  const pdfMutation = useMutation({
    mutationFn: () => dashboardApi.cashflowPdf(year),
  });

  const chartData = useMemo(() => buildChartData(data, mode), [data, mode]);

  const yoyDelta = useMemo(() => {
    if (!data || !data.previousYear) return null;
    const cur = parseFloat(data.total.income);
    const prev = parseFloat(data.previousYear.total.income);
    if (prev === 0) return cur === 0 ? 0 : null;
    return ((cur - prev) / Math.abs(prev)) * 100;
  }, [data]);

  return (
    <div className="card p-6 animate-slide-up">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-5">
        <div>
          <h2 className="text-lg font-semibold text-slate-900 dark:text-white">Cash flow</h2>
          <p className="text-xs text-slate-500 mt-0.5 dark:text-slate-400">
            Przychód, koszty i dochód miesięcznie. Przychód po metodzie księgowania (memoriał / kasa).
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex items-center gap-0.5 rounded-lg border border-slate-200 bg-white p-0.5 shadow-soft dark:bg-slate-900 dark:border-slate-700">
            <button
              onClick={() => setMode("current")}
              className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors ${
                mode === "current"
                  ? "bg-brand-600 text-white shadow-sm"
                  : "text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white"
              }`}
            >
              {year}
            </button>
            <button
              onClick={() => setMode("previous")}
              className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors ${
                mode === "previous"
                  ? "bg-brand-600 text-white shadow-sm"
                  : "text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white"
              }`}
            >
              {year - 1}
            </button>
            <button
              onClick={() => setMode("compare")}
              className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors ${
                mode === "compare"
                  ? "bg-brand-600 text-white shadow-sm"
                  : "text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white"
              }`}
            >
              YoY
            </button>
          </div>

          <div className="flex items-center gap-1">
            <button
              onClick={() => setYear((y) => y - 1)}
              className="p-1.5 rounded-md text-slate-500 hover:text-slate-900 hover:bg-slate-100 transition-colors dark:text-slate-400 dark:hover:text-white dark:hover:bg-slate-800"
              title="Poprzedni rok"
            >
              <ChevronLeft size={16} />
            </button>
            <button
              onClick={() => setYear((y) => y + 1)}
              disabled={year >= new Date().getFullYear()}
              className="p-1.5 rounded-md text-slate-500 hover:text-slate-900 hover:bg-slate-100 disabled:text-slate-300 disabled:hover:bg-transparent transition-colors dark:text-slate-400 dark:hover:text-white dark:hover:bg-slate-800 dark:disabled:text-slate-700"
              title="Następny rok"
            >
              <ChevronRight size={16} />
            </button>
          </div>

          <button
            onClick={() => pdfMutation.mutate()}
            disabled={pdfMutation.isPending || !data}
            className="btn-secondary text-xs px-3 py-1.5"
            title={`Pobierz raport PDF za ${year}`}
          >
            <FileDown size={14} className={pdfMutation.isPending ? "animate-pulse" : ""} />
            {pdfMutation.isPending ? "Generowanie…" : "PDF"}
          </button>
        </div>
      </div>

      {isLoading && <div className="h-72 flex items-center justify-center text-slate-400 text-sm dark:text-slate-500">Ładowanie wykresu…</div>}
      {isError && <div className="h-72 flex items-center justify-center text-rose-500 text-sm">Nie udało się pobrać danych</div>}

      {data && !isLoading && (
        <>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              {mode === "compare" ? (
                <LineChart data={chartData} margin={{ top: 10, right: 8, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
                  <XAxis dataKey="label" stroke="#94a3b8" fontSize={12} tickLine={false} />
                  <YAxis stroke="#94a3b8" fontSize={12} tickFormatter={shortMoney} tickLine={false} axisLine={false} />
                  <Tooltip content={<MoneyTooltip />} cursor={{ fill: "rgba(148,163,184,0.08)" }} />
                  <Legend wrapperStyle={{ fontSize: 12, paddingTop: 8 }} />
                  <Line type="monotone" dataKey="income" name={`Dochód ${year}`} stroke="#7c3aed" strokeWidth={2.5} dot={{ r: 3 }} />
                  <Line type="monotone" dataKey="incomePrev" name={`Dochód ${year - 1}`} stroke="#94a3b8" strokeWidth={2} strokeDasharray="5 5" dot={{ r: 2 }} />
                </LineChart>
              ) : (
                <BarChart data={chartData} margin={{ top: 10, right: 8, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
                  <XAxis dataKey="label" stroke="#94a3b8" fontSize={12} tickLine={false} />
                  <YAxis stroke="#94a3b8" fontSize={12} tickFormatter={shortMoney} tickLine={false} axisLine={false} />
                  <Tooltip content={<MoneyTooltip />} cursor={{ fill: "rgba(148,163,184,0.08)" }} />
                  <Legend wrapperStyle={{ fontSize: 12, paddingTop: 8 }} />
                  <Bar dataKey="revenue" name="Przychód" fill="#10b981" radius={[3, 3, 0, 0]} />
                  <Bar dataKey="costs" name="Koszty" fill="#f43f5e" radius={[3, 3, 0, 0]} />
                  <Bar dataKey="income" name="Dochód" fill="#7c3aed" radius={[3, 3, 0, 0]} />
                </BarChart>
              )}
            </ResponsiveContainer>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mt-5 pt-5 border-t border-slate-100 dark:border-slate-800">
            <SummaryStat label={`Przychód ${activeYear(year, mode)}`} value={activeTotal(data, mode).revenue} valueClass="text-emerald-700" />
            <SummaryStat label={`Koszty ${activeYear(year, mode)}`} value={activeTotal(data, mode).costs} valueClass="text-rose-700" />
            <SummaryStat label={`Dochód ${activeYear(year, mode)}`} value={activeTotal(data, mode).income} valueClass="text-brand-700" />
            <YoyStat delta={yoyDelta} year={year} />
          </div>
        </>
      )}
    </div>
  );
}

function activeYear(year: number, mode: ViewMode): number {
  return mode === "previous" ? year - 1 : year;
}

function activeTotal(data: CashflowResponse, mode: ViewMode) {
  if (mode === "previous" && data.previousYear) return data.previousYear.total;
  return data.total;
}

interface ChartPoint {
  label: string;
  revenue: number;
  costs: number;
  income: number;
  incomePrev?: number;
}

function buildChartData(data: CashflowResponse | undefined, mode: ViewMode): ChartPoint[] {
  if (!data) return [];
  const source = mode === "previous" && data.previousYear ? data.previousYear : data;
  return source.months.map((m, i) => {
    const prev = data.previousYear?.months[i];
    return {
      label: MONTH_LABELS[m.month - 1],
      revenue: parseFloat(m.revenue),
      costs: parseFloat(m.costs),
      income: parseFloat(m.income),
      incomePrev: prev ? parseFloat(prev.income) : undefined,
    };
  });
}

function shortMoney(value: number): string {
  if (Math.abs(value) >= 1_000_000) return `${(value / 1_000_000).toFixed(1)}M`;
  if (Math.abs(value) >= 1_000) return `${Math.round(value / 1_000)}k`;
  return String(Math.round(value));
}

interface TooltipPayload {
  active?: boolean;
  payload?: Array<{ name?: string; value?: number; color?: string }>;
  label?: string;
}

function MoneyTooltip({ active, payload, label }: TooltipPayload) {
  if (!active || !payload || payload.length === 0) return null;
  return (
    <div className="rounded-lg bg-white border border-slate-200 shadow-elevated px-3 py-2 text-xs dark:bg-slate-900 dark:border-slate-700">
      <p className="font-semibold text-slate-700 mb-1.5 dark:text-slate-200">{label}</p>
      {payload.map((entry, idx) => (
        <div key={idx} className="flex items-center justify-between gap-4 py-0.5">
          <span className="flex items-center gap-1.5">
            <span className="h-2 w-2 rounded-full" style={{ backgroundColor: entry.color }} />
            <span className="text-slate-600 dark:text-slate-300">{entry.name}</span>
          </span>
          <span className="font-medium text-slate-900 tabular-nums dark:text-white">{formatMoney(entry.value ?? 0)}</span>
        </div>
      ))}
    </div>
  );
}

function SummaryStat({ label, value, valueClass }: { label: string; value: string; valueClass: string }) {
  return (
    <div>
      <p className="text-[11px] font-medium text-slate-500 uppercase tracking-wide mb-1 dark:text-slate-400">{label}</p>
      <p className={`text-lg font-semibold tabular-nums ${valueClass}`}>{formatMoney(value)}</p>
    </div>
  );
}

function YoyStat({ delta, year }: { delta: number | null; year: number }) {
  if (delta === null) {
    return (
      <div>
        <p className="text-[11px] font-medium text-slate-500 uppercase tracking-wide mb-1 dark:text-slate-400">YoY dochodu</p>
        <p className="text-lg font-semibold text-slate-300 tabular-nums dark:text-slate-600">—</p>
      </div>
    );
  }
  const positive = delta >= 0;
  const Icon = positive ? ArrowUp : ArrowDown;
  const color = positive ? "text-emerald-700 dark:text-emerald-400" : "text-rose-700 dark:text-rose-400";
  const bg = positive ? "bg-emerald-50 dark:bg-emerald-500/10" : "bg-rose-50 dark:bg-rose-500/10";
  return (
    <div>
      <p className="text-[11px] font-medium text-slate-500 uppercase tracking-wide mb-1 dark:text-slate-400">
        YoY dochodu vs {year - 1}
      </p>
      <p className={`text-lg font-semibold tabular-nums flex items-center gap-1.5 ${color}`}>
        <span className={`inline-flex items-center justify-center h-5 w-5 rounded-full ${bg}`}>
          <Icon size={12} />
        </span>
        {positive ? "+" : ""}
        {delta.toFixed(1)}%
      </p>
    </div>
  );
}

function Card({ icon, iconClass, label, value, hint, valueClass = "text-slate-900 dark:text-white" }: {
  icon: React.ReactNode;
  iconClass: string;
  label: string;
  value: string;
  hint?: string;
  valueClass?: string;
}) {
  return (
    <div className="card-hover p-5 animate-slide-up">
      <div className="flex items-center justify-between mb-4">
        <div className={`h-9 w-9 rounded-lg flex items-center justify-center ${iconClass}`}>
          {icon}
        </div>
      </div>
      <p className="text-xs font-medium text-slate-500 uppercase tracking-wide mb-1.5 dark:text-slate-400">{label}</p>
      <p className={`text-2xl font-bold tabular-nums tracking-tight ${valueClass}`}>{value}</p>
      {hint && <p className="text-xs text-slate-400 mt-1 dark:text-slate-500">{hint}</p>}
    </div>
  );
}
