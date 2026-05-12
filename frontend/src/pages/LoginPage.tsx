import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { LogIn, Sparkles } from "lucide-react";
import { authApi, type LoginPayload } from "@/api/authApi";
import { useAuthStore } from "@/auth/authStore";
import ThemeToggle from "@/theme/ThemeToggle";

const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

const schema = z.object({
  email: z
    .string()
    .max(255)
    .regex(EMAIL_REGEX, "Email musi zawierać @ oraz prawidłową domenę"),
  password: z.string().min(1, "Hasło jest wymagane").max(128),
});

export default function LoginPage() {
  const navigate = useNavigate();
  const setSession = useAuthStore((s) => s.setSession);
  const [serverError, setServerError] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginPayload>({ resolver: zodResolver(schema) });

  const mutation = useMutation({
    mutationFn: authApi.login,
    onSuccess: (data) => {
      queryClient.clear();
      setSession({ token: data.token, user: data.user, expiresAt: data.expiresAt });
      navigate("/dashboard");
    },
    onError: (error: any) => {
      const data = error.response?.data;
      const violations = data?.violations as Array<{ field: string; message: string }> | undefined;
      if (violations && violations.length > 0) {
        setServerError(violations.map((v) => `${v.field}: ${v.message}`).join("\n"));
      } else {
        setServerError(data?.message ?? "Nie udało się zalogować");
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
          <p className="text-sm text-slate-500 mt-1 dark:text-slate-400">Zaloguj się do swojego panelu</p>
        </div>

        <div className="card p-7">
          <form onSubmit={handleSubmit((data) => mutation.mutate(data))} className="space-y-4">
            <div>
              <label className="label">Email</label>
              <input type="email" className="input" autoComplete="email" {...register("email")} />
              {errors.email && <p className="text-xs text-rose-600 mt-1">{errors.email.message}</p>}
            </div>

            <div>
              <label className="label">Hasło</label>
              <input type="password" className="input" autoComplete="current-password" {...register("password")} />
              {errors.password && <p className="text-xs text-rose-600 mt-1">{errors.password.message}</p>}
            </div>

            {serverError && (
              <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-sm text-rose-700 whitespace-pre-line">
                {serverError}
              </div>
            )}

            <button type="submit" disabled={mutation.isPending} className="btn-primary w-full">
              <LogIn size={16} />
              {mutation.isPending ? "Logowanie…" : "Zaloguj"}
            </button>
          </form>
        </div>

        <p className="text-center text-sm text-slate-500 mt-6 dark:text-slate-400">
          Pierwszy raz?{" "}
          <Link to="/register" className="text-brand-600 hover:text-brand-700 font-semibold transition-colors">
            Zarejestruj konto
          </Link>
        </p>
      </div>
    </div>
  );
}
