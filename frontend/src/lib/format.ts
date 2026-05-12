export function formatMoney(amount: string | number, currency: string = "PLN"): string {
  const value = typeof amount === "string" ? parseFloat(amount) : amount;
  return new Intl.NumberFormat("pl-PL", {
    style: "currency",
    currency,
    minimumFractionDigits: 2,
  }).format(value);
}

export function formatDate(iso?: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("pl-PL", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export const STATUS_LABELS: Record<string, { label: string; className: string }> = {
  PAID:      { label: "Opłacona",  className: "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-600/20" },
  SENT:      { label: "Wysłana",   className: "bg-sky-50 text-sky-700 ring-1 ring-sky-600/20" },
  PARTIAL:   { label: "Częściowa", className: "bg-amber-50 text-amber-700 ring-1 ring-amber-600/20" },
  OVERDUE:   { label: "Zaległa",   className: "bg-rose-50 text-rose-700 ring-1 ring-rose-600/20" },
  DRAFT:     { label: "Szkic",     className: "bg-slate-100 text-slate-700 ring-1 ring-slate-600/15" },
  CANCELLED: { label: "Anulowana", className: "bg-slate-100 text-slate-500 ring-1 ring-slate-400/15" },
};
