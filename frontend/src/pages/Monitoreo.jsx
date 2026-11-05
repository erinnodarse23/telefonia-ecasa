import { useState, useEffect, useCallback } from 'react';
import { toast } from 'sonner';
import { useAuth } from '../contexts/AuthContext';
import { get } from '../services/api';
import { FiRefreshCw, FiActivity } from 'react-icons/fi';

const meses = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];
const fmt = (n) => Number(n ?? 0).toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

function Monitoreo() {
  const { usuario } = useAuth();
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [anio, setAnio] = useState(new Date().getFullYear());
  const [mes, setMes] = useState(new Date().getMonth() + 1);
  const [uebId, setUebId] = useState('');
  const esJefe = usuario?.rol === 'JefeUnidad';

  const cargar = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      const params = new URLSearchParams({ anio: String(anio), mes: String(mes) });
      if (uebId) params.set('uebId', uebId);
      const result = await get(`/monitoreo/estado?${params.toString()}`);
      setData(Array.isArray(result) ? result : []);
    } catch {
      if (!silent) toast.error('Error al cargar monitoreo');
      setData([]);
    } finally {
      if (!silent) setLoading(false);
    }
  }, [anio, mes, uebId]);

  useEffect(() => { cargar(); }, [cargar]);

  useEffect(() => {
    const intervalo = setInterval(() => cargar(true), 30000);
    return () => clearInterval(intervalo);
  }, [cargar]);

  const stats = {
    total: data.length,
    rojos: data.filter(d => d.estado === 'rojo').length,
    amarillos: data.filter(d => d.estado === 'amarillo').length,
    verdes: data.filter(d => d.estado === 'verde').length,
  };

  const badgeClass = (estado) => {
    if (estado === 'rojo') return 'badge badge-danger';
    if (estado === 'amarillo') return 'badge badge-warning';
    return 'badge badge-success';
  };

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 800 }}>
          <FiActivity style={{ marginRight: 8, verticalAlign: 'middle' }} />
          Monitoreo
        </h1>
        <button className="btn btn-secondary btn-sm" onClick={() => cargar()} disabled={loading}>
          <FiRefreshCw style={{ marginRight: 4, verticalAlign: 'middle' }} />
          Actualizar
        </button>
      </div>

      <div className="filter-bar">
        <select className="form-select" value={anio} onChange={(e) => setAnio(Number(e.target.value))}>
          {[new Date().getFullYear(), new Date().getFullYear() - 1, new Date().getFullYear() - 2].map(y => (
            <option key={y} value={y}>{y}</option>
          ))}
        </select>
        <select className="form-select" value={mes} onChange={(e) => setMes(Number(e.target.value))}>
          {meses.map((m, i) => <option key={i + 1} value={i + 1}>{m}</option>)}
        </select>
        {!esJefe && (
          <input className="form-input" type="number" placeholder="UEB ID (opcional)" value={uebId}
            onChange={(e) => setUebId(e.target.value)} style={{ maxWidth: 180 }} />
        )}
      </div>

      {loading ? (
        <div className="spinner" style={{ margin: '2rem auto' }} />
      ) : (
        <>
          <div className="stat-cards-grid">
            <div className="dashboard-card">
              <div style={{ fontSize: '0.75rem', opacity: 0.7 }}>Total equipos</div>
              <div style={{ fontSize: '1.8rem', fontWeight: 800 }}>{stats.total}</div>
            </div>
            <div className="dashboard-card">
              <div style={{ fontSize: '0.75rem', opacity: 0.7 }}>Rojos</div>
              <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#dc2626' }}>{stats.rojos}</div>
            </div>
            <div className="dashboard-card">
              <div style={{ fontSize: '0.75rem', opacity: 0.7 }}>Amarillos</div>
              <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#d97706' }}>{stats.amarillos}</div>
            </div>
            <div className="dashboard-card">
              <div style={{ fontSize: '0.75rem', opacity: 0.7 }}>Verdes</div>
              <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#16a34a' }}>{stats.verdes}</div>
            </div>
          </div>

          {data.length === 0 ? (
            <div className="empty-state"><p>No hay equipos activos para el período seleccionado</p></div>
          ) : (
            <div className="table-container">
              <table className="table-responsive dashboard-table">
                <thead>
                  <tr>
                    <th>Nº Línea</th>
                    <th>UEB</th>
                    <th style={{ textAlign: 'right' }}>Consumo</th>
                    <th style={{ textAlign: 'right' }}>Umbral</th>
                    <th style={{ textAlign: 'right' }}>% Sobregasto</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {data.map((row) => (
                    <tr key={row.equipo_id}>
                      <td>{row.numero_linea}</td>
                      <td>{row.ueb || <span style={{ opacity: 0.5 }}>Sin asignar</span>}</td>
                      <td style={{ textAlign: 'right' }}>{fmt(row.consumo_actual)}</td>
                      <td style={{ textAlign: 'right' }}>{fmt(row.umbral)}</td>
                      <td style={{ textAlign: 'right' }}>{fmt(row.porcentaje)}%</td>
                      <td><span className={badgeClass(row.estado)}>{row.estado}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default Monitoreo;