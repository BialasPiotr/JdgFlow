import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { UserPlus, Sparkles } from "lucide-react";
import { authApi, type RegisterPayload } from "@/api/authApi";
import { useAuthStore } from "@/auth/authStore";
import ThemeToggle from "@/theme/ThemeToggle";

const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

const schema = z.object({
  fullName: z.string().min(1, "Wpisz imię i nazwisko").max(255),
  email: z
    .string()
    .max(255)
    .regex(EMAIL_REGEX, "Email musi zawierać @ oraz prawidłową domenę"),
  password: z.string().min(12, "Hasło musi mieć min. 12 znaków").max(128),
});

export default function RegisterPage() {
  const navigate = useNavigate();
  const setSession = useAuthStore((s) => s.setSession);
  const [serverError, setServerError] = useState<string | null>(null);

  const { register, handleSubmit, formState: { errors } } = useForm<RegisterPayload>({
    resolver: zodResolver(schema),
  });

  const mutation = useMutation({
    mutationFn: authApi.register,
    onSuccess: (data) => {
      setSession({ token: data.token, user: data.user, expiresAt: data.expiresAt });
      navigate("/dashboard");
    },
    onError: (error: any) => {
      const data = error.response?.data;
      const violations = data?.violations as Array<{ field: string; message: string }> | undefined;
      if (violations && violations.length > 0) {
        setServerError(violations.map((v) => `${v.field}: ${v.message}`).join("\n"));
      } else {
        setServerError(data?.message ?? "Nie udało się utworzyć konta");
      }
    },
  });

  return (
    <div className="min-h-full flex items-center justify-center px-4 py-12 relative">
      <div className="absolute top-4 right-4">
        <ThemeToggle />
      </div>
      <div className="w-full max-w-md animate-fade-in">
        <div className="text-center mb-8">
          <div className="inline-flex h-14 w-14 rounded-2xl bg-gradient-to-br from-brand-500 to-brand-700 items-center justify-center shadow-glow mb-4">
            <Sparkles size={26} className="text-white" />
          </div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight dark:text-white">JDGFlow</h1>
          <p className="text-sm text-slate-500 mt-1 dark:text-slate-400">Utwórz konto</p>
        </div>

        <div className="card p-7">
          <form onSubmit={handleSubmit((data) => mutation.mutate(data))} className="space-y-4">
            <div>
              <label className="label">Imię i nazwisko</label>
              <input className="input" {...register("fullName")} />
              {errors.fullName && <p className="text-xs text-rose-600 mt-1">{errors.fullName.message}</p>}
            </div>

            <div>
              <label className="label">Email</label>
              <input type="email" className="input" autoComplete="email" {...register("email")} />
              {errors.email && <p className="text-xs text-rose-600 mt-1">{errors.email.message}</p>}
            </div>

            <div>
              <label className="label">Hasło</label>
              <input type="password" className="input" autoComplete="new-password" {...register("password")} />
              {errors.password && <p className="text-xs text-rose-600 mt-1">{errors.password.message}</p>}
            </div>

            {serverError && (
              <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-sm text-rose-700 whitespace-pre-line">
                {serverError}
              </div>
            )}

            <button type="submit" disabled={mutation.isPending} className="btn-primary w-full">
              <UserPlus size={16} />
              {mutation.isPending ? "Tworzenie konta…" : "Utwórz konto"}
            </button>
          </form>
        </div>

        <p className="text-center text-sm text-slate-500 mt-6 dark:text-slate-400">
          Masz już konto?{" "}
          <Link to="/login" className="text-brand-600 hover:text-brand-700 font-semibold transition-colors">
            Zaloguj się
          </Link>
        </p>
      </div>
    </div>
  );
}
