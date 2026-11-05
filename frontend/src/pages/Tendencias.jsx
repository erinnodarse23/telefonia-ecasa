import { useState, useEffect, useCallback, useMemo } from 'react';
import { toast } from 'sonner';
import { useTheme } from '../contexts/ThemeContext';
import { get } from '../services/api';
import { FiTrendingUp, FiTrendingDown, FiBarChart2 } from 'react-icons/fi';
import { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, Legend, BarChart, Bar } from 'recharts';

const meses = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
const fmt = (n) => Number(n ?? 0).toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

function Tendencias() {
  const { theme } = useTheme();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [anio, setAnio] = useState(new Date().getFullYear());
  const isLight = theme === 'light';

  const cargar = useCallback(async () => {
    setLoading(true);
    try {
      const result = await get(`/reportes/tendencias?anio=${anio}`);
      setData(result);
    } catch {
      toast.error('Error al cargar tendencias');
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [anio]);

  useEffect(() => { cargar(); }, [cargar]);

  const chartComparativa = useMemo(() => {
    if (!data?.comparativa_anual) return [];
    return data.comparativa_anual.actual.map((item, i) => ({
      mes: meses[i],
      actual: Number(item.total ?? 0),
      anterior: Number(data.comparativa_anual.anterior[i]?.total ?? 0),
    }));
  }, [data]);

  const chartMensual = useMemo(() => {
    if (!data?.consumos_mensuales) return [];
    return data.consumos_mensuales.map((item) => ({
      mes: meses[(item.mes ?? 1) - 1],
      voz: Number(item.total_voz ?? 0),
      datos: Number(item.total_datos ?? 0),
      sms: Number(item.total_sms ?? 0),
    }));
  }, [data]);

  const axisColor = isLight ? '#4a5568' : '#cbd5e0';
  const gridColor = isLight ? '#e2e8f0' : '#334155';

  return (
    <div>
      <div className="page-header">
        <h1 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 800 }}>
          <FiBarChart2 style={{ marginRight: 8, verticalAlign: 'middle' }} />
          Tendencias
        </h1>
      </div>

      <div className="filter-bar">
        <select className="form-select" value={anio} onChange={(e) => setAnio(Number(e.target.value))}>
          {[new Date().getFullYear(), new Date().getFullYear() - 1, new Date().getFullYear() - 2].map(y => (
            <option key={y} value={y}>{y}</option>
          ))}
        </select>
      </div>

      {loading ? (
        <div className="spinner" style={{ margin: '2rem auto' }} />
      ) : !data ? (
        <div className="empty-state"><p>No hay datos para {anio}</p></div>
      ) : (
        <>
          <div className="dashboard-card">
            <div className="chart-title">Comparativa anual</div>
            <ResponsiveContainer width="100%" height={280}>
              <AreaChart data={chartComparativa}>
                <defs>
                  <linearGradient id="gActual" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.8} />
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="gAnterior" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#94a3b8" stopOpacity={0.6} />
                    <stop offset="95%" stopColor="#94a3b8" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke={gridColor} />
                <XAxis dataKey="mes" stroke={axisColor} fontSize={11} />
                <YAxis stroke={axisColor} fontSize={11} />
                <Tooltip formatter={(v) => fmt(v)} />
                <Legend />
                <Area type="monotone" dataKey="actual" name={`Año ${data.comparativa_anual.anio_actual}`} stroke="#3b82f6" fill="url(#gActual)" />
                <Area type="monotone" dataKey="anterior" name={`Año ${data.comparativa_anual.anio_anterior}`} stroke="#94a3b8" fill="url(#gAnterior)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>

          <div className="dashboard-card" style={{ marginTop: '1rem' }}>
            <div className="chart-title">Consumo mensual por tipo ({anio})</div>
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={chartMensual}>
                <CartesianGrid strokeDasharray="3 3" stroke={gridColor} />
                <XAxis dataKey="mes" stroke={axisColor} fontSize={11} />
                <YAxis stroke={axisColor} fontSize={11} />
                <Tooltip formatter={(v) => fmt(v)} />
                <Legend />
                <Bar dataKey="voz" name="Voz" fill="#3b82f6" />
                <Bar dataKey="datos" name="Datos" fill="#8b5cf6" />
                <Bar dataKey="sms" name="SMS" fill="#f59e0b" />
              </BarChart>
            </ResponsiveContainer>
          </div>

          <div className="dashboard-card" style={{ marginTop: '1rem' }}>
            <div className="chart-title">
              <FiTrendingUp style={{ marginRight: 6, verticalAlign: 'middle', color: '#dc2626' }} />
              Top 10 mayor consumo
            </div>
            <div className="table-container">
              <table className="table-responsive dashboard-table">
                <thead>
                  <tr>
                    <th>Nº Línea</th>
                    <th>UEB</th>
                    <th style={{ textAlign: 'right' }}>Total</th>
                  </tr>
                </thead>
                <tbody>
                  {data.ranking_top.map((r) => (
                    <tr key={r.equipo_id}>
                      <td>{r.numero_linea}</td>
                      <td>{r.ueb || <span style={{ opacity: 0.5 }}>Sin asignar</span>}</td>
                      <td style={{ textAlign: 'right' }}>{fmt(r.total)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="dashboard-card" style={{ marginTop: '1rem' }}>
            <div className="chart-title">
              <FiTrendingDown style={{ marginRight: 6, verticalAlign: 'middle', color: '#16a34a' }} />
              Top 10 menor consumo
            </div>
            <div className="table-container">
              <table className="table-responsive dashboard-table">
                <thead>
                  <tr>
                    <th>Nº Línea</th>
                    <th>UEB</th>
                    <th style={{ textAlign: 'right' }}>Total</th>
                  </tr>
                </thead>
                <tbody>
                  {data.ranking_bottom.map((r) => (
                    <tr key={r.equipo_id}>
                      <td>{r.numero_linea}</td>
                      <td>{r.ueb || <span style={{ opacity: 0.5 }}>Sin asignar</span>}</td>
                      <td style={{ textAlign: 'right' }}>{fmt(r.total)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

export default Tendencias;