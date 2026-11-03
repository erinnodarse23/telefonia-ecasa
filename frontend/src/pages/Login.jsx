import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';
import { login as apiLogin, fetchCaptcha } from '../services/api';
import Logo from '../components/Logo';
import { FiSun, FiMoon } from 'react-icons/fi';

const bgStyles = `
@keyframes liquidA {
  0% { transform: translate(0, 0) scale(1, 1) rotate(0deg); border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%; }
  10% { transform: translate(200px, -160px) scale(1.3, 0.7) rotate(36deg); border-radius: 30% 70% 60% 40% / 50% 60% 20% 80%; }
  20% { transform: translate(340px, -40px) scale(0.7, 1.4) rotate(72deg); border-radius: 70% 30% 40% 60% / 20% 80% 50% 60%; }
  30% { transform: translate(260px, 180px) scale(1.2, 0.8) rotate(108deg); border-radius: 40% 60% 20% 80% / 60% 30% 70% 40%; }
  40% { transform: translate(80px, 320px) scale(0.8, 1.3) rotate(144deg); border-radius: 20% 80% 60% 40% / 40% 70% 30% 60%; }
  50% { transform: translate(-160px, 260px) scale(1.4, 0.7) rotate(180deg); border-radius: 60% 40% 80% 20% / 30% 60% 40% 70%; }
  60% { transform: translate(-320px, 100px) scale(0.7, 1.3) rotate(216deg); border-radius: 80% 20% 40% 60% / 60% 30% 70% 40%; }
  70% { transform: translate(-280px, -120px) scale(1.3, 0.8) rotate(252deg); border-radius: 40% 60% 30% 70% / 20% 80% 50% 60%; }
  80% { transform: translate(-100px, -300px) scale(0.8, 1.4) rotate(288deg); border-radius: 30% 70% 80% 20% / 60% 40% 30% 70%; }
  90% { transform: translate(120px, -220px) scale(1.2, 0.8) rotate(324deg); border-radius: 60% 40% 30% 70% / 40% 60% 80% 20%; }
  100% { transform: translate(0, 0) scale(1, 1) rotate(360deg); border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%; }
}
@keyframes liquidB {
  0% { transform: translate(0, 0) scale(1, 1) rotate(0deg); border-radius: 40% 60% 70% 30% / 40% 50% 60% 50%; }
  10% { transform: translate(-180px, 140px) scale(0.7, 1.3) rotate(-36deg); border-radius: 60% 40% 20% 80% / 50% 40% 70% 30%; }
  20% { transform: translate(-360px, 40px) scale(1.4, 0.7) rotate(-72deg); border-radius: 20% 80% 50% 50% / 60% 20% 50% 80%; }
  30% { transform: translate(-280px, -160px) scale(0.8, 1.3) rotate(-108deg); border-radius: 80% 20% 60% 40% / 30% 70% 40% 60%; }
  40% { transform: translate(-80px, -320px) scale(1.3, 0.7) rotate(-144deg); border-radius: 50% 50% 30% 70% / 50% 30% 70% 50%; }
  50% { transform: translate(160px, -260px) scale(0.7, 1.4) rotate(-180deg); border-radius: 30% 70% 80% 20% / 70% 40% 30% 60%; }
  60% { transform: translate(340px, -80px) scale(1.4, 0.7) rotate(-216deg); border-radius: 60% 40% 30% 70% / 40% 60% 20% 80%; }
  70% { transform: translate(280px, 120px) scale(0.7, 1.3) rotate(-252deg); border-radius: 20% 80% 60% 40% / 80% 20% 50% 60%; }
  80% { transform: translate(100px, 300px) scale(1.3, 0.8) rotate(-288deg); border-radius: 70% 30% 40% 60% / 40% 70% 30% 60%; }
  90% { transform: translate(-120px, 220px) scale(0.8, 1.3) rotate(-324deg); border-radius: 40% 60% 70% 30% / 60% 30% 70% 40%; }
  100% { transform: translate(0, 0) scale(1, 1) rotate(-360deg); border-radius: 40% 60% 70% 30% / 40% 50% 60% 50%; }
}
@keyframes liquidC {
  0% { transform: translate(0, 0) scale(1, 1) rotate(0deg); border-radius: 50% 50% 40% 60% / 60% 40% 50% 50%; }
  10% { transform: translate(160px, 200px) scale(1.4, 0.6) rotate(40deg); border-radius: 20% 80% 60% 40% / 40% 60% 40% 70%; }
  20% { transform: translate(-40px, 360px) scale(0.6, 1.4) rotate(80deg); border-radius: 70% 30% 30% 70% / 50% 30% 80% 20%; }
  30% { transform: translate(-240px, 280px) scale(1.3, 0.7) rotate(120deg); border-radius: 40% 60% 70% 30% / 60% 40% 30% 70%; }
  40% { transform: translate(-360px, 60px) scale(0.7, 1.3) rotate(160deg); border-radius: 30% 70% 40% 60% / 20% 80% 50% 60%; }
  50% { transform: translate(-260px, -180px) scale(1.4, 0.6) rotate(200deg); border-radius: 60% 40% 20% 80% / 60% 30% 70% 40%; }
  60% { transform: translate(-60px, -360px) scale(0.6, 1.4) rotate(240deg); border-radius: 80% 20% 60% 40% / 40% 70% 30% 60%; }
  70% { transform: translate(180px, -280px) scale(1.3, 0.7) rotate(280deg); border-radius: 20% 80% 50% 50% / 70% 30% 60% 40%; }
  80% { transform: translate(360px, -60px) scale(0.7, 1.3) rotate(320deg); border-radius: 50% 50% 30% 70% / 40% 60% 40% 70%; }
  90% { transform: translate(280px, 160px) scale(1.4, 0.6) rotate(360deg); border-radius: 70% 30% 80% 20% / 30% 70% 50% 60%; }
  100% { transform: translate(0, 0) scale(1, 1) rotate(400deg); border-radius: 50% 50% 40% 60% / 60% 40% 50% 50%; }
}
@keyframes liquidD {
  0% { transform: translate(0, 0) scale(0, 0); opacity: 0; }
  5% { transform: translate(100px, -80px) scale(1, 1); opacity: 1; }
  15% { transform: translate(300px, 20px) scale(1.5, 0.6); border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%; }
  30% { transform: translate(100px, 250px) scale(0.6, 1.5); border-radius: 30% 70% 60% 40% / 50% 60% 20% 80%; }
  45% { transform: translate(-200px, 200px) scale(1.4, 0.7); border-radius: 70% 30% 40% 60% / 20% 80% 50% 60%; }
  60% { transform: translate(-350px, -50px) scale(0.6, 1.4); border-radius: 40% 60% 20% 80% / 60% 30% 70% 40%; }
  75% { transform: translate(-150px, -280px) scale(1.5, 0.6); border-radius: 20% 80% 60% 40% / 40% 70% 30% 60%; }
  90% { transform: translate(150px, -180px) scale(0.6, 1.4); border-radius: 80% 20% 40% 60% / 60% 30% 70% 40%; }
  95% { transform: translate(50px, -40px) scale(1, 1); opacity: 1; }
  100% { transform: translate(0, 0) scale(0, 0); opacity: 0; }
}
`;

