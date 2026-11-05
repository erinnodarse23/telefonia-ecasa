import { useState, useRef, useEffect } from 'react';
import { Outlet, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';
import ThemeToggle from './ThemeToggle';
import SidebarItem from './SidebarItem';
import ConfirmModal from './ConfirmModal';
import api from '../services/api';
import { FiHome, FiGrid, FiUsers, FiSmartphone, FiUserCheck, FiFileText, FiUploadCloud, FiBarChart2, FiShield, FiSettings, FiClock, FiChevronLeft, FiChevronRight, FiBell, FiActivity, FiDollarSign, FiTrendingUp } from 'react-icons/fi';

function Layout() {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [alertasOpen, setAlertasOpen] = useState(false);
  const [alertsCount, setAlertsCount] = useState(0);
  const [recentAlerts, setRecentAlerts] = useState([]);
  const [limpiarModalOpen, setLimpiarModalOpen] = useState(false);
  const { usuario, logout } = useAuth();
  const { theme } = useTheme();
  const navigate = useNavigate();
  const dropdownRef = useRef(null);
  const alertasRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false);
      }
      if (alertasRef.current && !alertasRef.current.contains(e.target)) {
        setAlertasOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const fetchAlertCount = async () => {
    try {
      const { data } = await api.get('/alertas/contar');
      setAlertsCount(data.total || 0);
    } catch {
      setAlertsCount(0);
    }
  };

  const fetchRecentAlerts = async () => {
    try {
      const { data } = await api.get('/alertas/recientes?limite=5');
      setRecentAlerts(Array.isArray(data) ? data : []);
    } catch {
      setRecentAlerts([]);
    }
  };

  useEffect(() => {
    if (!usuario) return;
    fetchAlertCount();
    fetchRecentAlerts();
    const interval = setInterval(fetchAlertCount, 30000);
    return () => clearInterval(interval);
  }, [usuario]);

  const handleAlertasToggle = async () => {
    if (!alertasOpen) {
      await fetchRecentAlerts();
    }
    setAlertasOpen(!alertasOpen);
  };

  const handleLimpiarClick = () => {
    setLimpiarModalOpen(true);
  };

  const handleConfirmLimpiar = async () => {
    try {
      const { data } = await api.post('/notificaciones/limpiar');
      setAlertsCount(0);
      setRecentAlerts([]);
      setAlertasOpen(false);
    } catch {
      // silencioso
    } finally {
      setLimpiarModalOpen(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isAdmin = usuario?.rol === 'Administrador';
  const isComunicaciones = usuario?.rol === 'GrupoComunicaciones';
  const isJefe = usuario?.rol === 'JefeUnidad';

  const menuItems = [];

  if (isAdmin || isComunicaciones || isJefe) {
    menuItems.push({ to: '/dashboard', label: 'Dashboard', icon: FiHome });
    menuItems.push({ to: '/monitoreo', label: 'Monitoreo', icon: FiActivity });
    menuItems.push({ to: '/presupuesto', label: 'Presupuesto', icon: FiDollarSign });
    menuItems.push({ to: '/tendencias', label: 'Tendencias', icon: FiTrendingUp });
  }

  if (isAdmin || isComunicaciones) {
    menuItems.push(
      { to: '/ueb', label: 'UEB', icon: FiGrid },
      { to: '/empleados', label: 'Empleados', icon: FiUsers },
      { to: '/equipos', label: 'Equipos', icon: FiSmartphone },
      { to: '/asignaciones', label: 'Asignaciones', icon: FiUserCheck },
      { to: '/planes', label: 'Planes', icon: FiFileText },
      { to: '/consumo-historico', label: 'Consumo Histórico', icon: FiClock },
      { to: '/carga', label: 'Importar Consumo', icon: FiUploadCloud },
    );
  }

  if (isAdmin || isComunicaciones || isJefe) {
    menuItems.push({ to: '/reportes', label: 'Reportes', icon: FiBarChart2 });
    menuItems.push({ to: '/alertas', label: 'Alertas', icon: FiBell });
  }

  if (isAdmin) {
    menuItems.push({ to: '/usuarios', label: 'Usuarios', icon: FiShield });
    menuItems.push({ to: '/trazas', label: 'Trazas', icon: FiClock });
    menuItems.push({ to: '/configuracion', label: 'Parámetros', icon: FiSettings });
  }

  const userInitial = (usuario?.login || 'U')[0].toUpperCase();

  return (
    <div className="layout-container">
      <aside className={`sidebar ${sidebarCollapsed ? 'collapsed' : ''}`}>
        <div className="sidebar-header">
          <div className="logo-container" onClick={() => navigate('/dashboard')}>
            {sidebarCollapsed ? (
              <span className="logo-text logo-text--collapsed">E</span>
            ) : (
              <span className="logo-text">ECASA</span>
            )}
          </div>
        </div>
        <nav className="sidebar-nav">
          {menuItems.map((item) => {
            const Icon = item.icon;
            return (
              <SidebarItem
                key={item.to}
                to={item.to}
                text={item.label}
                icon={Icon ? <Icon /> : 'Ã¢â‚¬Â¢'}
                collapsed={sidebarCollapsed}
              />
            );
          })}
        </nav>
      </aside>

      <div className="main-wrapper">
        <header className="header">
          <div className="header-left">
            <button className="sidebar-toggle" onClick={() => setSidebarCollapsed(!sidebarCollapsed)}>
              {sidebarCollapsed ? <FiChevronRight size={18} /> : <FiChevronLeft size={18} />}
            </button>
            <h2 className="header-title">ECASA Telefonía</h2>
          </div>
          <div className="header-actions">
            <div className="alertas-dropdown" ref={alertasRef}>
              <button className="alertas-trigger" onClick={handleAlertasToggle} aria-label="Alertas">
                <FiBell size={18} />
                {alertsCount > 0 && <span className="alertas-badge">{alertsCount > 99 ? '99+' : alertsCount}</span>}
              </button>
              {alertasOpen && (
                <div className={`alertas-menu ${theme}`}>
                  <div className="alertas-header">
                    <span className="alertas-title">Alertas de Sobregasto</span>
                    {alertsCount > 0 && (
                      <button className="alertas-leer-todas" onClick={handleLimpiarClick}>Limpiar todo</button>
                    )}
                  </div>
                  <div className="alertas-list">
                    {recentAlerts.length === 0 ? (
                      <div className="alertas-empty">No hay alertas recientes</div>
                    ) : (
                      recentAlerts.map((a) => (
                        <div key={a.id} className={`alerta-item${a.leida ? '' : ' alerta-item--no-leida'}`}>
                          <div className="alerta-item-icon">
                            <span className={`badge ${a.tipo === 'EXCESO_UMBRAL' ? 'badge-danger' : 'badge-warning'}`}>
                              {a.tipo === 'EXCESO_UMBRAL' ? '!' : '%'}
                            </span>
                          </div>
                          <div className="alerta-item-content">
                            <p className="alerta-item-mensaje">{a.mensaje}</p>
                            <span className="alerta-item-ueb">{a.ueb_nombre || a.ueb_codigo}</span>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                  <div className="alertas-footer">
                    <Link to="/alertas" onClick={() => setAlertasOpen(false)}>Ver todas las alertas</Link>
                  </div>
                </div>
              )}
            </div>
            <ThemeToggle />
            <div className="user-dropdown" ref={dropdownRef}>
              <button
                className={`user-dropdown-trigger ${theme}`}
                onClick={() => setDropdownOpen(!dropdownOpen)}
                aria-haspopup="true"
                aria-expanded={dropdownOpen}
              >
                <span className="user-avatar">{userInitial}</span>
                <span className="user-name">{usuario?.login || 'Usuario'}</span>
              </button>
              {dropdownOpen && (
                <div className={`user-dropdown-menu ${theme}`}>
                  <div className="user-dropdown-header">
                    <span className="user-dropdown-avatar">{userInitial}</span>
                    <div className="user-dropdown-info">
                      <div className="user-dropdown-name">{usuario?.login || 'Usuario'}</div>
                      <div className="user-dropdown-rol">{usuario?.rol || ''}</div>
                    </div>
                  </div>
                  <div className="user-dropdown-divider" />
                  <button className="user-dropdown-logout" onClick={handleLogout}>
                    Cerrar Sesión
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>
        <main className="main-content">
          <Outlet />
        </main>
      </div>

      <ConfirmModal
        isOpen={limpiarModalOpen}
        onClose={() => setLimpiarModalOpen(false)}
        onConfirm={handleConfirmLimpiar}
        title="Limpiar notificaciones"
        message="Ã‚Â¿Estás seguro de que deseas limpiar todas las notificaciones?"
        confirmText="Sí, limpiar todo"
        variant="warning"
      />
    </div>
  );
}

export default Layout;
