import { lazy } from 'react';

const loadLogin = () => import('../pages/Login');
const loadDashboard = () => import('../pages/Dashboard');
const loadUEB = () => import('../pages/UEB');
const loadEmpleados = () => import('../pages/Empleados');
const loadEquipos = () => import('../pages/Equipos');
const loadAsignaciones = () => import('../pages/Asignaciones');
const loadPlanes = () => import('../pages/Planes');
const loadCargaConsumo = () => import('../pages/CargaConsumo');
const loadConsumoHistorico = () => import('../pages/ConsumoHistorico');
const loadReportes = () => import('../pages/Reportes');
const loadUsuarios = () => import('../pages/Usuarios');
const loadConfiguracion = () => import('../pages/Configuracion');
const loadTrazas = () => import('../pages/Trazas');
const loadAlertas = () => import('../pages/Alertas');
const loadMonitoreo = () => import('../pages/Monitoreo');
const loadPresupuesto = () => import('../pages/Presupuesto');
const loadTendencias = () => import('../pages/Tendencias');

export const Login = lazy(loadLogin);
export const Dashboard = lazy(loadDashboard);
export const UEB = lazy(loadUEB);
export const Empleados = lazy(loadEmpleados);
export const Equipos = lazy(loadEquipos);
export const Asignaciones = lazy(loadAsignaciones);
export const Planes = lazy(loadPlanes);
export const CargaConsumo = lazy(loadCargaConsumo);
export const ConsumoHistorico = lazy(loadConsumoHistorico);
export const Reportes = lazy(loadReportes);
export const Usuarios = lazy(loadUsuarios);
export const Configuracion = lazy(loadConfiguracion);
export const Trazas = lazy(loadTrazas);
export const Alertas = lazy(loadAlertas);
export const Monitoreo = lazy(loadMonitoreo);
export const Presupuesto = lazy(loadPresupuesto);
export const Tendencias = lazy(loadTendencias);

export const routePrefetch = {
  '/login': loadLogin,
  '/dashboard': loadDashboard,
  '/ueb': loadUEB,
  '/empleados': loadEmpleados,
  '/equipos': loadEquipos,
  '/asignaciones': loadAsignaciones,
  '/planes': loadPlanes,
  '/carga': loadCargaConsumo,
  '/consumo-historico': loadConsumoHistorico,
  '/reportes': loadReportes,
  '/usuarios': loadUsuarios,
  '/configuracion': loadConfiguracion,
  '/trazas': loadTrazas,
  '/alertas': loadAlertas,
  '/monitoreo': loadMonitoreo,
  '/presupuesto': loadPresupuesto,
  '/tendencias': loadTendencias,
};
