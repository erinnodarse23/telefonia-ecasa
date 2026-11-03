import { Component } from 'react';

const getErrorMessage = (error) => {
  if (error instanceof Error) return error.message;
  if (typeof error === 'string') return error;
  try { return JSON.stringify(error); } catch { return 'Error desconocido'; }
};

class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('[ErrorBoundary] Error capturado:', error, errorInfo?.componentStack || errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '100vh',
          padding: '2rem',
          textAlign: 'center',
          background: 'var(--bg-primary, #f5f7fa)',
          color: 'var(--text-primary, #1a1a2e)',
        }}>
          <div style={{
            background: 'var(--bg-card, #ffffff)',
            borderRadius: 16,
            padding: '3rem',
            maxWidth: 500,
            width: '100%',
            boxShadow: '0 8px 32px rgba(0,0,0,0.1)',
          }}>
            <div style={{
              width: 64,
              height: 64,
              borderRadius: '50%',
              background: '#fee2e2',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 1.5rem',
              fontSize: 28,
            }}>
              ⚠️
            </div>
            <h2 style={{ margin: '0 0 0.5rem', fontSize: 22, fontWeight: 700 }}>Algo salió mal</h2>
            <p style={{ margin: '0 0 1.5rem', fontSize: 14, color: 'var(--text-secondary, #64748b)', lineHeight: 1.6 }}>
              Ocurrió un error inesperado al renderizar la aplicación.
              {this.state.error && (
                <code style={{
                  display: 'block',
                  marginTop: '0.75rem',
                  padding: '0.75rem',
                  background: '#f1f5f9',
                  borderRadius: 8,
                  fontSize: 12,
                  color: '#dc2626',
                  wordBreak: 'break-all',
                }}>
                  {getErrorMessage(this.state.error)}
                </code>
              )}
            </p>
            <button
              onClick={() => window.location.reload()}
              style={{
                padding: '0.75rem 2rem',
                background: '#2563eb',
                color: '#ffffff',
                border: 'none',
                borderRadius: 8,
                fontSize: 14,
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              Recargar página
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
