import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { UserSummary } from "@/types/api";

interface AuthState {
  token: string | null;
  user: UserSummary | null;
  expiresAt: string | null;
  setSession: (data: { token: string; user: UserSummary; expiresAt: string }) => void;
  clearSession: () => void;
  isAuthenticated: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      user: null,
      expiresAt: null,
      setSession: ({ token, user, expiresAt }) => set({ token, user, expiresAt }),
      clearSession: () => set({ token: null, user: null, expiresAt: null }),
      isAuthenticated: () => {
        const { token, expiresAt } = get();
        if (!token || !expiresAt) return false;
        return new Date(expiresAt).getTime() > Date.now();
      },
    }),
    { name: "jdgflow-auth" },
  ),
);
