import { useState, useEffect, useCallback } from 'react';
import { toast } from 'sonner';
import { useAuth } from '../contexts/AuthContext';
import { get, post } from '../services/api';
import { FiDollarSign, FiEdit2, FiSave, FiX } from 'react-icons/fi';

const fmt = (n) => Number(n ?? 0).toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

function Presupuesto() {
  const { usuario } = useAuth();
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [anio, setAnio] = useState(new Date().getFullYear());
  const [editando, setEditando] = useState(null);
  const [valorEdit, setValorEdit] = useState('');
  const [guardando, setGuardando] = useState(false);
  const esAdmin = usuario?.rol === 'Administrador';

  const cargar = useCallback(async () => {
    setLoading(true);
    try {
      const result = await get(`/presupuesto?anio=${anio}`);
      setData(Array.isArray(result) ? result : []);
    } catch {
      toast.error('Error al cargar presupuesto');
      setData([]);
    } finally {
      setLoading(false);
    }
  }, [anio]);

  useEffect(() => { cargar(); }, [cargar]);

  const iniciarEdicion = (row) => {
    setEditando(row.ueb_id);
    setValorEdit(String(row.asignado ?? 0));
  };

  const cancelarEdicion = () => {
    setEditando(null);
    setValorEdit('');
  };

  const guardar = async (row) => {
    const monto = parseFloat(valorEdit);
    if (isNaN(monto) || monto < 0) { toast.error('Monto inválido'); return; }
    setGuardando(true);
    try {
      await post('/presupuesto', { anio, uebId: row.ueb_id, asignado: monto });
      toast.success(`Presupuesto de ${row.codigo} actualizado`);
      setEditando(null);
      setValorEdit('');
      await cargar();
    } catch (error) {
      toast.error(error.response?.data?.error || 'Error al guardar');
    } finally {
      setGuardando(false);
    }
  };

  const badgeClass = (estado) => {
    if (estado === 'rojo') return 'badge badge-danger';
    if (estado === 'amarillo') return 'badge badge-warning';
    if (estado === 'sin_presupuesto') return 'badge badge-outline';
    return 'badge badge-success';
  };

  const totales = {
    asignado: data.reduce((s, r) => s + Number(r.asignado ?? 0), 0),
    ejecutado: data.reduce((s, r) => s + Number(r.ejecutado ?? 0), 0),
  };

  return (
    <div>
      <div className="page-header">
        <h1 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 800 }}>
          <FiDollarSign style={{ marginRight: 8, verticalAlign: 'middle' }} />
          Presupuesto Anual
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
      ) : (
        <>
          <div className="stat-cards-grid">
            <div className="dashboard-card">
              <div style={{ fontSize: '0.75rem', opacity: 0.7 }}>Total asignado</div>
              <div style={{ fontSize: '1.5rem', fontWeight: 800 }}>{fmt(totales.asignado)}</div>
            </div>
            <div className="dashboard-card">
              <div style={{ fontSize: '0.75rem', opacity: 0.7 }}>Total ejecutado</div>
              <div style={{ fontSize: '1.5rem', fontWeight: 800 }}>{fmt(totales.ejecutado)}</div>
            </div>
            <div className="dashboard-card">
              <div style={{ fontSize: '0.75rem', opacity: 0.7 }}>Disponible global</div>
              <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#16a34a' }}>{fmt(totales.asignado - totales.ejecutado)}</div>
            </div>
          </div>

          {data.length === 0 ? (
            <div className="empty-state"><p>No hay datos de presupuesto para {anio}</p></div>
          ) : (
            <div className="table-container">
              <table className="table-responsive dashboard-table">
                <thead>
                  <tr>
                    <th>UEB</th>
                    <th style={{ textAlign: 'right' }}>Asignado</th>
                    <th style={{ textAlign: 'right' }}>Ejecutado</th>
                    <th style={{ textAlign: 'right' }}>Disponible</th>
                    <th style={{ textAlign: 'right' }}>% Ejecución</th>
                    <th>Estado</th>
                    {esAdmin && <th style={{ textAlign: 'center' }}>Acciones</th>}
                  </tr>
                </thead>
                <tbody>
                  {data.map((row) => (
                    <tr key={row.ueb_id}>
                      <td>{row.codigo} - {row.ueb}</td>
                      <td style={{ textAlign: 'right' }}>
                        {editando === row.ueb_id ? (
                          <input type="number" className="form-input" value={valorEdit}
                            onChange={(e) => setValorEdit(e.target.value)}
                            style={{ width: 120, textAlign: 'right' }} autoFocus />
                        ) : fmt(row.asignado)}
                      </td>
                      <td style={{ textAlign: 'right' }}>{fmt(row.ejecutado)}</td>
                      <td style={{ textAlign: 'right', color: Number(row.disponible) < 0 ? '#dc2626' : undefined }}>
                        {fmt(row.disponible)}
                      </td>
                      <td style={{ textAlign: 'right' }}>{fmt(row.porcentaje_ejecucion)}%</td>
                      <td><span className={badgeClass(row.estado)}>{row.estado?.replace('_', ' ')}</span></td>
                      {esAdmin && (
                        <td style={{ textAlign: 'center' }}>
                          {editando === row.ueb_id ? (
                            <>
                              <button className="btn btn-primary btn-sm" onClick={() => guardar(row)} disabled={guardando} style={{ marginRight: 4 }}>
                                <FiSave />
                              </button>
                              <button className="btn btn-secondary btn-sm" onClick={cancelarEdicion} disabled={guardando}>
                                <FiX />
                              </button>
                            </>
                          ) : (
                            <button className="btn btn-secondary btn-sm" onClick={() => iniciarEdicion(row)}>
                              <FiEdit2 />
                            </button>
                          )}
                        </td>
                      )}
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

export default Presupuesto;