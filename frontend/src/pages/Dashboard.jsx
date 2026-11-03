import { useState, useEffect, useMemo, useCallback, useRef, memo } from 'react';
import { toast } from 'sonner';
import { useTheme } from '../contexts/ThemeContext';
import api from '../services/api';
import { FiSmartphone, FiDollarSign, FiAlertTriangle, FiDownload, FiPieChart, FiTrendingUp } from 'react-icons/fi';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
  AreaChart, Area,
} from 'recharts';

const meses = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

const fmt = (n) => Number(n).toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const fmtInt = (n) => Number(n).toLocaleString('es-ES');

const CustomTooltip = memo(({ active, payload, label }) => {
  const { theme } = useTheme();
  const isLight = theme === 'light';
  const tooltipStyle = {
    background: isLight ? '#ffffff' : '#1a365d',
    color: isLight ? '#1a202c' : '#ffffff',
    padding: '0.5rem 0.75rem',
    borderRadius: 6,
    border: `1px solid ${isLight ? '#e2e8f0' : '#334155'}`,
    boxShadow: isLight ? '0 4px 12px rgba(0,0,0,0.1)' : '0 8px 24px rgba(0,0,0,0.3)',
    fontSize: 12,
  };
  const textColor = isLight ? '#1a202c' : '#ffffff';
  if (active && payload && payload.length) {
    return (
      <div style={tooltipStyle}>
        <p style={{ margin: 0, fontWeight: 600, color: textColor, marginBottom: 3 }}>{label}</p>
        {payload.map((entry, index) => (
          <p key={index} style={{ margin: '1px 0', color: entry.color || textColor, fontSize: 11 }}>
            {entry.name}: <span style={{ color: textColor, fontWeight: 600 }}>{entry.value}</span>
          </p>
        ))}
      </div>
    );
  }
  return null;
});

const Skeleton = () => (
  <div>
    <h1 style={{ margin: '0 0 1.5rem', fontSize: '1.2rem', fontWeight: 800 }}>Dashboard</h1>
    <div className="stat-cards-grid">
      {[1, 2, 3, 4, 5].map(i => (
        <div key={i} className="card" style={{ height: 88, animation: 'pulse 1.5s infinite', background: 'var(--bg-card)' }} />
      ))}
    </div>
    <div className="grid-2" style={{ marginBottom: '1.25rem' }}>
      <div className="card" style={{ height: 300, background: 'var(--bg-card)' }} />
      <div className="card" style={{ height: 300, background: 'var(--bg-card)' }} />
    </div>
    <div className="card" style={{ height: 220, background: 'var(--bg-card)' }} />
  </div>
);

