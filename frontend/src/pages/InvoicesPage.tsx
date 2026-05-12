import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { RefreshCw, AlertCircle, FileText, CheckCircle2, Download } from "lucide-react";
import { invoicesApi, type InvoiceQuery } from "@/api/invoicesApi";
import type { InvoiceStatus } from "@/types/api";
import { formatDate, formatMoney, STATUS_LABELS } from "@/lib/format";

const STATUS_OPTIONS: Array<{ value: InvoiceStatus | "ALL"; label: string }> = [
  { value: "ALL",     label: "Wszystkie" },
  { value: "PAID",    label: "Opłacone" },
  { value: "SENT",    label: "Wysłane" },
  { value: "PARTIAL", label: "Częściowe" },
  { value: "OVERDUE", label: "Zaległe" },
  { value: "DRAFT",   label: "Szkice" },
];

export default function InvoicesPage() {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<InvoiceStatus | "ALL">("ALL");
  const [page, setPage] = useState(0);

  const query: InvoiceQuery = {
    page,
    size: 20,
    ...(filter !== "ALL" ? { status: filter } : {}),
  };

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["invoices", query],
    queryFn: () => invoicesApi.list(query),
  });

  const syncMutation = useMutation({
    mutationFn: invoicesApi.sync,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["invoices"] }),
  });

  const exportMutation = useMutation({
    mutationFn: () => invoicesApi.exportCsv(filter !== "ALL" ? { status: filter } : {}),
  });

  return (
    <div className="p-8 max-w-7xl mx-auto animate-fade-in">
      <header className="flex items-end justify-between mb-8">
        <div>
          <p className="text-xs font-semibold text-brand-600 uppercase tracking-wider mb-1 dark:text-brand-400">Faktury</p>
          <h1 className="text-3xl font-bold text-slate-900 dark:text-white">Faktury sprzedażowe</h1>
          <p className="text-sm text-slate-500 mt-1.5 dark:text-slate-400">
            Synchronizowane z Fakturowni — kliknij "Synchronizuj" aby pobrać nowe.
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => exportMutation.mutate()}
            disabled={exportMutation.isPending || (data?.totalElements ?? 0) === 0}
            className="btn-secondary"
            title="Pobierz CSV (z aktualnym filtrem)"
          >
            <Download size={16} className={exportMutation.isPending ? "animate-pulse" : ""} />
            {exportMutation.isPending ? "Generowanie…" : "CSV"}
          </button>
          <button
            onClick={() => syncMutation.mutate()}
            disabled={syncMutation.isPending}
            className="btn-primary"
          >
            <RefreshCw size={16} className={syncMutation.isPending ? "animate-spin" : ""} />
            {syncMutation.isPending ? "Synchronizowanie…" : "Synchronizuj"}
          </button>
        </div>
      </header>

      {syncMutation.isSuccess && (
        <div className="mb-4 p-3.5 rounded-lg bg-emerald-50 border border-emerald-200 text-sm text-emerald-800 flex items-start gap-2.5 animate-slide-up">
          <CheckCircle2 size={16} className="flex-shrink-0 mt-0.5 text-emerald-600" />
          <div>
            Sync zakończony — pobrano <strong>{syncMutation.data.created}</strong> nowych,
            zaktualizowano <strong>{syncMutation.data.updated}</strong>.
          </div>
        </div>
      )}

      {syncMutation.isError && (
        <div className="mb-4 p-3.5 rounded-lg bg-rose-50 border border-rose-200 text-sm text-rose-700 flex items-start gap-2.5 animate-slide-up">
          <AlertCircle size={16} className="flex-shrink-0 mt-0.5" />
          <div>
            Sync nie powiódł się: {(syncMutation.error as any)?.response?.data?.message ?? "błąd serwera"}
          </div>
        </div>
      )}

      <div className="flex gap-2 mb-4 flex-wrap">
        {STATUS_OPTIONS.map((opt) => (
          <button
            key={opt.value}
            onClick={() => { setFilter(opt.value); setPage(0); }}
            className={`px-3.5 py-1.5 rounded-full text-sm font-medium transition-all duration-150 ${
              filter === opt.value
                ? "bg-brand-600 text-white shadow-soft"
                : "bg-white text-slate-600 border border-slate-200 hover:bg-slate-50 hover:border-slate-300"
            }`}
          >
            {opt.label}
          </button>
        ))}
      </div>

      <div className="card overflow-hidden">
        {isLoading && <div className="p-12 text-center text-slate-500 dark:text-slate-400">Ładowanie…</div>}

        {isError && (
          <div className="p-8 text-center text-rose-700">
            Nie udało się pobrać faktur: {(error as any)?.message}
          </div>
        )}

        {data && data.content.length === 0 && (
          <div className="p-16 text-center">
            <div className="inline-flex h-14 w-14 rounded-2xl bg-slate-100 items-center justify-center mb-4 dark:bg-slate-800">
              <FileText size={26} className="text-slate-400" />
            </div>
            <p className="text-slate-700 font-medium dark:text-slate-200">Brak faktur</p>
            <p className="text-sm text-slate-500 mt-1 dark:text-slate-400">Kliknij "Synchronizuj" żeby pobrać z Fakturowni.</p>
          </div>
        )}

        {data && data.content.length > 0 && (
          <>
            <table className="w-full">
              <thead className="bg-slate-50/80 border-b border-slate-200 dark:bg-slate-800/40 dark:border-slate-700">
                <tr className="text-left text-[11px] font-semibold text-slate-500 uppercase tracking-wider dark:text-slate-400">
                  <th className="px-4 py-3">Numer</th>
                  <th className="px-4 py-3">Nabywca</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">Netto</th>
                  <th className="px-4 py-3 text-right">Brutto</th>
                  <th className="px-4 py-3">Wystawienia</th>
                  <th className="px-4 py-3">Płatności</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {data.content.map((inv) => {
                  const status = STATUS_LABELS[inv.status] ?? { label: inv.status, className: "bg-slate-100 text-slate-700" };
                  return (
                    <tr key={inv.id} className="hover:bg-slate-50/60 transition-colors dark:hover:bg-slate-800/40">
                      <td className="px-4 py-3.5 text-sm font-semibold text-slate-900 dark:text-slate-100">{inv.number}</td>
                      <td className="px-4 py-3.5 text-sm text-slate-700 dark:text-slate-300">
                        {inv.buyerName ?? "—"}
                        {inv.buyerNip && <span className="text-xs text-slate-400 block tabular-nums">NIP: {inv.buyerNip}</span>}
                      </td>
                      <td className="px-4 py-3.5">
                        <span className={`pill ${status.className}`}>
                          {status.label}
                        </span>
                      </td>
                      <td className="px-4 py-3.5 text-sm text-slate-700 dark:text-slate-300 text-right tabular-nums">
                        {formatMoney(inv.netAmount, inv.currency)}
                      </td>
                      <td className="px-4 py-3.5 text-sm font-semibold text-slate-900 dark:text-slate-100 text-right tabular-nums">
                        {formatMoney(inv.grossAmount, inv.currency)}
                      </td>
                      <td className="px-4 py-3.5 text-sm text-slate-600 dark:text-slate-400 tabular-nums">{formatDate(inv.issueDate)}</td>
                      <td className="px-4 py-3.5 text-sm text-slate-600 dark:text-slate-400 tabular-nums">{formatDate(inv.paymentDate)}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>

            <div className="px-4 py-3 border-t border-slate-100 flex items-center justify-between text-sm text-slate-600 dark:border-slate-800 dark:text-slate-400">
              <span>
                {data.totalElements}{" "}
                {data.totalElements === 1 ? "faktura" : "faktur"} łącznie
              </span>
              {data.totalPages > 1 && (
                <div className="flex gap-2 items-center">
                  <button
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="btn-secondary text-xs px-3 py-1.5"
                  >
                    Poprzednia
                  </button>
                  <span className="px-3 text-slate-500 tabular-nums dark:text-slate-400">
                    {page + 1} / {data.totalPages}
                  </span>
                  <button
                    onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
                    disabled={page >= data.totalPages - 1}
                    className="btn-secondary text-xs px-3 py-1.5"
                  >
                    Następna
                  </button>
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}
