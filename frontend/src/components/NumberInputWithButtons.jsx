import { useState, useCallback } from 'react';

const btnStyle = {
  width: 32, height: 24, padding: 0, border: 'none', borderRadius: 4, cursor: 'pointer',
  background: 'var(--primary)', color: 'var(--primary-text)',
  display: 'flex', alignItems: 'center', justifyContent: 'center',
  fontSize: 11, lineHeight: 1, transition: 'all 0.15s',
  flexShrink: 0,
};

const NumberInputWithButtons = ({ value, onChange, step = 1000, min = 0, max, disabled }) => {
  const [hoverBtn, setHoverBtn] = useState(null);

  const handleIncrement = useCallback(() => {
    const next = Number(value || 0) + step;
    onChange(max !== undefined ? Math.min(max, next) : next);
  }, [value, step, max, onChange]);

  const handleDecrement = useCallback(() => {
    onChange(Math.max(min, Number(value || 0) - step));
  }, [value, step, min, onChange]);

  const btnEvents = (name) => ({
    onMouseEnter: () => setHoverBtn(name),
    onMouseLeave: () => setHoverBtn(null),
    onMouseDown: (e) => { e.currentTarget.style.transform = 'scale(0.93)'; },
    onMouseUp: (e) => { e.currentTarget.style.transform = hoverBtn === name ? 'scale(1.06)' : 'scale(1)'; },
  });

  const isHover = (name) => hoverBtn === name;

  return (
    <div style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
      <button type="button" disabled={disabled}
        onClick={handleDecrement}
        title={`Decrementar -${step}`}
        style={{
          ...btnStyle,
          opacity: disabled ? 0.5 : (isHover('dec') ? 1 : 0.85),
          transform: isHover('dec') ? 'scale(1.06)' : 'scale(1)',
          boxShadow: isHover('dec') ? '0 2px 6px rgba(0,0,0,0.25)' : 'none',
        }}
        {...btnEvents('dec')}
      >−</button>
      <input type="number" step={step} min={min} max={max}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
        className="form-input"
        style={{
          maxWidth: 120, textAlign: 'center', padding: '0.35rem 0.4rem',
          opacity: disabled ? 0.6 : 1,
        }}
      />
      <button type="button" disabled={disabled}
        onClick={handleIncrement}
        title={`Incrementar +${step}`}
        style={{
          ...btnStyle,
          opacity: disabled ? 0.5 : (isHover('inc') ? 1 : 0.85),
          transform: isHover('inc') ? 'scale(1.06)' : 'scale(1)',
          boxShadow: isHover('inc') ? '0 2px 6px rgba(0,0,0,0.25)' : 'none',
        }}
        {...btnEvents('inc')}
      >+</button>
    </div>
  );
};

export default NumberInputWithButtons;