const StatCards = memo(({ totals, chartData, chartColors }) => {
  const trendDirection = useMemo(() => {
    if (chartData.length < 2) return 0;
    return chartData[chartData.length - 1].total - chartData[chartData.length - 2].total;
  }, [chartData]);

  const sparkLine = useMemo(() => chartData.length >= 2 && (
    <div className="sparkline-container">
      <AreaChart width={120} height={22} data={chartData}>
        <defs>
          <linearGradient id="sparkGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={chartColors.voz} stopOpacity={0.3} />
            <stop offset="100%" stopColor={chartColors.voz} stopOpacity={0} />
          </linearGradient>
        </defs>
        <Area type="monotone" dataKey="total" stroke={chartColors.voz} fill="url(#sparkGradient)" strokeWidth={2} dot={false} />
      </AreaChart>
    </div>
  ), [chartData, chartColors.voz]);

  const sobregastoColor = totals.sobregastoPromedio > 0 ? 'var(--warning)' : 'var(--success)';
  const trendColor = trendDirection > 0 ? 'var(--danger)' : 'var(--success)';

  return (
    <div className="stat-cards-grid">
      <div className="dashboard-card">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.4rem' }}>
          <div className="stat-icon" style={{ background: 'var(--primary-light)', color: 'var(--primary)' }}>
            <FiSmartphone size={13} />
          </div>
          <p className="stat-label">Total Servicios</p>
        </div>
        <p className="stat-value">{fmtInt(totals.totalServicios)}</p>
      </div>

      <div className="dashboard-card">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.4rem' }}>
          <div className="stat-icon" style={{ background: 'var(--info-light)', color: 'var(--info)' }}>
            <FiPieChart size={13} />
          </div>
          <p className="stat-label">% Líneas con Datos</p>
        </div>
        <p className="stat-value">{totals.promedioLineasDatos}%</p>
      </div>

      <div className="dashboard-card">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.4rem' }}>
          <div className="stat-icon" style={{ background: 'var(--success-light)', color: 'var(--success)' }}>
            <FiDollarSign size={13} />
          </div>
          <p className="stat-label">Total Consumido</p>
        </div>
        <p className="stat-value">${fmt(totals.totalConsumido)}</p>
        {sparkLine}
      </div>

      <div className="dashboard-card">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.4rem' }}>
          <div className="stat-icon" style={{ background: 'var(--primary-light)', color: 'var(--primary)' }}>
            <FiTrendingUp size={13} />
          </div>
          <p className="stat-label">Promedio Mensual</p>
        </div>
        <p className="stat-value">${fmt(totals.promedioRealMensual)}</p>
      </div>

      <div className="dashboard-card">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.4rem' }}>
          <div className="stat-icon" style={{ background: 'var(--warning-light)', color: 'var(--warning)' }}>
            <FiAlertTriangle size={13} />
          </div>
          <p className="stat-label">Sobregasto Promedio</p>
        </div>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.4rem' }}>
          <p className="stat-value" style={{ color: sobregastoColor }}>
            {totals.sobregastoPromedio}%
          </p>
          {trendDirection !== 0 && (
            <span style={{ fontSize: 11, fontWeight: 600, color: trendColor }}>
              {trendDirection > 0 ? '↑' : '↓'}
            </span>
          )}
        </div>
      </div>
    </div>
  );
});

