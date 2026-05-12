import { Moon, Sun } from "lucide-react";
import { useThemeStore } from "./themeStore";

interface Props {
  className?: string;
}

/**
 * Standalone theme toggle. Used in the sidebar (post-login) and as a floating
 * button on the public auth pages (login / register).
 */
export default function ThemeToggle({ className = "" }: Props) {
  const theme = useThemeStore((s) => s.theme);
  const toggle = useThemeStore((s) => s.toggle);
  const isDark = theme === "dark";

  return (
    <button
      onClick={toggle}
      aria-label={isDark ? "Włącz jasny tryb" : "Włącz ciemny tryb"}
      title={isDark ? "Jasny tryb" : "Ciemny tryb"}
      className={`p-2 rounded-lg text-slate-500 hover:text-slate-900 hover:bg-slate-100 transition-colors dark:text-slate-400 dark:hover:text-white dark:hover:bg-slate-800 ${className}`}
    >
      {isDark ? <Sun size={16} /> : <Moon size={16} />}
    </button>
  );
}
