import { createContext, useState, useContext, useEffect, useCallback, useRef } from 'react';
import api, { verifyToken as apiVerify } from '../services/api';

const INACTIVIDAD_MS = 30 * 60 * 1000;
const EVENTOS_ACTIVIDAD = ['mousedown', 'keydown', 'mousemove', 'touchstart', 'scroll'];

function isTokenExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.exp * 1000 < Date.now();
  } catch {
    return true;
  }
}

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);
  const [mensajeExpiracion, setMensajeExpiracion] = useState(null);
  const logoutRef = useRef();
  const inactividadRef = useRef();
  const logoutEnProgresoRef = useRef(false);

  const limpiarTimerInactividad = useCallback(() => {
    if (inactividadRef.current) {
      clearTimeout(inactividadRef.current);
      inactividadRef.current = null;
    }
  }, []);

  const reiniciarTimerInactividad = useCallback(() => {
    limpiarTimerInactividad();
    inactividadRef.current = setTimeout(() => {
      setMensajeExpiracion('Sesión expirada por inactividad');
      logoutRef.current();
    }, INACTIVIDAD_MS);
  }, [limpiarTimerInactividad]);

  const logout = useCallback(async (porInactividad = false) => {
    if (logoutEnProgresoRef.current) return;
    logoutEnProgresoRef.current = true;

    const tokenActual = sessionStorage.getItem('token');
    limpiarTimerInactividad();
    setToken(null);
    setUsuario(null);
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('usuario');

    if (porInactividad) {
      setMensajeExpiracion('Sesión expirada por inactividad');
    }

    if (tokenActual) {
      try {
        await api.post('/auth/logout', null, {
          headers: { Authorization: `Bearer ${tokenActual}` },
        });
      } catch {
        // El cierre local no depende de que el backend responda.
      }
    }

    logoutEnProgresoRef.current = false;
  }, [limpiarTimerInactividad]);
  useEffect(() => {
    logoutRef.current = logout;
  }, [logout]);

  useEffect(() => {
    let cancelled = false;

    const init = async () => {
      const storedToken = sessionStorage.getItem('token');
      const storedUsuario = sessionStorage.getItem('usuario');

      if (!storedToken || !storedUsuario) {
        setLoading(false);
        return;
      }

      if (isTokenExpired(storedToken)) {
        sessionStorage.removeItem('token');
        sessionStorage.removeItem('usuario');
        setLoading(false);
        return;
      }

      try {
        const data = await apiVerify();
        if (!cancelled) {
          setToken(storedToken);
          setUsuario(data.usuario);
        }
      } catch {
        if (!cancelled) {
          sessionStorage.removeItem('token');
          sessionStorage.removeItem('usuario');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    init();
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    const handler = () => {
      if (!sessionStorage.getItem('token')) return;
      logoutRef.current();
    };
    window.addEventListener('auth:unauthorized', handler);
    return () => window.removeEventListener('auth:unauthorized', handler);
  }, []);

  useEffect(() => {
    if (!token) {
      limpiarTimerInactividad();
      return;
    }

    const onActividad = () => reiniciarTimerInactividad();
    EVENTOS_ACTIVIDAD.forEach(event => window.addEventListener(event, onActividad));
    reiniciarTimerInactividad();

    return () => {
      EVENTOS_ACTIVIDAD.forEach(event => window.removeEventListener(event, onActividad));
      limpiarTimerInactividad();
    };
  }, [token, reiniciarTimerInactividad, limpiarTimerInactividad]);

  const login = (nuevoToken, nuevoUsuario) => {
    setToken(nuevoToken);
    setUsuario(nuevoUsuario);
    setMensajeExpiracion(null);
    sessionStorage.setItem('token', nuevoToken);
    sessionStorage.setItem('usuario', JSON.stringify(nuevoUsuario));
  };

  return (
    <AuthContext.Provider value={{ usuario, token, login, logout, loading, mensajeExpiracion }}>
      {children}
    </AuthContext.Provider>
  );
}

// El hook comparte el contexto definido en este módulo.
// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  return useContext(AuthContext);
}
