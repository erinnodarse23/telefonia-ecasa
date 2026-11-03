import { useState } from 'react';
import { useTheme } from '../contexts/ThemeContext';

const ThemeToggle = () => {
  const { theme, toggleTheme } = useTheme();
  const isDark = theme === 'dark';
  const [spinning, setSpinning] = useState(false);

  const handleClick = () => {
    setSpinning(true);
    toggleTheme();
    setTimeout(() => setSpinning(false), 700);
  };

  return (
    <>
      <style>{`
        @keyframes themeSpin {
          0% { transform: rotate(0deg) scale(1); }
          40% { transform: rotate(360deg) scale(1.25); }
          100% { transform: rotate(360deg) scale(1); }
        }
        @keyframes gradShift {
          0% { background-position: 0% 50%; }
          50% { background-position: 100% 50%; }
          100% { background-position: 0% 50%; }
        }
        @keyframes glowPulse {
          0%, 100% { box-shadow: 0 0 6px var(--glow, transparent); }
          50% { box-shadow: 0 0 22px var(--glow, transparent); }
        }
        .theme-btn { position: relative; overflow: hidden; }
        .theme-btn .theme-icon {
          display: inline-block;
          transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
        }
        .theme-btn:hover .theme-icon {
          transform: rotate(25deg) scale(1.15);
        }
      `}</style>
      <button
        className="theme-btn"
        onClick={handleClick}
        title={isDark ? 'Modo claro' : 'Modo oscuro'}
        style={{
          borderRadius: 20,
          padding: '0.35rem 0.85rem',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 6,
          fontSize: 13,
          fontWeight: 600,
          lineHeight: 1,
          border: 'none',
          background: isDark
            ? 'linear-gradient(135deg, #0f2b5e, #1a365d, #2b6cb0, #1e3a5f)'
            : 'linear-gradient(135deg, #e2e8f0, #f8fafc, #e2e8f0, #f1f5f9)',
          backgroundSize: '300% 300%',
          color: isDark ? '#ffffff' : '#1a202c',
          transition: 'transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1)',
          animation: 'glowPulse 3s ease-in-out infinite, gradShift 6s ease infinite',
          '--glow': isDark ? 'rgba(59,130,246,0.35)' : 'rgba(99,102,241,0.18)',
        }}
        onMouseEnter={(e) => { e.currentTarget.style.transform = 'scale(1.1)'; }}
        onMouseLeave={(e) => { e.currentTarget.style.transform = 'scale(1)'; }}
      >
        <span
          className="theme-icon"
          style={{
            fontSize: 17,
            lineHeight: 1,
            animation: spinning ? 'themeSpin 0.7s ease' : 'none',
          }}
        >
          {isDark ? '🌙' : '☀️'}
        </span>
      </button>
    </>
  );
};

export default ThemeToggle;
