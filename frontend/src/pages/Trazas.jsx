import { useState, useEffect } from 'react';
import { FiRefreshCw, FiSearch, FiDownload, FiChevronLeft, FiChevronRight } from 'react-icons/fi';
import api from '../services/api';

const acciones = ['LOGIN', 'LOGOUT', 'CREATE', 'UPDATE', 'DELETE', 'ACTIVATE', 'DEACTIVATE'];
const entidades = ['Usuario', 'UEB', 'Empleado', 'Equipo', 'PlanAnual', 'Configuracion'];
const POR_PAGINA = 50;

const Trazas = () => {
  const [trazas, setTrazas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pagina, setPagina] = useState(1);
  const [total, setTotal] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [filtros, setFiltros] = useState({ accion: '', entidad: '', desde: '', hasta: '' });

  const cargarTrazas = async (filtrosActuales, paginaActual = 1) => {
    setLoading(true);
    try {
      const params = { pagina: paginaActual, limite: POR_PAGINA };
      if (filtrosActuales.accion) params.accion = filtrosActuales.accion;
      if (filtrosActuales.entidad) params.entidad = filtrosActuales.entidad;
      if (filtrosActuales.desde) params.desde = filtrosActuales.desde;
      if (filtrosActuales.hasta) params.hasta = filtrosActuales.hasta;
      const { data } = await api.get('/trazas', { params });
      setTrazas(data.datos);
      setTotal(data.total);
      setPagina(data.pagina);
      setTotalPaginas(data.totalPaginas);
    } catch (err) {
      console.error('Error al cargar trazas:', err);
      setTrazas([]);
      setTotal(0);
      setTotalPaginas(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    cargarTrazas(filtros, pagina);
  }, []);

  const cambiarPagina = (nuevaPagina) => {
    if (nuevaPagina < 1 || nuevaPagina > totalPaginas) return;
    setPagina(nuevaPagina);
    cargarTrazas(filtros, nuevaPagina);
  };

  const handleFilter = () => {
    setPagina(1);
    cargarTrazas(filtros, 1);
  };

  const limpiarFiltros = () => {
    const vacio = { accion: '', entidad: '', desde: '', hasta: '' };
    setFiltros(vacio);
    setPagina(1);
    cargarTrazas(vacio, 1);
  };

  const exportarExcel = async () => {
    try {
      const params = {};
      if (filtros.accion) params.accion = filtros.accion;
      if (filtros.entidad) params.entidad = filtros.entidad;
      if (filtros.desde) params.desde = filtros.desde;
      if (filtros.hasta) params.hasta = filtros.hasta;
      const response = await api.get('/trazas/exportar/excel', { params, responseType: 'blob' });
      const blobUrl = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = blobUrl;
      const ahora = new Date();
      const ts = `${ahora.getFullYear()}${String(ahora.getMonth() + 1).padStart(2, '0')}${String(ahora.getDate()).padStart(2, '0')}_${String(ahora.getHours()).padStart(2, '0')}${String(ahora.getMinutes()).padStart(2, '0')}${String(ahora.getSeconds()).padStart(2, '0')}`;
      link.setAttribute('download', `trazas_auditoria_${ts}.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(blobUrl);
    } catch (err) {
      console.error('Error al exportar trazas:', err);
    }
  };

  const formatFecha = (fecha) => {
    if (!fecha) return '-';
    return new Date(fecha).toLocaleString('es-CU', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit', second: '2-digit',
    });
  };

  const badgeStyle = (accion) => {
    const base = { padding: '0.2rem 0.5rem', borderRadius: 6, fontSize: '0.7rem', fontWeight: 600, textTransform: 'uppercase' };
    if (['CREATE', 'LOGIN'].includes(accion)) return { ...base, background: 'var(--success-light)', color: 'var(--success)' };
    if (['UPDATE', 'ACTIVATE'].includes(accion)) return { ...base, background: 'var(--primary-light)', color: 'var(--primary)' };
    if (['DELETE', 'DEACTIVATE'].includes(accion)) return { ...base, background: 'var(--danger-light)', color: 'var(--danger)' };
    if (accion === 'LOGOUT') return { ...base, background: 'var(--warning-light)', color: 'var(--warning)' };
    return { ...base, background: 'var(--bg-hover)', color: 'var(--text-muted)' };
  };

  const cardStyle = {
    background: 'var(--bg-card)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius)',
    padding: '0.75rem',
  };

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto' }}>
      <div style={{ marginBottom: '1rem' }}>
        <h1 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 800, color: 'var(--text-primary)' }}>
          Trazas de Auditoría
        </h1>
      </div>

      <div style={{ ...cardStyle, marginBottom: '1rem' }}>
        <div style={{ display: 'flex', gap: '0.6rem', flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <div style={{ minWidth: 130, flex: '1 1 130px' }}>
            <label className="compact-label">Acción</label>
            <select value={filtros.accion} onChange={(e) => setFiltros({ ...filtros, accion: e.target.value })}
              className="form-select compact-select">
              <option value="">Todas</option>
              {acciones.map(a => <option key={a} value={a}>{a}</option>)}
            </select>
          </div>
          <div style={{ minWidth: 130, flex: '1 1 130px' }}>
            <label className="compact-label">Entidad</label>
            <select value={filtros.entidad} onChange={(e) => setFiltros({ ...filtros, entidad: e.target.value })}
              className="form-select compact-select">
              <option value="">Todas</option>
              {entidades.map(e => <option key={e} value={e}>{e}</option>)}
            </select>
          </div>
          <div style={{ minWidth: 130, flex: '1 1 130px' }}>
            <label className="compact-label">Desde</label>
            <input type="date" value={filtros.desde} onChange={(e) => setFiltros({ ...filtros, desde: e.target.value })}
              className="form-input compact-input" />
          </div>
          <div style={{ minWidth: 130, flex: '1 1 130px' }}>
            <label className="compact-label">Hasta</label>
            <input type="date" value={filtros.hasta} onChange={(e) => setFiltros({ ...filtros, hasta: e.target.value })}
              className="form-input compact-input" />
          </div>
          <button onClick={handleFilter} className="btn btn-primary btn-sm" style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 4, minWidth: 110 }}>
            <FiSearch size={12} /> Filtrar
          </button>
          <button onClick={exportarExcel} className="btn btn-primary btn-sm" style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 4, minWidth: 110 }}>
            <FiDownload size={14} /> Exportar
          </button>
          <button onClick={limpiarFiltros} className="btn btn-secondary btn-sm" style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 4, minWidth: 110 }}>
            <FiRefreshCw size={12} /> Limpiar
          </button>
        </div>
      </div>

      {total > 0 && !loading && (
        <div style={{ ...cardStyle, marginBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.5rem 0.75rem' }}>
          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            Total: <strong>{total}</strong> registros — Página {pagina} de {totalPaginas}
          </span>
          <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
            <button
              onClick={() => cambiarPagina(pagina - 1)}
              disabled={pagina <= 1}
              className="btn btn-secondary btn-sm"
              style={{ display: 'flex', alignItems: 'center', gap: 4, opacity: pagina <= 1 ? 0.4 : 1 }}
            >
              <FiChevronLeft size={14} /> Anterior
            </button>
            <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-primary)', minWidth: 40, textAlign: 'center' }}>
              {pagina}
            </span>
            <button
              onClick={() => cambiarPagina(pagina + 1)}
              disabled={pagina >= totalPaginas}
              className="btn btn-secondary btn-sm"
              style={{ display: 'flex', alignItems: 'center', gap: 4, opacity: pagina >= totalPaginas ? 0.4 : 1 }}
            >
              Siguiente <FiChevronRight size={14} />
            </button>
          </div>
        </div>
      )}

      <div style={cardStyle}>
        {loading ? (
          <p style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>Cargando...</p>
        ) : trazas.length === 0 ? (
          <p style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>No hay trazas registradas</p>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border)' }}>
                  <th>Fecha</th>
                  <th>Usuario</th>
                  <th>Rol</th>
                  <th>Acción</th>
                  <th>Entidad</th>
                  <th>Descripción</th>
                  <th>IP</th>
                </tr>
              </thead>
              <tbody>
                {trazas.map((t) => (
                  <tr key={t.id} style={{ borderBottom: '1px solid var(--border-light)' }}>
                    <td style={{ whiteSpace: 'nowrap' }}>{formatFecha(t.fecha)}</td>
                    <td style={{ fontWeight: 500 }}>{t.usuario_login || '—'}</td>
                    <td>{t.usuario_rol || '—'}</td>
                    <td><span style={badgeStyle(t.accion)}>{t.accion}</span></td>
                    <td>{t.entidad}{t.entidad_id ? ` #${t.entidad_id}` : ''}</td>
                    <td style={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis' }}>{t.descripcion}</td>
                    <td style={{ fontFamily: 'monospace', fontSize: '0.7rem' }}>{t.ip || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default Trazas;
