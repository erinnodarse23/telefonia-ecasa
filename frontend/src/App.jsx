import { Suspense } from 'react';
import { Navigate, Routes, Route } from 'react-router-dom';
import { Toaster } from 'sonner';
import { useAuth } from './contexts/AuthContext';
import { ThemeProvider } from './contexts/ThemeContext';
import ErrorBoundary from './components/ErrorBoundary';
import Layout from './components/Layout';
import PageSkeleton from './components/PageSkeleton';
import ProtectedRoute from './components/ProtectedRoute';
import { Login, Dashboard, UEB, Empleados, Equipos, Asignaciones, Planes, CargaConsumo, ConsumoHistorico, Reportes, Usuarios, Configuracion, Trazas, Alertas } from './utils/pagePrefetch';

function App() {
  const { loading, token } = useAuth();
  if (loading) return null;

  return (
    <ThemeProvider>
      <ErrorBoundary>
        <Toaster position="top-right" richColors closeButton />
        <Suspense fallback={<PageSkeleton />}>
          <Routes>
            <Route path="/login" element={token ? <Navigate to="/dashboard" replace /> : <Login />} />
            <Route
              element={
                <ProtectedRoute>
                  <Layout />
                </ProtectedRoute>
              }
            >
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/ueb" element={<UEB />} />
              <Route path="/empleados" element={<Empleados />} />
              <Route path="/equipos" element={<Equipos />} />
              <Route path="/asignaciones" element={<Asignaciones />} />
              <Route path="/planes" element={<Planes />} />
              <Route path="/carga" element={<CargaConsumo />} />
              <Route path="/consumo-historico" element={<ConsumoHistorico />} />
              <Route path="/reportes" element={<Reportes />} />
              <Route path="/alertas" element={<Alertas />} />
              <Route path="/usuarios" element={<Usuarios />} />
              <Route path="/configuracion" element={<Configuracion />} />
              <Route path="/trazas" element={<Trazas />} />
              <Route path="/" element={<Navigate to="/dashboard" />} />
            </Route>
          </Routes>
        </Suspense>
      </ErrorBoundary>
    </ThemeProvider>
  );
}

export default App;
