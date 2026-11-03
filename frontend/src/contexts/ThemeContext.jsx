import { createContext, useState, useContext, useLayoutEffect, useCallback } from 'react';

const ThemeContext = createContext();

const STORAGE_KEY = 'theme';

function getInitialTheme() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored === 'dark' || stored === 'light' ? stored : 'light';
  } catch {
    return 'light';
  }
}

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(getInitialTheme);

  useLayoutEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, theme);
    } catch {
      // localStorage no disponible
    }
    document.body.classList.remove('light', 'dark');
    document.body.classList.add(theme);
  }, [theme]);

  const toggleTheme = useCallback(() => {
    setTheme(prev => prev === 'light' ? 'dark' : 'light');
  }, []);

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  return useContext(ThemeContext);
}
