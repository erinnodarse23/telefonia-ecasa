import { useState, useEffect, useMemo } from 'react';
import { toast } from 'sonner';
import api from '../services/api';
import { FiEdit2, FiCheck, FiX, FiXCircle } from 'react-icons/fi';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts';

const meses = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

const chartColors = {
  voz: '#2563eb',
  datos: '#06b6d4',
  sms: '#8b5cf6',
};

const CustomTooltip = ({ active, payload, label }) => {
  if (active && payload && payload.length) {
    return (
      <div style={{
        backgroundColor: '#1e293b',
        color: '#ffffff',
        padding: '0.5rem 0.75rem',
        borderRadius: 6,
        border: '1px solid #334155',
        boxShadow: '0 8px 24px rgba(0,0,0,0.3)',
        fontSize: 13,
      }}>
        <p style={{ margin: 0, fontWeight: 600, color: '#ffffff', marginBottom: 4 }}>{label}</p>
        {payload.map((entry, index) => (
          <p key={index} style={{ margin: '2px 0', color: entry.color || '#ffffff', fontSize: 12 }}>
            {entry.name}: <span style={{ color: '#ffffff', fontWeight: 600 }}>{entry.value?.toFixed?.(1) ?? entry.value}</span>
          </p>
        ))}
      </div>
    );
  }
  return null;
};

