// src/theme/ThemeProvider.tsx
import { createContext, useContext, useEffect, useState, ReactNode } from 'react';

export const THEMES = [
  { key: 'original',  label: 'Original Navy' },
  { key: 'plum',      label: 'Plum & Sage' },
  { key: 'forest',    label: 'Forest & Amber' },
  { key: 'mono',      label: 'Monochrome Slate' },
  { key: 'indigo',    label: 'Royal Indigo' },
  { key: 'sandstone', label: 'Sandstone & Clay' },
  { key: 'arctic',    label: 'Arctic Ice' },
  { key: 'rose',      label: 'Dusty Rose' },
  { key: 'contrast',  label: 'High-Contrast AAA' },
] as const;

export type ThemeKey = typeof THEMES[number]['key'];
const STORAGE_KEY = 'app-theme';

type Ctx = { theme: ThemeKey; setTheme: (t: ThemeKey) => void };
const ThemeContext = createContext<Ctx | null>(null);

function readInitial(): ThemeKey {
  if (typeof window === 'undefined') return 'original';
  const saved = localStorage.getItem(STORAGE_KEY) as ThemeKey | null;
  if (saved && THEMES.some(t => t.key === saved)) return saved;
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'original' : 'arctic';
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<ThemeKey>(readInitial);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    try { localStorage.setItem(STORAGE_KEY, theme); } catch {}
  }, [theme]);

  const setTheme = (t: ThemeKey) => setThemeState(t);

  return <ThemeContext.Provider value={{ theme, setTheme }}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be used inside ThemeProvider');
  return ctx;
}