const inputBase = (isDark) => ({
  width: '100%',
  padding: '12px 14px',
  fontSize: 14,
  borderRadius: 10,
  border: `1px solid var(--border)`,
  background: 'var(--bg-input)',
  color: 'var(--text-primary)',
  outline: 'none',
  boxSizing: 'border-box',
  transition: 'border-color 0.2s',
});

const Login = () => {
  const [loginValue, setLoginValue] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [loading, setLoading] = useState(false);
  const [isDark, setIsDark] = useState(() => document.body.classList.contains('dark'));
  const [captcha, setCaptcha] = useState(null);
  const [captchaAnswer, setCaptchaAnswer] = useState('');
  const [requireCaptcha, setRequireCaptcha] = useState(false);
  const navigate = useNavigate();
  const { login } = useAuth();
  const { toggleTheme } = useTheme();

  useEffect(() => {
    const observer = new MutationObserver(() => {
      setIsDark(document.body.classList.contains('dark'));
    });
    observer.observe(document.body, { attributes: true, attributeFilter: ['class'] });
    return () => observer.disconnect();
  }, []);

  const loadCaptcha = async () => {
    try {
      const data = await fetchCaptcha();
      setCaptcha(data);
      setCaptchaAnswer('');
    } catch {
      toast.error('Error al cargar el captcha');
    }
  };

  useEffect(() => {
    if (requireCaptcha) {
      loadCaptcha();
    }
  }, [requireCaptcha]);

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!loginValue.trim() || !password) {
      toast.error('Todos los campos son obligatorios');
      return;
    }

    if (requireCaptcha && !captchaAnswer.trim()) {
      toast.error('Debe responder el captcha');
      return;
    }

    setLoading(true);
    try {
      const data = await apiLogin(
        loginValue,
        password,
        rememberMe,
        captcha?.captchaId,
        captchaAnswer.trim() || null
      );
      setCaptcha(null);
      setCaptchaAnswer('');
      setRequireCaptcha(false);
      login(data.token, data.usuario);
      navigate('/dashboard');
    } catch (err) {
      const errorData = err.response?.data;
      if (errorData?.requireCaptcha) {
        setRequireCaptcha(true);
      }
      if (errorData?.error) {
        toast.error(errorData.error);
      } else {
        toast.error('Error al iniciar sesión');
      }
    } finally {
      setLoading(false);
    }
  };

  const cardBg = isDark ? 'rgba(0, 0, 0, 0.7)' : 'rgba(255, 255, 255, 0.9)';

  return (
    <>
      <style>{bgStyles}</style>
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: isDark
            ? 'linear-gradient(135deg, #0a1628 0%, #0f2847 30%, #1a3a5c 60%, #0d1f3c 100%)'
            : 'linear-gradient(135deg, #dbeafe 0%, #bfdbfe 30%, #eff6ff 60%, #93c5fd 100%)',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        {isDark ? (
          <>
            <div style={{
              position: 'absolute', width: 640, height: 640,
              background: 'radial-gradient(circle, rgba(56,189,248,0.4) 0%, rgba(56,189,248,0.06) 60%, transparent 100%)',
              top: '3%', left: '3%',
              filter: 'blur(50px)',
              animation: 'liquidA 14s ease-in-out infinite',
              pointerEvents: 'none',
            }} />
            <div style={{
              position: 'absolute', width: 540, height: 540,
              background: 'radial-gradient(circle, rgba(139,92,246,0.4) 0%, rgba(139,92,246,0.06) 60%, transparent 100%)',
              bottom: '3%', right: '3%',
              filter: 'blur(50px)',
              animation: 'liquidB 16s ease-in-out infinite',
              pointerEvents: 'none',
            }} />
            <div style={{
              position: 'absolute', width: 460, height: 460,
              background: 'radial-gradient(circle, rgba(236,72,153,0.35) 0%, rgba(236,72,153,0.05) 60%, transparent 100%)',
              top: '40%', left: '38%',
              filter: 'blur(60px)',
              animation: 'liquidC 18s ease-in-out infinite',
              pointerEvents: 'none',
            }} />
            <div style={{
              position: 'absolute', width: 320, height: 320,
              background: 'radial-gradient(circle, rgba(167,139,250,0.3) 0%, rgba(167,139,250,0.04) 60%, transparent 100%)',
              top: '70%', left: '60%',
              filter: 'blur(50px)',
              animation: 'liquidD 12s ease-in-out infinite',
              pointerEvents: 'none',
            }} />
            <div style={{
              position: 'absolute', width: 480, height: 480,
              background: 'radial-gradient(circle, rgba(15,23,42,0.55) 0%, rgba(15,23,42,0.08) 60%, transparent 100%)',
              top: '55%', left: '8%',
              filter: 'blur(55px)',
              animation: 'liquidA 22s ease-in-out infinite',
              pointerEvents: 'none',
            }} />
            <div style={{
              position: 'absolute', width: 400, height: 400,
              background: 'radial-gradient(circle, rgba(244,63,94,0.5) 0%, rgba(244,63,94,0.08) 60%, transparent 100%)',
              top: '22%', left: '58%',
              filter: 'blur(50px)',
              animation: 'liquidD 15s ease-in-out infinite',
              pointerEvents: 'none',
            }} />
            <div style={{
              position: 'absolute', width: 350, height: 350,
              background: 'radial-gradient(circle, rgba(219,39,119,0.55) 0%, rgba(219,39,119,0.08) 60%, transparent 100%)',
              top: '78%', left: '32%',
              filter: 'blur(48px)',
              animation: 'liquidB 20s ease-in-out infinite',
              pointerEvents: 'none',
            }} />
          </>
        ) : (
          <>
            <div style={{
              position: 'absolute', width: 620, height: 620,
              background: 'radial-gradient(circle, rgba(30,64,175,0.3) 0%, rgba(30,64,175,0.07) 60%, transparent 100%)',
              top: '2%', right: '3%',
              filter: 'blur(40px)',
              animation: 'liquidA 10s ease-in-out infinite',
              pointerEvents: 'none',
              opacity: 0.95,
            }} />
            <div style={{
              position: 'absolute', width: 520, height: 520,
              background: 'radial-gradient(circle, rgba(37,99,235,0.28) 0%, rgba(37,99,235,0.06) 60%, transparent 100%)',
              bottom: '4%', left: '2%',
              filter: 'blur(40px)',
              animation: 'liquidB 12s ease-in-out infinite',
              pointerEvents: 'none',
              opacity: 0.95,
            }} />
            <div style={{
              position: 'absolute', width: 440, height: 440,
              background: 'radial-gradient(circle, rgba(59,130,246,0.3) 0%, rgba(59,130,246,0.06) 60%, transparent 100%)',
              top: '42%', left: '40%',
              filter: 'blur(45px)',
              animation: 'liquidC 14s ease-in-out infinite',
              pointerEvents: 'none',
              opacity: 0.92,
            }} />
            <div style={{
              position: 'absolute', width: 320, height: 320,
              background: 'radial-gradient(circle, rgba(96,165,250,0.28) 0%, rgba(96,165,250,0.05) 60%, transparent 100%)',
              top: '72%', left: '58%',
              filter: 'blur(38px)',
              animation: 'liquidD 9s ease-in-out infinite',
              pointerEvents: 'none',
              opacity: 0.92,
            }} />
            <div style={{
              position: 'absolute', width: 500, height: 500,
              background: 'radial-gradient(circle, rgba(12,30,70,0.4) 0%, rgba(12,30,70,0.08) 60%, transparent 100%)',
              top: '50%', left: '5%',
              filter: 'blur(45px)',
              animation: 'liquidB 18s ease-in-out infinite',
              pointerEvents: 'none',
              opacity: 0.9,
            }} />
            <div style={{
              position: 'absolute', width: 360, height: 360,
              background: 'radial-gradient(circle, rgba(20,50,100,0.35) 0%, rgba(20,50,100,0.06) 60%, transparent 100%)',
              top: '15%', left: '55%',
              filter: 'blur(42px)',
              animation: 'liquidC 20s ease-in-out infinite',
              pointerEvents: 'none',
              opacity: 0.88,
            }} />
            <div style={{
              position: 'absolute', width: 420, height: 420,
              background: 'radial-gradient(circle, rgba(15,35,80,0.45) 0%, rgba(15,35,80,0.07) 60%, transparent 100%)',
              top: '65%', left: '45%',
              filter: 'blur(44px)',
              animation: 'liquidD 16s ease-in-out infinite',
              pointerEvents: 'none',
              opacity: 0.92,
            }} />
            <div style={{
              position: 'absolute', width: 380, height: 380,
              background: 'radial-gradient(circle, rgba(20,45,90,0.4) 0%, rgba(20,45,90,0.06) 60%, transparent 100%)',
              top: '8%', left: '30%',
              filter: 'blur(42px)',
              animation: 'liquidA 24s ease-in-out infinite',
              pointerEvents: 'none',
              opacity: 0.9,
            }} />
            <div style={{
              position: 'absolute', width: 360, height: 360,
              background: 'radial-gradient(circle, rgba(15,35,80,0.4) 0%, rgba(15,35,80,0.06) 60%, transparent 100%)',
              top: '5%', left: '12%',
              filter: 'blur(40px)',
              animation: 'liquidC 17s ease-in-out infinite',
              pointerEvents: 'none',
              opacity: 0.88,
            }} />
            <div style={{
              position: 'absolute', width: 340, height: 340,
              background: 'radial-gradient(circle, rgba(20,45,90,0.38) 0%, rgba(20,45,90,0.06) 60%, transparent 100%)',
              top: '76%', left: '42%',
              filter: 'blur(41px)',
              animation: 'liquidA 19s ease-in-out infinite',
              pointerEvents: 'none',
              opacity: 0.88,
            }} />
            <div style={{
              position: 'absolute', width: 340, height: 340,
              background: 'radial-gradient(circle, rgba(13,148,136,0.3) 0%, rgba(13,148,136,0.05) 60%, transparent 100%)',
              top: '30%', left: '68%',
              filter: 'blur(40px)',
              animation: 'liquidB 13s ease-in-out infinite',
              pointerEvents: 'none',
              opacity: 0.88,
            }} />
            <div style={{
              position: 'absolute', width: 300, height: 300,
              background: 'radial-gradient(circle, rgba(8,145,178,0.28) 0%, rgba(8,145,178,0.05) 60%, transparent 100%)',
              top: '60%', left: '25%',
              filter: 'blur(38px)',
              animation: 'liquidD 11s ease-in-out infinite',
              pointerEvents: 'none',
              opacity: 0.85,
            }} />
          </>
        )}

        <button
          onClick={toggleTheme}
          style={{
            position: 'fixed',
            top: 16,
            right: 16,
            width: 40,
            height: 40,
            borderRadius: '50%',
            border: '1px solid rgba(255,255,255,0.2)',
            background: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.08)',
            color: isDark ? '#f1f5f9' : '#1e293b',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            fontSize: 18,
            zIndex: 10,
            backdropFilter: 'blur(8px)',
            transition: 'background 0.2s, transform 0.2s',
          }}
          onMouseEnter={(e) => e.currentTarget.style.transform = 'scale(1.1)'}
          onMouseLeave={(e) => e.currentTarget.style.transform = 'scale(1)'}
          title={isDark ? 'Modo claro' : 'Modo oscuro'}
        >
          {isDark ? <FiSun /> : <FiMoon />}
        </button>

        <div
          style={{
            width: '100%',
            maxWidth: 420,
            margin: '0 16px',
            padding: '36px 32px 32px',
            borderRadius: 20,
            background: cardBg,
            backdropFilter: 'blur(20px) saturate(180%)',
            WebkitBackdropFilter: 'blur(20px) saturate(180%)',
            boxShadow: isDark
              ? '0 8px 32px rgba(0,0,0,0.4)'
              : '0 8px 32px rgba(0,0,0,0.1)',
            border: isDark
              ? '1px solid rgba(255,255,255,0.08)'
              : '1px solid rgba(255,255,255,0.5)',
            position: 'relative',
            zIndex: 1,
          }}
        >
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginBottom: 28 }}>
            <Logo size="large" />
            <h1
              style={{
                margin: '12px 0 4px',
                fontSize: 22,
                fontWeight: 700,
                color: 'var(--text-primary)',
              }}
            >
              ECASA.SA
            </h1>
            <p
              style={{
                margin: 0,
                fontSize: 13,
                color: 'var(--text-secondary)',
              }}
            >
              Sistema de Control de Telefonía Móvil
            </p>
          </div>

          <form onSubmit={handleSubmit} noValidate autoComplete="off">
            <div style={{ marginBottom: 20 }}>
              <input
                type="text"
                placeholder="Usuario"
                value={loginValue}
                onChange={(e) => setLoginValue(e.target.value)}
                disabled={loading}
                style={inputBase(isDark)}
              />
            </div>

            <div style={{ marginBottom: 20 }}>
              <input
                type="password"
                placeholder="Contraseña"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={loading}
                style={inputBase(isDark)}
              />
            </div>

            {requireCaptcha && captcha && (
              <div style={{ marginBottom: 20 }}>
                <p style={{
                  margin: '0 0 8px',
                  fontSize: 14,
                  fontWeight: 600,
                  color: 'var(--text-primary)',
                  textAlign: 'center',
                }}>
                  {captcha.question}
                </p>
                <input
                  type="text"
                  placeholder="Respuesta"
                  value={captchaAnswer}
                  onChange={(e) => setCaptchaAnswer(e.target.value)}
                  disabled={loading}
                  style={inputBase(isDark)}
                  autoComplete="off"
                />
              </div>
            )}

            <label
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                marginBottom: 24,
                cursor: loading ? 'not-allowed' : 'pointer',
                fontSize: 13,
                color: 'var(--text-secondary)',
              }}
            >
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={() => !loading && setRememberMe((prev) => !prev)}
                style={{ accentColor: 'var(--primary)', width: 16, height: 16 }}
              />
              Recordarme
            </label>

            <button
              type="submit"
              disabled={loading}
              style={{
                width: '100%',
                padding: '12px 24px',
                fontSize: 15,
                fontWeight: 600,
                borderRadius: 10,
                border: 'none',
                background: 'var(--primary)',
                color: '#fff',
                cursor: loading ? 'not-allowed' : 'pointer',
                opacity: loading ? 0.7 : 1,
                transition: 'transform 0.2s ease, opacity 0.2s ease',
              }}
              onMouseEnter={(e) => {
                if (!loading) e.currentTarget.style.transform = 'scale(1.02)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'scale(1)';
              }}
            >
              {loading ? 'Ingresando...' : 'Iniciar Sesión'}
            </button>
          </form>

          <div
            style={{
              textAlign: 'center',
              marginTop: 28,
              fontSize: 12,
              color: 'var(--text-muted)',
            }}
          >
            &copy; 2026 ECASA &mdash; v1.0.0
          </div>
        </div>
      </div>
    </>
  );
};

export default Login;