const ConsumoHistorico = () => {
  const [equipos, setEquipos] = useState([]);
  const [equipoId, setEquipoId] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [dropdownAbierto, setDropdownAbierto] = useState(false);
  const [consumos, setConsumos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadingConsumo, setLoadingConsumo] = useState(false);
  const [selectedEquipo, setSelectedEquipo] = useState(null);
  const [editandoId, setEditandoId] = useState(null);
  const [editForm, setEditForm] = useState({ voz_consumido: 0, datos_consumido: 0, sms_consumido: 0 });
  const [filtroAnio, setFiltroAnio] = useState('');
  const [filtroMes, setFiltroMes] = useState(0);

  useEffect(() => {
    const init = async () => {
      try {
        const { data } = await api.get('/equipos?activo=true');
        setEquipos(data);
      } catch (err) {
        console.error('Error al cargar equipos:', err);
      } finally {
        setLoading(false);
      }
    };
    init();
  }, []);

  useEffect(() => {
    if (!equipoId) {
      setConsumos([]);
      setSelectedEquipo(null);
      return;
    }
    setFiltroAnio('');
    setFiltroMes(0);
    const cargar = async () => {
      setLoadingConsumo(true);
      try {
        const eq = equipos.find((e) => e.id === Number(equipoId));
        setSelectedEquipo(eq || null);
        const { data } = await api.get(`/consumo/equipo/${equipoId}`);
        setConsumos(data);
      } catch (err) {
        console.error('Error al cargar consumo:', err);
        setConsumos([]);
      } finally {
        setLoadingConsumo(false);
      }
    };
    cargar();
  }, [equipoId, equipos]);

  const filtrados = useMemo(() => {
    if (!busqueda.trim()) return equipos;
    const q = busqueda.trim().toLowerCase();
    return equipos.filter((e) => e.numero_linea.toLowerCase().includes(q));
  }, [busqueda, equipos]);

  const aniosDisponibles = useMemo(() => {
    const anios = [...new Set(consumos.map((c) => c.anio))];
    return anios.sort((a, b) => b - a);
  }, [consumos]);

  const consumosFiltrados = useMemo(() => {
    let filtered = [...consumos];
    if (filtroAnio) filtered = filtered.filter((c) => c.anio === Number(filtroAnio));
    if (filtroMes) filtered = filtered.filter((c) => c.mes === filtroMes);
    return filtered.sort((a, b) => a.anio - b.anio || a.mes - b.mes);
  }, [consumos, filtroAnio, filtroMes]);

  const chartData = useMemo(() => {
    return consumosFiltrados.map((c) => ({
      label: `${meses[c.mes - 1]} ${c.anio}`,
      voz: c.voz_consumido,
      datos: c.datos_consumido,
      sms: c.sms_consumido,
    }));
  }, [consumosFiltrados]);

  const iniciarEdicion = (c) => {
    setEditandoId(c.id);
    setEditForm({ voz_consumido: c.voz_consumido, datos_consumido: c.datos_consumido, sms_consumido: c.sms_consumido });
  };

  const cancelarEdicion = () => {
    setEditandoId(null);
  };

  const guardarEdicion = async (id) => {
    try {
      await api.put(`/consumo/${id}`, editForm);
      toast.success('Consumo actualizado');
      setConsumos((prev) =>
        prev.map((c) => (c.id === id ? { ...c, ...editForm } : c))
      );
      setEditandoId(null);
    } catch (err) {
      toast.error(err.response?.data?.error || 'Error al actualizar consumo');
    }
  };

  const handleEditChange = (field, value) => {
    setEditForm((prev) => ({ ...prev, [field]: value === '' ? 0 : Number(value) }));
  };

  if (loading) {
    return (
      <div style={{ animation: 'fadeInUp 0.35s ease' }}>
        <h1 style={{ margin: '0 0 1.5rem', fontSize: '1.2rem', fontWeight: 800 }}>Consumo Histórico</h1>
        <div className="card" style={{ height: 200 }}>
          <div className="empty-state" style={{ paddingTop: '2rem' }}>
            <p>Cargando equipos...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div style={{ animation: 'fadeInUp 0.35s ease' }}>
      <div className="page-header">
        <h1>Consumo Histórico</h1>
      </div>

      <div className="card" style={{ marginBottom: '1.25rem' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
          <div style={{ position: 'relative' }}>
            <label className="form-label">Buscar equipo</label>
            <input
              className="form-input"
              type="text"
              placeholder="Buscar por número de línea…"
              value={busqueda}
              onChange={(e) => { setBusqueda(e.target.value); setDropdownAbierto(true); }}
              onFocus={() => setDropdownAbierto(true)}
              onBlur={() => setTimeout(() => setDropdownAbierto(false), 200)}
              autoComplete="off"
            />
            {dropdownAbierto && (
              <div className="card" style={{
                position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 100,
                padding: 0, maxHeight: 220, overflowY: 'auto', marginTop: 4,
                border: '1px solid var(--border)', borderRadius: 6, boxShadow: '0 8px 24px rgba(0,0,0,0.12)',
              }}>
                {filtrados.length === 0 && !busqueda.trim() && (
                  <div style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                    Escriba para buscar equipos
                  </div>
                )}
                {filtrados.length === 0 && busqueda.trim() && (
                  <div style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                    No se encontraron equipos con &ldquo;{busqueda}&rdquo;
                  </div>
                )}
                {filtrados.map((eq) => (
                  <div
                    key={eq.id}
                    onMouseDown={() => { setEquipoId(eq.id); setBusqueda(''); setDropdownAbierto(false); }}
                    className={equipoId === eq.id ? 'selected' : ''}
                    style={{
                      padding: '0.5rem 0.75rem',
                      cursor: 'pointer',
                      borderBottom: '1px solid var(--border)',
                      background: equipoId === eq.id ? 'var(--primary-light)' : 'transparent',
                      color: equipoId === eq.id ? 'var(--primary)' : 'var(--text-primary)',
                      fontWeight: equipoId === eq.id ? 600 : 400,
                      transition: 'background 0.12s',
                      display: 'flex',
                      alignItems: 'center',
                      gap: 8,
                    }}
                    onMouseEnter={(e) => { if (equipoId !== eq.id) e.currentTarget.style.background = 'var(--bg-hover)'; }}
                    onMouseLeave={(e) => { if (equipoId !== eq.id) e.currentTarget.style.background = 'transparent'; }}
                  >
                    <span style={{ fontWeight: 600 }}>{eq.numero_linea}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
          {equipoId && !dropdownAbierto && (
            <div style={{ color: 'var(--text-secondary)' }}>
              Equipo seleccionado: <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                {equipos.find((e) => e.id === Number(equipoId))?.numero_linea}
              </span>
            </div>
          )}
        </div>
      </div>

      {loadingConsumo && (
        <div className="card" style={{ height: 200, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div className="empty-state">
            <p>Cargando consumo...</p>
          </div>
        </div>
      )}

      {!loadingConsumo && equipoId && selectedEquipo && consumos.length > 0 && (
        <>
          <div className="card" style={{ marginBottom: '1rem' }}>
            <div style={{ display: 'flex', gap: '0.6rem', flexWrap: 'wrap', alignItems: 'flex-end' }}>
              <div style={{ flex: 1, minWidth: 150 }}>
                <label className="form-label">Año</label>
                <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
                  <select className="form-select" value={filtroAnio} onChange={(e) => setFiltroAnio(e.target.value)} style={{ height: 36, flex: 1 }}>
                    <option value="">Todos</option>
                    {aniosDisponibles.map((a) => (
                      <option key={a} value={a}>{a}</option>
                    ))}
                  </select>
                  {filtroAnio && (
                    <button onClick={() => setFiltroAnio('')} className="btn btn-secondary btn-sm" title="Limpiar año" style={{ height: 36, width: 36, padding: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                      <FiX size={13} />
                    </button>
                  )}
                </div>
              </div>
              <div style={{ flex: 1, minWidth: 150 }}>
                <label className="form-label">Mes</label>
                <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
                  <select className="form-select" value={filtroMes} onChange={(e) => setFiltroMes(Number(e.target.value))} style={{ height: 36, flex: 1 }}>
                    <option value={0}>Todos</option>
                    {meses.map((m, i) => (
                      <option key={i + 1} value={i + 1}>{m}</option>
                    ))}
                  </select>
                  {filtroMes > 0 && (
                    <button onClick={() => setFiltroMes(0)} className="btn btn-secondary btn-sm" title="Limpiar mes" style={{ height: 36, width: 36, padding: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                      <FiX size={13} />
                    </button>
                  )}
                </div>
              </div>
              <div style={{ display: 'flex', alignItems: 'flex-end' }}>
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => { setFiltroAnio(''); setFiltroMes(0); }}
                  title="Limpiar filtros"
                  style={{ height: 36 }}
                >
                  <FiXCircle size={14} style={{ marginRight: 4 }} /> Limpiar
                </button>
              </div>
            </div>
          </div>

          <div className="table-container" style={{ marginBottom: '1.25rem' }}>
            <h2 className="chart-title" style={{ margin: 0 }}>
              Consumo — {selectedEquipo.numero_linea}
            </h2>
            <div className="table-responsive" style={{ marginTop: '0.5rem' }}>
              <table>
                <thead>
                  <tr>
                    <th>Año</th>
                    <th>Mes</th>
                    <th>Voz (min)</th>
                    <th>Datos (MB)</th>
                    <th>SMS</th>
                    <th style={{ width: 80 }}>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {consumosFiltrados.map((c, i) => (
                      <tr key={`${c.anio}-${c.mes}`} style={{ animation: `fadeInUp 0.2s ease ${i * 0.02}s both` }}>
                        <td style={{ fontWeight: 600 }}>{c.anio}</td>
                        <td>{meses[c.mes - 1]}</td>
                        {editandoId === c.id ? (
                          <>
                            <td><input className="form-input" type="number" step="0.1" min="0" style={{ width: 80, height: 32 }} value={editForm.voz_consumido} onChange={(e) => handleEditChange('voz_consumido', e.target.value)} /></td>
                            <td><input className="form-input" type="number" step="0.1" min="0" style={{ width: 80, height: 32 }} value={editForm.datos_consumido} onChange={(e) => handleEditChange('datos_consumido', e.target.value)} /></td>
                            <td><input className="form-input" type="number" min="0" style={{ width: 70, height: 32 }} value={editForm.sms_consumido} onChange={(e) => handleEditChange('sms_consumido', e.target.value)} /></td>
                            <td>
                              <div style={{ display: 'flex', gap: 4 }}>
                                <button onClick={() => guardarEdicion(c.id)} className="btn btn-primary btn-sm" title="Guardar"><FiCheck size={13} /></button>
                                <button onClick={cancelarEdicion} className="btn btn-secondary btn-sm" title="Cancelar"><FiX size={13} /></button>
                              </div>
                            </td>
                          </>
                        ) : (
                          <>
                            <td>{(c.voz_consumido || 0).toFixed(1)}</td>
                            <td>{(c.datos_consumido || 0).toFixed(1)}</td>
                            <td>{c.sms_consumido}</td>
                            <td>
                              <button onClick={() => iniciarEdicion(c)} className="btn btn-secondary btn-sm" title="Editar"><FiEdit2 size={13} /></button>
                            </td>
                          </>
                        )}
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="card">
            <h2 className="chart-title">Evolución del Consumo</h2>
            <div style={{ width: '100%', height: Math.max(250, Math.min(350, window.innerHeight * 0.35)) }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" strokeOpacity={0.5} />
                  <XAxis dataKey="label" tick={{ fill: 'var(--text-muted)', fontSize: 11 }} axisLine={{ stroke: 'var(--border)' }} tickLine={false} />
                  <YAxis tick={{ fill: 'var(--text-muted)', fontSize: 12 }} axisLine={false} tickLine={false} />
                  <Tooltip content={<CustomTooltip />} cursor={{ fill: 'var(--bg-hover)', opacity: 0.5 }} />
                  <Legend iconType="circle" iconSize={8} wrapperStyle={{ paddingTop: 8 }} />
                  <Bar dataKey="voz" name="Voz (min)" fill={chartColors.voz} radius={[4, 4, 0, 0]} maxBarSize={40} />
                  <Bar dataKey="datos" name="Datos (MB)" fill={chartColors.datos} radius={[4, 4, 0, 0]} maxBarSize={40} />
                  <Bar dataKey="sms" name="SMS" fill={chartColors.sms} radius={[4, 4, 0, 0]} maxBarSize={40} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </>
      )}

      {!loadingConsumo && equipoId && selectedEquipo && consumos.length === 0 && (
        <div className="card">
          <div className="empty-state">
            <p>No hay registros de consumo para {selectedEquipo.numero_linea}</p>
          </div>
        </div>
      )}

      {!loadingConsumo && equipoId && selectedEquipo && consumos.length > 0 && consumosFiltrados.length === 0 && (
        <div className="card">
          <div className="empty-state">
            <p>No hay registros que coincidan con los filtros seleccionados</p>
          </div>
        </div>
      )}

      {!equipoId && (
        <div className="card">
          <div className="empty-state">
            <p>Seleccione un equipo para ver su historial de consumo</p>
          </div>
        </div>
      )}
    </div>
  );
};

export default ConsumoHistorico;
