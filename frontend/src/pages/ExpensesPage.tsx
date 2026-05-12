import { useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  Plus,
  Trash2,
  Pencil,
  X,
  Receipt as ReceiptIcon,
  Filter,
  UploadCloud,
  ScanLine,
  CheckCircle2,
  AlertCircle,
  Loader2,
  Download,
} from "lucide-react";
import { expensesApi, type ExpensePayload, type ExpenseQuery } from "@/api/expensesApi";
import { receiptsApi, type ReceiptUploadResponse } from "@/api/receiptsApi";
import type { ExpenseResponse } from "@/types/api";
import { formatDate, formatMoney } from "@/lib/format";

const formSchema = z.object({
  categoryId: z.string().uuid("Wybierz kategorię"),
  amount: z
    .string()
    .regex(/^\d+([.,]\d{1,2})?$/, "Kwota np. 99.00")
    .refine((v) => parseFloat(v.replace(",", ".")) > 0, "Kwota musi być większa od 0"),
  currency: z.string().regex(/^[A-Z]{3}$/, "3-literowy kod waluty (PLN, EUR, USD)"),
  expenseDate: z.string().min(1, "Wybierz datę"),
  description: z.string().min(1, "Opis jest wymagany").max(500),
  vendor: z.string().max(255).optional(),
  vatDeductible: z.boolean(),
  vatAmount: z.string().optional(),
});

type FormValues = z.infer<typeof formSchema>;

const today = () => new Date().toISOString().slice(0, 10);

