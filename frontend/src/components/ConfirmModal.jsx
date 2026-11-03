import { useCallback, useEffect, useState } from 'react';

const variantStyles = {
  primary: { confirmBg: 'var(--primary)', confirmBgHover: 'var(--primary-hover)' },
  warning: { confirmBg: 'var(--warning)', confirmBgHover: 'var(--warning-hover)' },
  danger: { confirmBg: 'var(--danger)', confirmBgHover: 'var(--danger-hover)' },
};

const ConfirmModal = ({ isOpen, onClose, onCancel, onConfirm, title, message, confirmText = 'Confirmar', cancelText = 'Cancelar', variant = 'primary' }) => {
  const handleClose = onClose || onCancel;
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (isOpen) {
      requestAnimationFrame(() => setVisible(true));
    } else {
      setVisible(false);
    }
  }, [isOpen]);

  const handleKeyDown = useCallback((e) => {
    if (e.key === 'Escape' && handleClose) handleClose();
  }, [handleClose]);

  useEffect(() => {
    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown);
      return () => document.removeEventListener('keydown', handleKeyDown);
    }
  }, [isOpen, handleKeyDown]);

  const v = variantStyles[variant] || variantStyles.primary;

  if (!isOpen) return null;

  return (
    <div
      style={{
        position: 'fixed',
        top: 0, left: 0, right: 0, bottom: 0,
        backgroundColor: 'rgba(0,0,0,0.5)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 200,
        opacity: visible ? 1 : 0,
        transition: 'opacity 0.2s ease',
      }}
      onClick={handleClose}
    >
      <div
        style={{
          backgroundColor: 'var(--bg-elevated)',
          color: 'var(--text-primary)',
          padding: '1.5rem',
          borderRadius: 'var(--radius)',
          width: '100%',
          maxWidth: 400,
          boxShadow: 'var(--shadow-xl)',
          transform: visible ? 'scale(1)' : 'scale(0.95)',
          transition: 'transform 0.2s ease',
          border: '1px solid var(--border)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <h3 style={{ margin: '0 0 0.5rem', fontSize: '1.125rem', fontWeight: 700, color: 'var(--text-primary)' }}>
          {title}
        </h3>
        <p style={{ margin: '0 0 1.5rem', fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
          {message}
        </p>
        <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
          <button
            type="button"
            onClick={handleClose}
            className="btn btn-secondary btn-sm"
          >
            {cancelText}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            style={{
              padding: '0.5rem 1rem',
              backgroundColor: v.confirmBg,
              color: '#ffffff',
              border: 'none',
              borderRadius: 'var(--radius-sm)',
              cursor: 'pointer',
              fontSize: '0.875rem',
              fontWeight: 600,
              transition: 'all 0.2s ease',
            }}
            onMouseEnter={(e) => e.target.style.backgroundColor = v.confirmBgHover}
            onMouseLeave={(e) => e.target.style.backgroundColor = v.confirmBg}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmModal;
