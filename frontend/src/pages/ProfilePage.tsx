import { useEffect } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { Save, CheckCircle2 } from "lucide-react";
import { taxApi } from "@/api/taxApi";
import type {
  AccountingMethod,
  TaxForm,
  TaxProfileResponse,
  UpdateTaxProfileRequest,
  ZusMode,
} from "@/types/api";

const ZUS_MODE_LABELS: Record<ZusMode, string> = {
  ULGA_NA_START: "Ulga na start (pierwsze 6 mies.)",
  PREFERENTIAL: "Składki preferencyjne (24 mies.)",
  MALY_ZUS_PLUS: "Mały ZUS Plus (proporcjonalny)",
  STANDARD: "Duży ZUS (standardowy)",
};

const TAX_FORM_LABELS: Record<TaxForm, string> = {
  SCALE: "Skala podatkowa (12% / 32%)",
  LINEAR: "Podatek liniowy (19%)",
  RYCZALT: "Ryczałt ewidencjonowany",
};

const ACCOUNTING_LABELS: Record<AccountingMethod, string> = {
  ACCRUAL: "Memoriałowo (po dacie wystawienia)",
  CASH: "Kasowo (po dacie zapłaty)",
};

export default function ProfilePage() {
  const queryClient = useQueryClient();

  const profileQuery = useQuery({
    queryKey: ["tax-profile"],
    queryFn: taxApi.profile,
  });

  const { register, handleSubmit, watch, reset, formState } = useForm<UpdateTaxProfileRequest>();

  useEffect(() => {
    if (profileQuery.data) reset(toFormDefaults(profileQuery.data));
  }, [profileQuery.data, reset]);

  const mutation = useMutation({
    mutationFn: taxApi.updateProfile,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tax-profile"] });
      queryClient.invalidateQueries({ queryKey: ["tax-year"] });
    },
  });

  const zusMode = watch("zusMode");

  const onSubmit = (values: UpdateTaxProfileRequest) => {
    mutation.mutate({
      ...values,
      previousYearRevenue: values.previousYearRevenue
        ? String(values.previousYearRevenue).replace(",", ".")
        : null,
      previousYearIncome: values.previousYearIncome
        ? String(values.previousYearIncome).replace(",", ".")
        : null,
    });
  };

  if (profileQuery.isLoading) {
    return <div className="p-8 text-slate-500 dark:text-slate-400">Ładowanie…</div>;
  }

  return (
    <div className="p-8 max-w-3xl mx-auto animate-fade-in">
      <header className="mb-8">
        <p className="text-xs font-semibold text-brand-600 uppercase tracking-wider mb-1 dark:text-brand-400">Ustawienia</p>
        <h1 className="text-3xl font-bold text-slate-900 dark:text-white">Profil podatkowy</h1>
        <p className="text-sm text-slate-500 mt-1.5 dark:text-slate-400">
          Te ustawienia napędzają kalkulator ZUS / PIT. Zmień, kliknij Zapisz, potem odśwież widok podatków.
        </p>
      </header>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <section className="card p-6 space-y-4 animate-slide-up">
          <h2 className="text-base font-semibold text-slate-900 dark:text-white">Forma opodatkowania i ZUS</h2>

          <div>
            <label className="label">Forma opodatkowania</label>
            <select className="input" {...register("taxForm")}>
              {Object.entries(TAX_FORM_LABELS).map(([k, v]) => (
                <option key={k} value={k}>{v}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="label">Tryb ZUS</label>
            <select className="input" {...register("zusMode")}>
              {Object.entries(ZUS_MODE_LABELS).map(([k, v]) => (
                <option key={k} value={k}>{v}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="label">Metoda rozliczania przychodu</label>
            <select className="input" {...register("accountingMethod")}>
              {Object.entries(ACCOUNTING_LABELS).map(([k, v]) => (
                <option key={k} value={k}>{v}</option>
              ))}
            </select>
          </div>
        </section>

        <section className="card p-6 space-y-4 animate-slide-up">
          <h2 className="text-base font-semibold text-slate-900 dark:text-white">Opcje dodatkowe</h2>

          <Toggle label="VAT-owiec (czynny podatnik VAT)" {...register("vatPayer")} />
          <Toggle label="Dobrowolna składka chorobowa" {...register("voluntarySickness")} />
          <Toggle label="IP BOX (preferencyjna stawka 5% PIT)" {...register("ipBoxEligible")} />
          <Toggle label="Wspólne rozliczenie ze współmałżonkiem" {...register("jointSettlement")} />
        </section>

        <section className="card p-6 space-y-4 animate-slide-up">
          <h2 className="text-base font-semibold text-slate-900 dark:text-white">Dane historyczne</h2>

          <div>
            <label className="label">Data rozpoczęcia działalności</label>
            <input type="date" className="input" {...register("businessStartDate")} />
          </div>

          <div>
            <label className="label">Przychód z poprzedniego roku (PLN)</label>
            <input
              className="input"
              placeholder="0,00"
              {...register("previousYearRevenue")}
            />
            <p className="text-xs text-slate-500 mt-1.5 dark:text-slate-400">
              Używane do weryfikacji uprawnień Małego ZUS Plus (limit 120 000 zł).
            </p>
          </div>

          {zusMode === "MALY_ZUS_PLUS" && (
            <div>
              <label className="label">
                Średni miesięczny dochód z poprzedniego roku (PLN)
                <span className="ml-2 text-rose-600">*</span>
              </label>
              <input
                className="input"
                placeholder="0,00"
                {...register("previousYearIncome")}
              />
              <p className="text-xs text-slate-500 mt-1.5 dark:text-slate-400">
                Wymagane dla Małego ZUS Plus. Podstawa składek = 50% × średni dochód, ograniczona do widełek.
              </p>
            </div>
          )}
        </section>

        {mutation.isError && (
          <div className="p-3.5 rounded-lg bg-rose-50 border border-rose-200 text-sm text-rose-700">
            {(mutation.error as any)?.response?.data?.message ?? "Nie udało się zapisać"}
          </div>
        )}

        {mutation.isSuccess && (
          <div className="p-3.5 rounded-lg bg-emerald-50 border border-emerald-200 text-sm text-emerald-800 flex items-center gap-2">
            <CheckCircle2 size={16} className="text-emerald-600" />
            Profil zapisany.
          </div>
        )}

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={mutation.isPending || !formState.isDirty}
            className="btn-primary"
          >
            <Save size={16} />
            {mutation.isPending ? "Zapisywanie…" : "Zapisz profil"}
          </button>
        </div>
      </form>
    </div>
  );
}

const Toggle = ({ label, ...rest }: any) => (
  <label className="flex items-center gap-3 p-3 rounded-lg bg-slate-50 border border-slate-100 cursor-pointer hover:bg-slate-100 transition-colors dark:bg-slate-800/40 dark:border-slate-700 dark:hover:bg-slate-800">
    <input type="checkbox" className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500" {...rest} />
    <span className="text-sm text-slate-700 dark:text-slate-300">{label}</span>
  </label>
);

function toFormDefaults(p: TaxProfileResponse): UpdateTaxProfileRequest {
  return {
    zusMode: p.zusMode,
    taxForm: p.taxForm,
    vatPayer: p.vatPayer,
    accountingMethod: p.accountingMethod,
    voluntarySickness: p.voluntarySickness,
    ipBoxEligible: p.ipBoxEligible,
    jointSettlement: p.jointSettlement,
    businessStartDate: p.businessStartDate,
    previousYearRevenue: p.previousYearRevenue,
    previousYearIncome: p.previousYearIncome,
  };
}
