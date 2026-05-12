import { NavLink, Outlet, useNavigate } from "react-router-dom";
import {
  FileText,
  LogOut,
  LayoutDashboard,
  Receipt,
  Calculator,
  Sparkles,
  Settings,
} from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/auth/authStore";
import ThemeToggle from "@/theme/ThemeToggle";

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `group relative flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-150 ${
    isActive
      ? "bg-brand-50 text-brand-700 dark:bg-brand-500/15 dark:text-brand-300"
      : "text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-white"
  }`;

const NAV_ITEMS = [
  { to: "/dashboard", icon: LayoutDashboard, label: "Pulpit" },
  { to: "/invoices",  icon: FileText,        label: "Faktury" },
  { to: "/expenses",  icon: Receipt,         label: "Wydatki" },
  { to: "/tax",       icon: Calculator,      label: "Podatki" },
  { to: "/profile",   icon: Settings,        label: "Ustawienia" },
];

export default function AppLayout() {
  const user = useAuthStore((s) => s.user);
  const clearSession = useAuthStore((s) => s.clearSession);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const logout = () => {
    queryClient.clear();
    clearSession();
    navigate("/login");
  };

  const initials = (user?.fullName ?? "")
    .split(" ")
    .map((part) => part[0])
    .filter(Boolean)
    .slice(0, 2)
    .join("")
    .toUpperCase();

  return (
    <div className="h-full flex">
      <aside className="w-64 bg-white backdrop-blur border-r border-slate-200 flex flex-col shadow-soft dark:bg-slate-900/80 dark:border-slate-800 dark:shadow-none">
        <div className="px-5 py-5 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between gap-2">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="h-9 w-9 rounded-lg bg-gradient-to-br from-brand-500 to-brand-700 flex items-center justify-center shadow-glow shrink-0">
              <Sparkles size={18} className="text-white" />
            </div>
            <h1 className="text-base font-bold text-slate-900 tracking-tight dark:text-white truncate">
              JDGFlow
            </h1>
          </div>
          <ThemeToggle />
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1">
          {NAV_ITEMS.map(({ to, icon: Icon, label }) => (
            <NavLink key={to} to={to} className={navLinkClass}>
              {({ isActive }) => (
                <>
                  {isActive && (
                    <span className="absolute left-0 top-2 bottom-2 w-1 bg-brand-600 rounded-r-full" />
                  )}
                  <Icon
                    size={18}
                    className={
                      isActive
                        ? "text-brand-600 dark:text-brand-300"
                        : "text-slate-400 group-hover:text-slate-600 dark:text-slate-500 dark:group-hover:text-slate-200"
                    }
                  />
                  {label}
                </>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="p-3 border-t border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-3 p-2.5 rounded-lg bg-slate-50 border border-slate-100 mb-2 dark:bg-slate-800/60 dark:border-slate-700">
            <div className="h-9 w-9 rounded-full bg-gradient-to-br from-brand-400 to-brand-600 flex items-center justify-center text-white text-xs font-bold shadow-soft shrink-0">
              {initials || "?"}
            </div>
            <div className="min-w-0">
              <p className="text-sm font-medium text-slate-900 truncate dark:text-white">{user?.fullName ?? "—"}</p>
              <p className="text-xs text-slate-500 truncate dark:text-slate-400">{user?.email}</p>
            </div>
          </div>
          <button
            onClick={logout}
            className="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-lg text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition-colors dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white"
          >
            <LogOut size={15} />
            Wyloguj
          </button>
        </div>
      </aside>

      <main className="flex-1 overflow-auto">
        <Outlet />
      </main>
    </div>
  );
}