export default function ExpensesPage() {
  const queryClient = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<ExpenseResponse | null>(null);
  const [filterCategory, setFilterCategory] = useState<string>("");
  const [page, setPage] = useState(0);
  const [receiptUpload, setReceiptUpload] = useState<ReceiptUploadResponse | null>(null);

  const categoriesQuery = useQuery({
    queryKey: ["expense-categories"],
    queryFn: expensesApi.categories,
    staleTime: 5 * 60_000,
  });

  const query: ExpenseQuery = useMemo(
    () => ({
      page,
      size: 20,
      ...(filterCategory ? { categoryId: filterCategory } : {}),
    }),
    [page, filterCategory],
  );

  const expensesQuery = useQuery({
    queryKey: ["expenses", query],
    queryFn: () => expensesApi.list(query),
  });

  const createMutation = useMutation({
    mutationFn: expensesApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["expenses"] });
      reset(initialFormValues());
      setReceiptUpload(null);
      setShowForm(false);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: ExpensePayload }) =>
      expensesApi.update(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["expenses"] });
      reset(initialFormValues());
      setEditing(null);
      setShowForm(false);
    },
  });

  const uploadMutation = useMutation({
    mutationFn: receiptsApi.upload,
    onSuccess: (response) => {
      setReceiptUpload(response);
      applySuggestion(response);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: expensesApi.delete,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["expenses"] }),
  });

  const exportMutation = useMutation({
    mutationFn: () => expensesApi.exportCsv(filterCategory ? { categoryId: filterCategory } : {}),
  });

  const { register, handleSubmit, formState, reset, watch, setValue } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: initialFormValues(),
  });

  const isVatDeductible = watch("vatDeductible");

  const onSubmit = (values: FormValues) => {
    const payload: ExpensePayload = {
      categoryId: values.categoryId,
      amount: values.amount.replace(",", "."),
      currency: values.currency,
      expenseDate: values.expenseDate,
      description: values.description,
      vendor: values.vendor || null,
      vatDeductible: values.vatDeductible,
      vatAmount: values.vatDeductible && values.vatAmount ? values.vatAmount.replace(",", ".") : null,
      receiptId: !editing && receiptUpload ? receiptUpload.id : null,
    };

    if (editing) {
      updateMutation.mutate({ id: editing.id, payload });
    } else {
      createMutation.mutate(payload);
    }
  };

  const applySuggestion = (response: ReceiptUploadResponse) => {
    const s = response.suggested;
    if (!s) return;
    if (s.amount != null) setValue("amount", String(s.amount).replace(",", "."));
    if (s.currency) setValue("currency", s.currency.toUpperCase());
    if (s.expenseDate) setValue("expenseDate", s.expenseDate);
    if (s.description) setValue("description", s.description);
    if (s.vendor) setValue("vendor", s.vendor);
    if (s.suggestedCategoryId) setValue("categoryId", s.suggestedCategoryId);
    if (s.vatDeductible != null) setValue("vatDeductible", s.vatDeductible);
    if (s.vatAmount != null) setValue("vatAmount", String(s.vatAmount).replace(",", "."));
  };

  const startEdit = (expense: ExpenseResponse) => {
    setEditing(expense);
    setShowForm(true);
    setValue("categoryId", expense.category.id);
    setValue("amount", expense.amount);
    setValue("currency", expense.currency);
    setValue("expenseDate", expense.expenseDate);
    setValue("description", expense.description);
    setValue("vendor", expense.vendor ?? "");
    setValue("vatDeductible", expense.vatDeductible);
    setValue("vatAmount", expense.vatAmount ?? "");
  };

  const cancelEdit = () => {
    setEditing(null);
    setShowForm(false);
    setReceiptUpload(null);
    uploadMutation.reset();
    reset(initialFormValues());
  };

  const totalAmount = expensesQuery.data?.content.reduce(
    (sum, e) => sum + parseFloat(e.amount),
    0,
  ) ?? 0;

  return (
    <div className="p-8 max-w-7xl mx-auto animate-fade-in">
      <header className="flex items-end justify-between mb-8">
        <div>
          <p className="text-xs font-semibold text-brand-600 uppercase tracking-wider mb-1 dark:text-brand-400">Wydatki</p>
          <h1 className="text-3xl font-bold text-slate-900 dark:text-white">Rejestr wydatków</h1>
          <p className="text-sm text-slate-500 mt-1.5 dark:text-slate-400">
            Software, sprzęt, paliwo, biuro — wszystko co odliczasz od dochodu.
          </p>
        </div>
        {!showForm && (
          <div className="flex gap-2">
            <button
              onClick={() => exportMutation.mutate()}
              disabled={exportMutation.isPending || (expensesQuery.data?.totalElements ?? 0) === 0}
              className="btn-secondary"
              title="Pobierz CSV (z aktualnym filtrem)"
            >
              <Download size={16} className={exportMutation.isPending ? "animate-pulse" : ""} />
              {exportMutation.isPending ? "Generowanie…" : "CSV"}
            </button>
            <button onClick={() => setShowForm(true)} className="btn-primary">
              <Plus size={16} />
              Dodaj wydatek
            </button>
          </div>
        )}
      </header>

      {showForm && (
        <div className="card p-6 mb-6 animate-slide-up">
          <div className="flex items-center justify-between mb-5">
            <h2 className="text-lg font-semibold text-slate-900 dark:text-white">
              {editing ? "Edytuj wydatek" : "Nowy wydatek"}
            </h2>
            <button onClick={cancelEdit} className="text-slate-400 hover:text-slate-700 transition-colors dark:hover:text-slate-200">
              <X size={18} />
            </button>
          </div>

          {!editing && (
            <ReceiptDropZone
              upload={receiptUpload}
              isUploading={uploadMutation.isPending}
              uploadError={uploadMutation.error}
              onPickFile={(f) => uploadMutation.mutate(f)}
              onClear={() => {
                setReceiptUpload(null);
                uploadMutation.reset();
              }}
            />
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="grid grid-cols-2 gap-4">
            <div className="col-span-2 md:col-span-1">
              <label className="label">Kategoria</label>
              <select className="input" {...register("categoryId")}>
                <option value="">— wybierz —</option>
                {categoriesQuery.data?.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
              {formState.errors.categoryId && (
                <p className="text-xs text-rose-600 mt-1">{formState.errors.categoryId.message}</p>
              )}
            </div>

            <div className="col-span-2 md:col-span-1">
              <label className="label">Data</label>
              <input type="date" className="input" max={today()} {...register("expenseDate")} />
              {formState.errors.expenseDate && (
                <p className="text-xs text-rose-600 mt-1">{formState.errors.expenseDate.message}</p>
              )}
            </div>

            <div>
              <label className="label">Kwota</label>
              <input className="input" placeholder="0,00" {...register("amount")} />
              {formState.errors.amount && (
                <p className="text-xs text-rose-600 mt-1">{formState.errors.amount.message}</p>
              )}
            </div>

            <div>
              <label className="label">Waluta</label>
              <input className="input" placeholder="PLN" maxLength={3} {...register("currency", { setValueAs: (v) => v.toUpperCase() })} />
              {formState.errors.currency && (
                <p className="text-xs text-rose-600 mt-1">{formState.errors.currency.message}</p>
              )}
            </div>

            <div className="col-span-2">
              <label className="label">Opis</label>
              <input className="input" {...register("description")} />
              {formState.errors.description && (
                <p className="text-xs text-rose-600 mt-1">{formState.errors.description.message}</p>
              )}
            </div>

            <div className="col-span-2">
              <label className="label">Sprzedawca (opcjonalnie)</label>
              <input className="input" placeholder="Nazwa sprzedawcy" {...register("vendor")} />
            </div>

            <label className="col-span-2 flex items-center gap-3 p-3 rounded-lg bg-slate-50 border border-slate-100 cursor-pointer hover:bg-slate-100 transition-colors dark:bg-slate-800/40 dark:border-slate-700 dark:hover:bg-slate-800">
              <input type="checkbox" className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500" {...register("vatDeductible")} />
              <span className="text-sm text-slate-700 dark:text-slate-300">VAT odliczalny (faktura VAT-owa)</span>
            </label>

            {isVatDeductible && (
              <div className="col-span-2">
                <label className="label">Kwota VAT</label>
                <input className="input" placeholder="0,00" {...register("vatAmount")} />
              </div>
            )}

            {(createMutation.isError || updateMutation.isError) && (
              <div className="col-span-2 p-3 rounded-lg bg-rose-50 border border-rose-200 text-sm text-rose-700">
                {((createMutation.error ?? updateMutation.error) as any)?.response?.data?.message ?? "Błąd zapisu"}
              </div>
            )}

            <div className="col-span-2 flex justify-end gap-2 pt-2">
              <button type="button" onClick={cancelEdit} className="btn-secondary">Anuluj</button>
              <button
                type="submit"
                disabled={createMutation.isPending || updateMutation.isPending}
                className="btn-primary"
              >
                {createMutation.isPending || updateMutation.isPending ? "Zapisywanie…" : "Zapisz"}
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="flex items-center gap-3 mb-4">
        <div className="relative">
          <Filter size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
          <select
            value={filterCategory}
            onChange={(e) => { setFilterCategory(e.target.value); setPage(0); }}
            className="input pl-9 max-w-xs"
          >
            <option value="">Wszystkie kategorie</option>
            {categoriesQuery.data?.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
        </div>

        <span className="ml-auto text-sm text-slate-500 dark:text-slate-400">
          Suma na stronie:{" "}
          <strong className="text-slate-900 tabular-nums dark:text-slate-100">{formatMoney(totalAmount)}</strong>
        </span>
      </div>

      <div className="card overflow-hidden">
        {expensesQuery.isLoading && (
          <div className="p-12 text-center text-slate-500 dark:text-slate-400">Ładowanie…</div>
        )}

        {expensesQuery.data && expensesQuery.data.content.length === 0 && (
          <div className="p-16 text-center">
            <div className="inline-flex h-14 w-14 rounded-2xl bg-slate-100 items-center justify-center mb-4 dark:bg-slate-800">
              <ReceiptIcon size={26} className="text-slate-400" />
            </div>
            <p className="text-slate-700 font-medium dark:text-slate-200">Brak wydatków</p>
            <p className="text-sm text-slate-500 mt-1 dark:text-slate-400">Kliknij "Dodaj wydatek" żeby zacząć.</p>
          </div>
        )}

        {expensesQuery.data && expensesQuery.data.content.length > 0 && (
          <>
            <table className="w-full">
              <thead className="bg-slate-50/80 border-b border-slate-200 dark:bg-slate-800/40 dark:border-slate-700">
                <tr className="text-left text-[11px] font-semibold text-slate-500 uppercase tracking-wider dark:text-slate-400">
                  <th className="px-4 py-3">Data</th>
                  <th className="px-4 py-3">Opis</th>
                  <th className="px-4 py-3">Kategoria</th>
                  <th className="px-4 py-3">Sprzedawca</th>
                  <th className="px-4 py-3 text-right">Kwota</th>
                  <th className="px-4 py-3">VAT</th>
                  <th className="px-4 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {expensesQuery.data.content.map((e) => (
                  <tr key={e.id} className="hover:bg-slate-50/60 transition-colors dark:hover:bg-slate-800/40">
                    <td className="px-4 py-3.5 text-sm text-slate-600 dark:text-slate-400 tabular-nums">{formatDate(e.expenseDate)}</td>
                    <td className="px-4 py-3.5 text-sm font-medium text-slate-900 dark:text-slate-100">{e.description}</td>
                    <td className="px-4 py-3.5">
                      <span
                        className="pill"
                        style={{
                          backgroundColor: (e.category.color ?? "#64748b") + "18",
                          color: e.category.color ?? "#64748b",
                          boxShadow: `inset 0 0 0 1px ${(e.category.color ?? "#64748b")}33`,
                        }}
                      >
                        {e.category.name}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 text-sm text-slate-600 dark:text-slate-400">{e.vendor ?? "—"}</td>
                    <td className="px-4 py-3.5 text-sm font-semibold text-slate-900 dark:text-slate-100 text-right tabular-nums">
                      {formatMoney(e.amount, e.currency)}
                    </td>
                    <td className="px-4 py-3.5 text-sm">
                      {e.vatDeductible ? (
                        <span className="text-emerald-700 tabular-nums">
                          {e.vatAmount ? formatMoney(e.vatAmount, e.currency) : "tak"}
                        </span>
                      ) : (
                        <span className="text-slate-300">—</span>
                      )}
                    </td>
                    <td className="px-4 py-3.5">
                      <div className="flex gap-1">
                        <button
                          onClick={() => startEdit(e)}
                          className="p-1.5 text-slate-400 hover:text-brand-600 hover:bg-brand-50 rounded-md transition-colors"
                          title="Edytuj"
                        >
                          <Pencil size={14} />
                        </button>
                        <button
                          onClick={() => {
                            if (confirm(`Usunąć: "${e.description}"?`)) deleteMutation.mutate(e.id);
                          }}
                          className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-md transition-colors"
                          title="Usuń"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div className="px-4 py-3 border-t border-slate-100 flex items-center justify-between text-sm text-slate-600 dark:border-slate-800 dark:text-slate-400">
              <span>
                {expensesQuery.data.totalElements}{" "}
                {expensesQuery.data.totalElements === 1 ? "wydatek" : "wydatków"}
              </span>
              {expensesQuery.data.totalPages > 1 && (
                <div className="flex gap-2 items-center">
                  <button
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="btn-secondary text-xs px-3 py-1.5"
                  >
                    Poprzednia
                  </button>
                  <span className="px-3 text-slate-500 tabular-nums dark:text-slate-400">
                    {page + 1} / {expensesQuery.data.totalPages}
                  </span>
                  <button
                    onClick={() => setPage((p) => Math.min(expensesQuery.data!.totalPages - 1, p + 1))}
                    disabled={page >= expensesQuery.data.totalPages - 1}
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

function initialFormValues(): FormValues {
  return {
    categoryId: "",
    amount: "",
    currency: "PLN",
    expenseDate: today(),
    description: "",
    vendor: "",
    vatDeductible: false,
    vatAmount: "",
  };
}

const ACCEPTED_MIME = "image/jpeg,image/png,application/pdf";
const MAX_FILE_SIZE = 10 * 1024 * 1024;

interface ReceiptDropZoneProps {
  upload: ReceiptUploadResponse | null;
  isUploading: boolean;
  uploadError: unknown;
  onPickFile: (file: File) => void;
  onClear: () => void;
}

function ReceiptDropZone({ upload, isUploading, uploadError, onPickFile, onClear }: ReceiptDropZoneProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragActive, setDragActive] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);

  const handleFile = (file: File | null | undefined) => {
    if (!file) return;
    if (file.size > MAX_FILE_SIZE) {
      setLocalError("Plik jest większy niż 10 MB");
      return;
    }
    if (!ACCEPTED_MIME.split(",").includes(file.type)) {
      setLocalError("Dozwolone formaty: JPG, PNG, PDF");
      return;
    }
    setLocalError(null);
    onPickFile(file);
  };

  if (upload && upload.status === "PROCESSED") {
    const confidencePct = upload.suggested?.confidence != null
      ? Math.round(upload.suggested.confidence * 100)
      : null;
    return (
      <div className="mb-5 p-4 rounded-xl border border-emerald-200 bg-emerald-50/60 flex items-start gap-3 animate-fade-in">
        <CheckCircle2 size={20} className="text-emerald-600 mt-0.5 shrink-0" />
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <p className="text-sm font-semibold text-emerald-900">Paragon przeanalizowany</p>
            {confidencePct != null && (
              <span className="text-[11px] font-medium px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800">
                {confidencePct}% pewności
              </span>
            )}
          </div>
          <p className="text-xs text-emerald-800/80 mt-0.5 truncate">
            {upload.originalFilename ?? "paragon"} · pola formularza wstępnie wypełnione
          </p>
        </div>
        <button
          type="button"
          onClick={onClear}
          className="text-emerald-700/70 hover:text-emerald-900 transition-colors"
          title="Odepnij paragon"
        >
          <X size={16} />
        </button>
      </div>
    );
  }

  if (upload && upload.status === "FAILED") {
    return (
      <div className="mb-5 p-4 rounded-xl border border-amber-200 bg-amber-50/60 flex items-start gap-3 animate-fade-in">
        <AlertCircle size={20} className="text-amber-600 mt-0.5 shrink-0" />
        <div className="flex-1 min-w-0">
          <p className="text-sm font-semibold text-amber-900">
            Nie udało się odczytać paragonu — uzupełnij ręcznie
          </p>
          <p className="text-xs text-amber-800/80 mt-0.5">
            {upload.errorMessage ?? "OCR niedostępne"} · plik został zapisany i będzie powiązany z wydatkiem.
          </p>
        </div>
        <button
          type="button"
          onClick={onClear}
          className="text-amber-700/70 hover:text-amber-900 transition-colors"
          title="Odepnij paragon"
        >
          <X size={16} />
        </button>
      </div>
    );
  }

  if (isUploading) {
    return (
      <div className="mb-5 p-6 rounded-xl border-2 border-dashed border-brand-300 bg-brand-50/40 flex items-center justify-center gap-3 animate-fade-in">
        <Loader2 size={20} className="animate-spin text-brand-600" />
        <p className="text-sm font-medium text-brand-900">Analizuję paragon… (do 10 sekund)</p>
      </div>
    );
  }

  const errorMessage =
    localError ??
    (uploadError ? ((uploadError as any)?.response?.data?.message ?? "Upload nie powiódł się") : null);

  return (
    <div
      className={`mb-5 p-6 rounded-xl border-2 border-dashed transition-colors cursor-pointer ${
        dragActive
          ? "border-brand-500 bg-brand-50"
          : "border-slate-300 hover:border-brand-400 hover:bg-brand-50/40"
      }`}
      onClick={() => inputRef.current?.click()}
      onDragOver={(e) => {
        e.preventDefault();
        setDragActive(true);
      }}
      onDragLeave={() => setDragActive(false)}
      onDrop={(e) => {
        e.preventDefault();
        setDragActive(false);
        handleFile(e.dataTransfer.files?.[0]);
      }}
    >
      <input
        ref={inputRef}
        type="file"
        accept={ACCEPTED_MIME}
        className="hidden"
        onChange={(e) => handleFile(e.target.files?.[0])}
      />
      <div className="flex items-center gap-4">
        <div className="h-11 w-11 shrink-0 rounded-xl bg-brand-100 text-brand-700 flex items-center justify-center">
          <ScanLine size={20} />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-semibold text-slate-900 flex items-center gap-2 dark:text-slate-100">
            Wrzuć paragon — wypełnię formularz za Ciebie
          </p>
          <p className="text-xs text-slate-500 mt-0.5 dark:text-slate-400">
            Przeciągnij plik tutaj lub kliknij. JPG, PNG, PDF do 10 MB.
          </p>
        </div>
        <UploadCloud size={18} className="text-slate-400 hidden sm:block" />
      </div>
      {errorMessage && (
        <p className="mt-3 text-xs text-rose-600">{errorMessage}</p>
      )}
    </div>
  );
}
