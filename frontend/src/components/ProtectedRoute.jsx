import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const ROLES_POR_RUTA = {
  '/dashboard': ['Administrador', 'GrupoComunicaciones', 'JefeUnidad'],
  '/ueb': ['Administrador', 'GrupoComunicaciones'],
  '/empleados': ['Administrador', 'GrupoComunicaciones'],
  '/equipos': ['Administrador', 'GrupoComunicaciones'],
  '/asignaciones': ['Administrador', 'GrupoComunicaciones'],
  '/planes': ['Administrador', 'GrupoComunicaciones'],
  '/carga': ['Administrador', 'GrupoComunicaciones'],
  '/consumo-historico': ['Administrador', 'GrupoComunicaciones'],
  '/reportes': ['Administrador', 'GrupoComunicaciones', 'JefeUnidad'],
  '/usuarios': ['Administrador'],
  '/configuracion': ['Administrador'],
  '/trazas': ['Administrador'],
};

function getMatchingRoute(pathname) {
  const exact = ROLES_POR_RUTA[pathname];
  if (exact) return exact;
  const prefix = Object.keys(ROLES_POR_RUTA).find((r) => pathname.startsWith(r));
  return prefix ? ROLES_POR_RUTA[prefix] : null;
}

export default function ProtectedRoute({ children }) {
  const { token, usuario, loading } = useAuth();
  const location = useLocation();

  if (loading) return null;

  if (!token) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  const rolesPermitidos = getMatchingRoute(location.pathname);
  if (rolesPermitidos && !rolesPermitidos.includes(usuario?.rol)) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
}
