import { useState, useEffect, useCallback } from 'react';
import { toast } from 'sonner';
import api from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import ConfirmModal from '../components/ConfirmModal';
import { FiBell, FiCheckCircle, FiXCircle, FiChevronLeft, FiChevronRight, FiAlertTriangle, FiTrendingUp, FiTrash2 } from 'react-icons/fi';

const PAGE_SIZE = 20;

const Alertas = () => {
  const { usuario } = useAuth();
  const [alertas, setAlertas] = useState([]);
  const [total, setTotal] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [pagina, setPagina] = useState(1);
  const [loading, setLoading] = useState(false);
  const [filtroTipo, setFiltroTipo] = useState('');
  const [filtroLeida, setFiltroLeida] = useState('');
  const [noLeidas, setNoLeidas] = useState(0);
  const [archivarModalOpen, setArchivarModalOpen] = useState(false);

  const isAdmin = usuario?.rol === 'Administrador';

  const fetchAlertas = useCallback(async (p) => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      params.set('pagina', p || pagina);
      params.set('limite', PAGE_SIZE);
      if (filtroTipo) params.set('tipo', filtroTipo);
      if (filtroLeida !== '') params.set('leida', filtroLeida);
      params.set('solo_activas', 'true');

      const { data } = await api.get(`/alertas?${params}`);
      setAlertas(data.datos || []);
      setTotal(data.total || 0);
      setTotalPaginas(data.totalPaginas || 0);
    } catch (err) {
      console.error('Error al cargar alertas:', err);
      toast.error('Error al cargar alertas');
    } finally {
      setLoading(false);
    }
  }, [pagina, filtroTipo, filtroLeida]);

  const fetchNoLeidas = useCallback(async () => {
    try {
      const { data } = await api.get('/alertas/contar');
      setNoLeidas(data.total || 0);
    } catch { setNoLeidas(0); }
  }, []);

  useEffect(() => { fetchAlertas(pagina); }, [pagina, filtroTipo, filtroLeida]);
  useEffect(() => { fetchNoLeidas(); }, []);

  const handleMarcarLeida = async (id) => {
    try {
      await api.put(`/alertas/${id}/leer`);
      setAlertas((prev) => prev.map((a) => a.id === id ? { ...a, leida: true } : a));
      setNoLeidas((prev) => Math.max(0, prev - 1));
    } catch { toast.error('Error al marcar alerta'); }
  };

  const handleLeerTodas = async () => {
    try {
      const { data } = await api.put('/alertas/leer-todas');
      toast.success(data.mensaje || 'Alertas marcadas como leídas');
      setAlertas((prev) => prev.map((a) => ({ ...a, leida: true })));
      setNoLeidas(0);
    } catch { toast.error('Error al marcar todas'); }
  };

  const handleDescartar = async (id) => {
    try {
      await api.put(`/alertas/${id}/descartar`);
      setAlertas((prev) => prev.filter((a) => a.id !== id));
      setTotal((prev) => prev - 1);
      fetchNoLeidas();
    } catch { toast.error('Error al descartar alerta'); }
  };

  const handleArchivarTodas = async () => {
    try {
      const { data } = await api.put('/alertas/archivar-todas');
      toast.success(data.mensaje || 'Alertas archivadas');
      setAlertas([]);
      setTotal(0);
      setTotalPaginas(0);
      setPagina(1);
      setNoLeidas(0);
    } catch { toast.error('Error al archivar alertas'); }
  };

  const formatFecha = (f) => {
    if (!f) return '-';
    const d = new Date(f);
    return d.toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div style={{ animation: 'fadeInUp 0.35s ease' }}>
      <div className="page-header" style={{ marginBottom: '0.75rem' }}>
        <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <FiBell size={18} style={{ color: 'var(--primary)' }} /> Alertas de Sobregasto
          {noLeidas > 0 && (
            <span className="badge badge-danger" style={{ fontSize: '0.75rem', padding: '0.15rem 0.5rem' }}>
              {noLeidas} sin leer
            </span>
          )}
        </h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
          <select value={filtroTipo} onChange={(e) => { setFiltroTipo(e.target.value); setPagina(1); }} className="form-select" style={{ width: 140 }}>
            <option value="">Todos los tipos</option>
            <option value="SOBREGASTO">Sobregasto</option>
            <option value="EXCESO_UMBRAL">Exceso de umbral</option>
          </select>
          <select value={filtroLeida} onChange={(e) => { setFiltroLeida(e.target.value); setPagina(1); }} className="form-select" style={{ width: 130 }}>
            <option value="">Todos</option>
            <option value="false">No leídas</option>
            <option value="true">Leídas</option>
          </select>
          {noLeidas > 0 && (
            <button onClick={handleLeerTodas} className="btn btn-primary btn-sm" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <FiCheckCircle size={14} /> Leer todas
            </button>
          )}
          {alertas.length > 0 && (
            <button onClick={() => setArchivarModalOpen(true)} className="btn btn-secondary btn-sm" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <FiTrash2 size={14} /> Limpiar todo
            </button>
          )}
        </div>
      </div>

      <div className="table-container">
        <div className="table-responsive">
          <table>
            <thead>
              <tr>
                <th style={{ width: 40 }}></th>
                <th>UEB</th>
                <th>Período</th>
                <th>Tipo</th>
                <th>Mensaje</th>
                <th>%</th>
                <th>Fecha</th>
                <th style={{ width: 100 }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={8} style={{ textAlign: 'center', padding: '1.5rem', color: 'var(--text-muted)' }}>Cargando...</td></tr>
              ) : alertas.length === 0 ? (
                <tr><td colSpan={8} style={{ textAlign: 'center', padding: '1.5rem', color: 'var(--text-muted)' }}>
                  <FiBell size={24} style={{ opacity: 0.3, marginBottom: 6 }} /><br />
                  No hay alertas.
                </td></tr>
              ) : (
                alertas.map((a) => (
                  <tr key={a.id} style={{ opacity: a.descartada ? 0.5 : 1, background: a.leida ? '' : 'var(--warning-light)' }}>
                    <td>
                      {a.tipo === 'EXCESO_UMBRAL' ? (
                        <FiAlertTriangle size={16} style={{ color: 'var(--danger)' }} />
                      ) : (
                        <FiTrendingUp size={16} style={{ color: 'var(--warning)' }} />
                      )}
                    </td>
                    <td style={{ fontWeight: 600 }}>{a.ueb_nombre || a.ueb_codigo || '-'}</td>
                    <td>{a.mes && a.anio ? `${a.mes}/${a.anio}` : '-'}</td>
                    <td>
                      <span className={`badge ${a.tipo === 'EXCESO_UMBRAL' ? 'badge-danger' : 'badge-warning'}`}>
                        {a.tipo === 'EXCESO_UMBRAL' ? 'Exceso de umbral' : 'Sobregasto'}
                      </span>
                    </td>
                    <td style={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{a.mensaje}</td>
                    <td>{a.porcentaje != null ? `${a.porcentaje}%` : '-'}</td>
                    <td style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{formatFecha(a.creada_en)}</td>
                    <td>
                      <div style={{ display: 'flex', gap: 4 }}>
                        {!a.leida && (
                          <button onClick={() => handleMarcarLeida(a.id)} className="btn btn-sm" style={{ padding: '0.2rem 0.4rem', fontSize: 11 }} title="Marcar como leída">
                            <FiCheckCircle size={13} />
                          </button>
                        )}
                        {isAdmin && (
                          <button onClick={() => handleDescartar(a.id)} className="btn btn-sm" style={{ padding: '0.2rem 0.4rem', fontSize: 11 }} title="Descartar alerta">
                            <FiXCircle size={13} />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {totalPaginas > 1 && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem', borderTop: '1px solid var(--border)' }}>
            <span style={{ fontSize: 13, color: 'var(--text-muted)' }}>Total: {total} alerta(s)</span>
            <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
              <button disabled={pagina <= 1} onClick={() => setPagina(p => p - 1)} className="btn btn-sm" style={{ padding: '0.3rem 0.5rem' }}>
                <FiChevronLeft size={14} />
              </button>
              <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{pagina} / {totalPaginas}</span>
              <button disabled={pagina >= totalPaginas} onClick={() => setPagina(p => p + 1)} className="btn btn-sm" style={{ padding: '0.3rem 0.5rem' }}>
                <FiChevronRight size={14} />
              </button>
            </div>
          </div>
        )}
      </div>

      <ConfirmModal
        isOpen={archivarModalOpen}
        onClose={() => setArchivarModalOpen(false)}
        onConfirm={() => { setArchivarModalOpen(false); handleArchivarTodas(); }}
        title="Limpiar alertas"
        message="¿Estás seguro de que deseas archivar todas las alertas? Se eliminarán de la vista activa pero se conservarán en el historial."
        confirmText="Sí, archivar todo"
        variant="warning"
      />
    </div>
  );
};

export default Alertas;
