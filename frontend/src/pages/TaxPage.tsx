import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Calculator,
  Receipt,
  Landmark,
  HeartPulse,
  RefreshCw,
  CheckCircle2,
  AlertCircle,
  Calendar,
} from "lucide-react";
import { Link } from "react-router-dom";
import { taxApi } from "@/api/taxApi";
import type {
  ObligationType,
  TaxObligationResponse,
} from "@/types/api";
import { formatDate, formatMoney } from "@/lib/format";

const MONTH_NAMES = [
  "Styczeń", "Luty", "Marzec", "Kwiecień", "Maj", "Czerwiec",
  "Lipiec", "Sierpień", "Wrzesień", "Październik", "Listopad", "Grudzień",
];

const OBLIGATION_LABELS: Record<ObligationType, { label: string; tone: string }> = {
  ZUS_SOCIAL:  { label: "ZUS społeczny", tone: "bg-sky-50 text-sky-700 ring-1 ring-sky-600/20" },
  ZUS_HEALTH:  { label: "Zdrowotna",     tone: "bg-rose-50 text-rose-700 ring-1 ring-rose-600/20" },
  PIT_ADVANCE: { label: "Zaliczka PIT",  tone: "bg-violet-50 text-violet-700 ring-1 ring-violet-600/20" },
  VAT:         { label: "VAT (JPK)",     tone: "bg-amber-50 text-amber-700 ring-1 ring-amber-600/20" },
};