const ChartsSection = memo(({ chartData, comparativaData, comparativa, chartColors, isDark }) => {
  const hasComparativa = comparativaData.length > 0 && comparativaData.some(d => d.actual > 0 || d.anterior > 0);

  return (
    <div className="grid-2" style={{ marginBottom: '1.25rem' }}>
      <div className="card">
        <h3 className="chart-title">Consumo Mensual · Voz + Datos</h3>
        <div style={{ width: '100%', height: 260 }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} margin={{ top: 5, right: 10, left: -10, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke={chartColors.grid} strokeOpacity={chartColors.gridOpacity} />
              <XAxis dataKey="mesLabel" tick={{ fill: chartColors.tick, fontSize: 11 }} axisLine={{ stroke: chartColors.grid }} tickLine={false} />
              <YAxis tick={{ fill: chartColors.tick, fontSize: 11 }} axisLine={false} tickLine={false} />
              <Tooltip content={<CustomTooltip />} cursor={{ fill: isDark ? '#334155' : '#f1f5f9', opacity: 0.5 }} />
              <Legend iconType="circle" iconSize={7} wrapperStyle={{ paddingTop: 6, fontSize: 11 }} />
              <Bar dataKey="total_voz" name="Voz (min)" fill={chartColors.voz} radius={[4, 4, 0, 0]} maxBarSize={28} />
              <Bar dataKey="total_datos" name="Datos (MB)" fill={chartColors.datos} radius={[4, 4, 0, 0]} maxBarSize={28} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {hasComparativa ? (
        <div className="card">
          <h3 className="chart-title">Comparativa · {comparativa.anio_anterior} vs {comparativa.anio_actual}</h3>
          <div style={{ width: '100%', height: 260 }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={comparativaData} margin={{ top: 5, right: 10, left: -10, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke={chartColors.grid} strokeOpacity={chartColors.gridOpacity} />
                <XAxis dataKey="mes" tick={{ fill: chartColors.tick, fontSize: 11 }} axisLine={{ stroke: chartColors.grid }} tickLine={false} />
                <YAxis tick={{ fill: chartColors.tick, fontSize: 11 }} axisLine={false} tickLine={false} />
                <Tooltip content={<CustomTooltip />} cursor={{ fill: isDark ? '#334155' : '#f1f5f9', opacity: 0.5 }} />
                <Legend iconType="circle" iconSize={7} wrapperStyle={{ paddingTop: 6, fontSize: 11 }} />
                <Bar dataKey="anterior" name={`${comparativa.anio_anterior}`} fill={chartColors.datos} radius={[4, 4, 0, 0]} maxBarSize={28} />
                <Bar dataKey="actual" name={`${comparativa.anio_actual}`} fill={chartColors.voz} radius={[4, 4, 0, 0]} maxBarSize={28} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      ) : (
        <div className="card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div className="empty-state">
            <p>No hay datos comparativos disponibles</p>
          </div>
        </div>
      )}
    </div>
  );
});

const UEBTable = memo(({ resumen }) => (
  <div className="table-container" style={{ marginBottom: '1rem' }}>
    <h3 className="chart-title" style={{ margin: 0 }}>
      Resumen por UEB
    </h3>
    <div className="dashboard-table-container">
      <table className="dashboard-table">
        <thead>
          <tr>
            <th>UEB</th>
            <th>Servicios</th>
            <th>Voz (min)</th>
            <th>Datos (MB)</th>
            <th>% Datos</th>
            <th>Plan Anual</th>
            <th>Prom. Mensual</th>
            <th>Est. Cierre</th>
            <th>vs Plan</th>
            <th>% Sobregasto</th>
          </tr>
        </thead>
        <tbody>
          {resumen.map((r) => {
            const diff = r.diferencia || 0;
            const sobre = r.pct_sobregasto || 0;
            return (
              <tr key={r.ueb_id}>
                <td style={{ fontWeight: 600 }}>{r.ueb_nombre}</td>
                <td>{r.cantidad_servicios}</td>
                <td>{r.total_voz != null ? fmtInt(r.total_voz) : '-'}</td>
                <td>{r.total_datos != null ? fmtInt(r.total_datos) : '-'}</td>
                <td>{r.pct_lineas_con_datos}%</td>
                <td>${fmt(r.plan_anual)}</td>
                <td>${fmt(r.promedio_real_mensual)}</td>
                <td>${fmt(r.estimado_cierre)}</td>
                <td className={diff >= 0 ? 'text-positive' : 'text-negative'}>{diff >= 0 ? `+${fmtInt(diff)}` : fmtInt(diff)}</td>
                <td>
                  <span className={`badge ${sobre > 0 ? 'badge-warning' : 'badge-success'}`}>
                    {sobre}%
                  </span>
                </td>
              </tr>
            );
          })}
          {resumen.length === 0 && (
            <tr>
              <td colSpan={10} style={{ textAlign: 'center', padding: '0.75rem', color: 'var(--text-muted)' }}>
                No hay datos disponibles
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  </div>
));

const Dashboard = () => {
  const { theme } = useTheme();
  const [anio, setAnio] = useState(new Date().getFullYear());
  const [aniosDisponibles, setAniosDisponibles] = useState([]);
  const [resumen, setResumen] = useState([]);
  const [totalesMensuales, setTotalesMensuales] = useState([]);
  const [comparativa, setComparativa] = useState(null);
  const [loading, setLoading] = useState(true);
  const abortRef = useRef(null);

  const isDark = theme === 'dark';
  const chartColors = useMemo(() => ({
    voz: isDark ? '#60a5fa' : '#2563eb',
    datos: isDark ? '#94a3b8' : '#64748b',
    tick: isDark ? '#94a3b8' : '#475569',
    grid: isDark ? '#334155' : '#e2e8f0',
    gridOpacity: 0.5,
  }), [isDark]);

  useEffect(() => {
    const anioActual = new Date().getFullYear();
    const anios = [];
    for (let a = 2020; a <= anioActual + 2; a++) {
      anios.push(a);
    }
    setAniosDisponibles(anios);
  }, []);

  const cargarResumen = useCallback(async (a, signal) => {
    try {
      const res = await api.get(`/dashboard/resumen?anio=${a}`, { signal });
      setResumen(res.data.uebs || []);
    } catch (err) {
      if (err.name !== 'CanceledError' && err.code !== 'ERR_CANCELED') {
        setResumen([]);
      }
    }
  }, []);

  const cargarTotalesMensuales = useCallback(async (a, signal) => {
    try {
      const res = await api.get(`/dashboard/totales?anio=${a}`, { signal });
      setTotalesMensuales(res.data.meses || []);
    } catch (err) {
      if (err.name !== 'CanceledError' && err.code !== 'ERR_CANCELED') {
        setTotalesMensuales([]);
      }
    }
  }, []);

  const cargarComparativa = useCallback(async (a, signal) => {
    try {
      const res = await api.get(`/dashboard/comparativa?anio=${a}`, { signal });
      setComparativa(res.data);
    } catch (err) {
      if (err.name !== 'CanceledError' && err.code !== 'ERR_CANCELED') {
        setComparativa(null);
      }
    }
  }, []);

  useEffect(() => {
    if (abortRef.current) abortRef.current.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    let cancelled = false;
    const fetchData = async () => {
      setLoading(true);
      await Promise.all([
        cargarResumen(anio, controller.signal),
        cargarTotalesMensuales(anio, controller.signal),
        cargarComparativa(anio, controller.signal),
      ]);
      if (!cancelled) setLoading(false);
    };
    fetchData();

    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [anio, cargarResumen, cargarTotalesMensuales, cargarComparativa]);

  const exportarExcel = useCallback(async () => {
    try {
      const response = await api.get(`/exportar/dashboard/excel?anio=${anio}`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `dashboard_${anio}.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      toast.success('Excel exportado correctamente');
    } catch (error) {
      toast.error('Error al exportar Excel');
    }
  }, [anio]);

  const totals = useMemo(() => ({
    totalServicios: resumen.reduce((s, r) => s + (r.cantidad_servicios || 0), 0),
    totalConsumido: resumen.reduce((s, r) => s + (r.total_consumido || 0), 0),
    promedioLineasDatos: resumen.length > 0
      ? Math.round(resumen.reduce((s, r) => s + (r.pct_lineas_con_datos || 0), 0) / resumen.length)
      : 0,
    promedioRealMensual: resumen.length > 0
      ? resumen.reduce((s, r) => s + (r.promedio_real_mensual || 0), 0)
      : 0,
    sobregastoPromedio: resumen.length > 0
      ? Math.round(resumen.reduce((s, r) => s + (r.pct_sobregasto || 0), 0) / resumen.length)
      : 0,
  }), [resumen]);

  const chartData = useMemo(() => totalesMensuales.map(m => ({
    ...m,
    mesLabel: meses[m.mes - 1] || m.mes,
    total: (m.total_voz || 0) + (m.total_datos || 0),
  })), [totalesMensuales]);

  const comparativaData = useMemo(() => {
    if (!comparativa) return [];
    const data = [];
    for (let i = 1; i <= 12; i++) {
      const a = (comparativa.actual || []).find(m => m.mes === i);
      const p = (comparativa.anterior || []).find(m => m.mes === i);
      data.push({
        mes: meses[i - 1],
        actual: a ? (a.total_voz || 0) + (a.total_datos || 0) : 0,
        anterior: p ? (p.total_voz || 0) + (p.total_datos || 0) : 0,
      });
    }
    return data;
  }, [comparativa]);

  if (loading && !resumen.length && !totalesMensuales.length) {
    return <Skeleton />;
  }

  return (
    <div style={{ animation: 'fadeInUp 0.4s ease', maxWidth: '100%' }}>
      <div className="page-header">
        <h1>
          Dashboard
          <span style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary)', marginLeft: '0.75rem' }}>
            {anio}
          </span>
        </h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <select
            value={anio}
            onChange={(e) => setAnio(Number(e.target.value))}
            className="form-select"
            style={{ width: 90 }}
          >
            {aniosDisponibles.map((a) => <option key={a} value={a}>{a}</option>)}
          </select>
          <button onClick={exportarExcel} className="btn btn-primary btn-sm" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <FiDownload size={14} /> Exportar Excel
          </button>
        </div>
      </div>

      <StatCards totals={totals} chartData={chartData} chartColors={chartColors} />
      <ChartsSection chartData={chartData} comparativaData={comparativaData} comparativa={comparativa} chartColors={chartColors} isDark={isDark} />
      <UEBTable resumen={resumen} />
    </div>
  );
};

export default Dashboard;
