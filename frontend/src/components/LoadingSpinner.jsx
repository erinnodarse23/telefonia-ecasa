const sizes = {
  xs: { width: 20, height: 20, border: 2 },
  sm: { width: 18, height: 18, border: 2 },
  md: { width: 32, height: 32, border: 3 },
  lg: { width: 48, height: 48, border: 4 },
};

const LoadingSpinner = ({ size = 'md', overlay = false, text, color, className = '' }) => {
  const s = sizes[size] || sizes.md;
  const spinner = (
    <div
      className={className}
      style={{
        width: s.width,
        height: s.height,
        border: `${s.border}px solid ${color ? `${color}33` : 'var(--border)'}`,
        borderTopColor: color || 'var(--primary)',
        borderRadius: '50%',
        animation: 'spin 0.7s linear infinite',
      }}
    />
  );

  if (overlay) {
    return (
      <div
        style={{
          position: 'fixed',
          inset: 0,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 12,
          backgroundColor: 'rgba(0,0,0,0.3)',
          zIndex: 200,
        }}
      >
        {spinner}
        {text && <span style={{ color: '#fff', fontSize: 14 }}>{text}</span>}
      </div>
    );
  }

  if (text) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, color: 'var(--text-secondary)', fontSize: 14 }}>
        {spinner}
        <span>{text}</span>
      </div>
    );
  }

  return spinner;
};

export default LoadingSpinner;