export default function TaxPage() {
  const queryClient = useQueryClient();
  const now = new Date();
  const currentYear = now.getFullYear();
  const [year, setYear] = useState(currentYear);
  const [selectedMonth, setSelectedMonth] = useState<number>(now.getMonth() + 1);

  const yearOptions = Array.from({ length: 4 }, (_, i) => currentYear - 2 + i);

  const yearQuery = useQuery({
    queryKey: ["tax-year", year],
    queryFn: () => taxApi.year(year),
  });

  const upcomingQuery = useQuery({
    queryKey: ["tax-upcoming"],
    queryFn: () => taxApi.upcomingObligations(),
  });

  const recomputeMutation = useMutation({
    mutationFn: (y: number) => taxApi.recompute(y),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ["tax-year", variables] });
      queryClient.invalidateQueries({ queryKey: ["tax-upcoming"] });
    },
  });

  const markPaidMutation = useMutation({
    mutationFn: ({ id }: { id: string }) => taxApi.markPaid(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tax-year", year] });
      queryClient.invalidateQueries({ queryKey: ["tax-upcoming"] });
    },
  });

  const periods = yearQuery.data ?? [];
  const ytd = useMemo(() => {
    return periods.reduce(
      (acc, p) => ({
        revenue: acc.revenue + parseFloat(p.revenue),
        costs: acc.costs + parseFloat(p.costs),
        zus: acc.zus + parseFloat(p.zusSocialTotal),
        health: acc.health + parseFloat(p.healthAmount),
        pit: acc.pit + parseFloat(p.pitAdvance),
        vat: acc.vat + parseFloat(p.vatDue),
      }),
      { revenue: 0, costs: 0, zus: 0, health: 0, pit: 0, vat: 0 },
    );
  }, [periods]);

  const selected = periods.find((p) => p.month === selectedMonth);

  const errMessage = (recomputeMutation.error as any)?.response?.data?.message;

  return (
    <div className="p-8 max-w-7xl mx-auto animate-fade-in">
      <header className="flex items-end justify-between mb-8">
        <div>
          <p className="text-xs font-semibold text-brand-600 uppercase tracking-wider mb-1 dark:text-brand-400">Podatki</p>
          <h1 className="text-3xl font-bold text-slate-900 dark:text-white">ZUS, PIT, VAT</h1>
          <p className="text-sm text-slate-500 mt-1.5 dark:text-slate-400">
            Prognozy składek i zaliczek liczone z Twoich faktur i wydatków.{" "}
            <Link to="/profile" className="text-brand-600 hover:underline font-medium">
              Skonfiguruj profil podatkowy →
            </Link>
          </p>
        </div>
        <div className="flex gap-2">
          <select
            value={year}
            onChange={(e) => { setYear(parseInt(e.target.value)); setSelectedMonth(now.getMonth() + 1); }}
            className="input max-w-[8rem]"
          >
            {yearOptions.map((y) => (
              <option key={y} value={y}>{y}</option>
            ))}
          </select>
          <button
            onClick={() => recomputeMutation.mutate(year)}
            disabled={recomputeMutation.isPending}
            className="btn-primary"
          >
            <RefreshCw size={16} className={recomputeMutation.isPending ? "animate-spin" : ""} />
            {recomputeMutation.isPending ? "Przeliczanie…" : `Przelicz ${year}`}
          </button>
        </div>
      </header>

      {recomputeMutation.isError && (
        <div className="mb-4 p-3.5 rounded-lg bg-rose-50 border border-rose-200 text-sm text-rose-700 flex items-start gap-2.5">
          <AlertCircle size={16} className="flex-shrink-0 mt-0.5" />
          <div>{errMessage ?? "Nie udało się przeliczyć"}</div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <Tile
          icon={<Calculator size={18} />}
          iconClass="bg-violet-50 text-violet-600"
          label="PIT (zaliczki)"
          value={formatMoney(ytd.pit)}
          hint={`narastająco ${year}`}
          valueClass="text-violet-600"
        />
        <Tile
          icon={<Receipt size={18} />}
          iconClass="bg-amber-50 text-amber-600"
          label="VAT należny"
          value={formatMoney(ytd.vat)}
          hint={`narastająco ${year}`}
          valueClass="text-amber-600"
        />
        <Tile
          icon={<Landmark size={18} />}
          iconClass="bg-sky-50 text-sky-600"
          label="ZUS społeczny"
          value={formatMoney(ytd.zus)}
          hint={`narastająco ${year}`}
          valueClass="text-sky-600"
        />
        <Tile
          icon={<HeartPulse size={18} />}
          iconClass="bg-rose-50 text-rose-600"
          label="Zdrowotna"
          value={formatMoney(ytd.health)}
          hint={`narastająco ${year}`}
          valueClass="text-rose-600"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 card overflow-hidden">
          <div className="px-5 py-4 border-b border-slate-100 dark:border-slate-800">
            <h3 className="font-semibold text-slate-900 dark:text-white">Miesięczne rozliczenia {year}</h3>
          </div>

          {periods.length === 0 ? (
            <div className="p-12 text-center">
              <div className="inline-flex h-14 w-14 rounded-2xl bg-slate-100 items-center justify-center mb-4 dark:bg-slate-800">
                <Calculator size={26} className="text-slate-400" />
              </div>
              <p className="text-slate-700 font-medium dark:text-slate-200">Brak rozliczeń dla {year}</p>
              <p className="text-sm text-slate-500 mt-1 dark:text-slate-400">Kliknij "Przelicz rok" żeby wygenerować prognozy.</p>
            </div>
          ) : (
            <table className="w-full">
              <thead className="bg-slate-50/80 border-b border-slate-200 dark:bg-slate-800/40 dark:border-slate-700">
                <tr className="text-left text-[11px] font-semibold text-slate-500 uppercase tracking-wider dark:text-slate-400">
                  <th className="px-4 py-3">Miesiąc</th>
                  <th className="px-4 py-3 text-right">Przychód</th>
                  <th className="px-4 py-3 text-right">Koszty</th>
                  <th className="px-4 py-3 text-right">ZUS</th>
                  <th className="px-4 py-3 text-right">Zdrow.</th>
                  <th className="px-4 py-3 text-right">PIT</th>
                  <th className="px-4 py-3 text-right">VAT</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {periods.map((p) => (
                  <tr
                    key={p.id}
                    onClick={() => setSelectedMonth(p.month)}
                    className={`cursor-pointer transition-colors ${
                      selectedMonth === p.month ? "bg-brand-50/60" : "hover:bg-slate-50/60"
                    }`}
                  >
                    <td className="px-4 py-3 text-sm font-medium text-slate-900 dark:text-slate-100">
                      {MONTH_NAMES[p.month - 1]}
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-700 dark:text-slate-300 text-right tabular-nums">
                      {formatMoney(p.revenue)}
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-500 dark:text-slate-400 text-right tabular-nums">
                      {formatMoney(p.costs)}
                    </td>
                    <td className="px-4 py-3 text-sm text-sky-700 text-right tabular-nums">
                      {formatMoney(p.zusSocialTotal)}
                    </td>
                    <td className="px-4 py-3 text-sm text-rose-700 text-right tabular-nums">
                      {formatMoney(p.healthAmount)}
                    </td>
                    <td className="px-4 py-3 text-sm font-semibold text-violet-700 text-right tabular-nums">
                      {formatMoney(p.pitAdvance)}
                    </td>
                    <td className="px-4 py-3 text-sm text-amber-700 text-right tabular-nums">
                      {formatMoney(p.vatDue)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <UpcomingDeadlines
          obligations={upcomingQuery.data ?? []}
          onMarkPaid={(id) => markPaidMutation.mutate({ id })}
          isMutating={markPaidMutation.isPending}
        />
      </div>

      {selected && selected.breakdown && (
        <div className="mt-8 card p-6 animate-slide-up">
          <h3 className="font-semibold text-slate-900 mb-6 dark:text-white">
            Szczegóły — {MONTH_NAMES[selected.month - 1]} {selected.year}
          </h3>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
            <BreakdownBlock
              icon={<Landmark size={16} />}
              tint="sky"
              title="ZUS społeczny"
              total={selected.breakdown.zus.total}
            >
              <p className="text-xs text-slate-500 mb-2 dark:text-slate-400">
                Tryb: <strong>{selected.breakdown.zus.mode}</strong>
              </p>
              <p className="text-xs text-slate-500 mb-3 dark:text-slate-400">
                Podstawa: <strong className="tabular-nums">{formatMoney(selected.breakdown.zus.base)}</strong>
              </p>
              <ul className="space-y-1.5">
                {Object.entries(selected.breakdown.zus.components).map(([component, amount]) => (
                  <li key={component} className="flex justify-between gap-3 text-sm">
                    <span className="text-slate-600 dark:text-slate-400 min-w-0 truncate">{componentLabel(component)}</span>
                    <span className="text-slate-900 dark:text-slate-100 font-medium tabular-nums shrink-0">
                      {formatMoney(amount as string)}
                    </span>
                  </li>
                ))}
              </ul>
            </BreakdownBlock>

            <BreakdownBlock
              icon={<HeartPulse size={16} />}
              tint="rose"
              title="Składka zdrowotna"
              total={selected.breakdown.health.total}
            >
              <BreakdownSteps steps={selected.breakdown.health.steps} />
            </BreakdownBlock>

            <BreakdownBlock
              icon={<Calculator size={16} />}
              tint="violet"
              title="Zaliczka PIT"
              total={selected.breakdown.pit.total}
            >
              <BreakdownSteps steps={selected.breakdown.pit.steps} />
            </BreakdownBlock>
          </div>
        </div>
      )}
    </div>
  );
}

const DEADLINES_PAGE_SIZE = 6;

function UpcomingDeadlines({ obligations, onMarkPaid, isMutating }: {
  obligations: TaxObligationResponse[];
  onMarkPaid: (id: string) => void;
  isMutating: boolean;
}) {
  const [page, setPage] = useState(0);
  const sorted = [...obligations].sort((a, b) => a.deadline.localeCompare(b.deadline));
  const unpaid = sorted.filter((o) => !o.isPaid);
  const today = new Date().toISOString().slice(0, 10);
  const totalPages = Math.ceil(unpaid.length / DEADLINES_PAGE_SIZE);
  const visible = unpaid.slice(page * DEADLINES_PAGE_SIZE, (page + 1) * DEADLINES_PAGE_SIZE);

  return (
    <div className="card overflow-hidden flex flex-col">
      <div className="px-5 py-4 border-b border-slate-100 flex items-center gap-2 dark:border-slate-800">
        <Calendar size={16} className="text-slate-400" />
        <h3 className="font-semibold text-slate-900 dark:text-white">Najbliższe deadliny</h3>
        {unpaid.length > 0 && (
          <span className="ml-auto text-xs text-slate-400 tabular-nums">{unpaid.length} otwartych</span>
        )}
      </div>

      {unpaid.length === 0 ? (
        <div className="p-8 text-center text-sm text-slate-500 dark:text-slate-400">
          Brak otwartych obowiązków.
        </div>
      ) : (
        <>
          <ul className="divide-y divide-slate-100 dark:divide-slate-800 flex-1">
            {visible.map((o) => {
              const isOverdue = o.deadline < today;
              return (
                <li key={o.id} className="px-5 py-3.5 flex items-center justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className={`pill ${OBLIGATION_LABELS[o.type].tone}`}>
                        {OBLIGATION_LABELS[o.type].label}
                      </span>
                    </div>
                    <p className={`text-xs mt-1.5 tabular-nums ${isOverdue ? "text-rose-600 font-medium" : "text-slate-500"}`}>
                      {isOverdue ? "Po terminie · " : "do "}{formatDate(o.deadline)}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="text-sm font-semibold text-slate-900 tabular-nums dark:text-slate-100">
                      {formatMoney(o.amount)}
                    </span>
                    <button
                      onClick={() => onMarkPaid(o.id)}
                      disabled={isMutating}
                      className="p-1.5 text-slate-400 hover:text-emerald-600 hover:bg-emerald-50 rounded-md transition-colors"
                      title="Zaznacz jako zapłacone"
                    >
                      <CheckCircle2 size={16} />
                    </button>
                  </div>
                </li>
              );
            })}
          </ul>
          {totalPages > 1 && (
            <div className="px-5 py-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="btn-secondary text-xs px-3 py-1.5"
              >
                Poprzednia
              </button>
              <span className="text-xs text-slate-500 tabular-nums dark:text-slate-400">
                {page + 1} / {totalPages}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="btn-secondary text-xs px-3 py-1.5"
              >
                Następna
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

function BreakdownBlock({ icon, tint, title, total, children }: {
  icon: React.ReactNode;
  tint: "sky" | "rose" | "violet";
  title: string;
  total: string;
  children: React.ReactNode;
}) {
  const tintMap = {
    sky:    { bg: "bg-sky-50",    text: "text-sky-700"    },
    rose:   { bg: "bg-rose-50",   text: "text-rose-700"   },
    violet: { bg: "bg-violet-50", text: "text-violet-700" },
  } as const;
  const t = tintMap[tint];

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5 min-w-0">
      <div className="flex items-center gap-2 mb-2">
        <span className={`h-7 w-7 shrink-0 rounded-lg flex items-center justify-center ${t.bg} ${t.text}`}>
          {icon}
        </span>
        <span className="text-xs font-semibold text-slate-500 uppercase tracking-wide truncate">{title}</span>
      </div>
      <p className={`text-xl font-bold tabular-nums mb-4 ${t.text}`}>{formatMoney(total)}</p>
      <div className="text-sm">{children}</div>
    </div>
  );
}

function BreakdownSteps({ steps }: { steps: { label: string; amount: string; formula: string | null }[] }) {
  return (
    <ul className="space-y-2">
      {steps.map((step, i) => (
        <li key={i} className="flex justify-between gap-3">
          <div className="min-w-0 flex-1">
            <span className="text-slate-600 text-sm break-words dark:text-slate-300">{step.label}</span>
            {step.formula && (
              <span className="block text-[11px] text-slate-400 tabular-nums break-words dark:text-slate-500">{step.formula}</span>
            )}
          </div>
          <span className="text-slate-900 dark:text-slate-100 font-medium text-sm tabular-nums shrink-0 whitespace-nowrap">
            {formatMoney(step.amount)}
          </span>
        </li>
      ))}
    </ul>
  );
}

function componentLabel(c: string): string {
  switch (c) {
    case "PENSION":    return "Emerytalna";
    case "DISABILITY": return "Rentowa";
    case "SICKNESS":   return "Chorobowa";
    case "ACCIDENT":   return "Wypadkowa";
    case "LABOR_FUND": return "Fundusz Pracy";
    default:           return c;
  }
}

function Tile({ icon, iconClass, label, value, hint, valueClass = "text-slate-900" }: {
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
      <p className="text-xs font-medium text-slate-500 uppercase tracking-wide mb-1.5">{label}</p>
      <p className={`text-2xl font-bold tabular-nums tracking-tight ${valueClass}`}>{value}</p>
      {hint && <p className="text-xs text-slate-400 mt-1">{hint}</p>}
    </div>
  );
}
