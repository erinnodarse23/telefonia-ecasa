import { useState, useEffect } from 'react';
import { toast } from 'sonner';
import { FiDownload, FiSearch, FiUserCheck, FiMonitor, FiBarChart2 } from 'react-icons/fi';
import api from '../services/api';
import { formatDate } from '../utils/formatDate';

const estados = ['todos', 'nuevo', 'bueno', 'dañado', 'de baja'];

const tabs = [
  { key: 'responsables', label: 'Responsables por UEB', icon: FiUserCheck },
  { key: 'equipos', label: 'Equipos por Estado', icon: FiMonitor },
];

const TabButton = ({ tab, active, onClick }) => {
  const Icon = tab.icon;
  return (
    <button
      onClick={onClick}
      className={`tab-btn${active ? ' tab-btn-active' : ''}`}
      style={{
        display: 'flex', alignItems: 'center', gap: 7,
        padding: '0.5rem 0.75rem',
        border: 'none',
        cursor: 'pointer',
        fontWeight: 600,
        borderRadius: '6px 6px 0 0',
        background: active ? 'var(--card-bg)' : 'transparent',
        color: active ? 'var(--primary)' : 'var(--text-secondary)',
        borderBottom: active ? '2px solid var(--primary)' : '2px solid transparent',
        transition: 'all 0.2s',
      }}
    >
      <Icon size={16} />
      {tab.label}
    </button>
  );
};

const DownloadBtn = ({ onClick, label, loading, icon: Icon, disabled }) => (
  <button
    onClick={onClick}
    disabled={disabled || loading}
    className="btn btn-primary"
    style={{ whiteSpace: 'nowrap', display: 'flex', alignItems: 'center', gap: 6, transition: 'transform 0.15s ease, opacity 0.15s ease', opacity: disabled ? 0.5 : 1 }}
    onMouseEnter={(e) => { if (!disabled) e.currentTarget.style.transform = 'scale(1.02)'; }}
    onMouseLeave={(e) => { e.currentTarget.style.transform = 'scale(1)'; }}
  >
    {loading ? (
      <span className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} />
    ) : (
      <Icon size={15} />
    )}
    {loading ? 'Descargando...' : label}
  </button>
);

const blobDownload = async (url, filename) => {
  try {
    const response = await api.get(url, { responseType: 'blob' });
    const blobUrl = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = blobUrl;
    link.setAttribute('download', filename);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(blobUrl);
    toast.success(`${filename} descargado correctamente`);
    return true;
  } catch (error) {
    console.error('Error al descargar:', error);
    toast.error('Error al descargar archivo');
    return false;
  }
};

const Reportes = () => {
  const [pestana, setPestana] = useState('responsables');

  const [uebs, setUebs] = useState([]);
  const [uebId, setUebId] = useState('');
  const [responsables, setResponsables] = useState([]);
  const [loadResp, setLoadResp] = useState(false);
  const [genResp, setGenResp] = useState(false);
  const [exportRespLoading, setExportRespLoading] = useState(false);
  const [exportRespExcelLoading, setExportRespExcelLoading] = useState(false);

  const [filtroEstado, setFiltroEstado] = useState('todos');
  const [equipos, setEquipos] = useState([]);
  const [loadEq, setLoadEq] = useState(false);
  const [genEq, setGenEq] = useState(false);
  const [exportEqLoading, setExportEqLoading] = useState(false);
  const [exportEqPdfLoading, setExportEqPdfLoading] = useState(false);

  useEffect(() => {
    api.get('/ueb').then(({ data }) => setUebs(Array.isArray(data) ? data : [])).catch(err => console.error('Error al cargar UEBs:', err));
  }, []);

  const generarResponsables = async () => {
    setLoadResp(true);
    setGenResp(false);
    try {
      const endpoint = uebId ? `/reportes/responsables?uebId=${uebId}` : '/reportes/responsables';
      const { data } = await api.get(endpoint);
      setResponsables(Array.isArray(data) ? data : []);
      setGenResp(true);
    } catch (err) {
      console.error('Error al cargar responsables:', err);
      setResponsables([]);
    } finally {
      setLoadResp(false);
    }
  };

  const generarEquipos = async () => {
    setLoadEq(true);
    setGenEq(false);
    try {
      const estado = filtroEstado && filtroEstado !== 'todos' ? `?estado=${filtroEstado}` : '';
      const { data } = await api.get(`/reportes/equipos/estado${estado}`);
      setEquipos(Array.isArray(data) ? data : []);
      setGenEq(true);
    } catch (err) {
      console.error('Error al cargar equipos:', err);
      setEquipos([]);
    } finally {
      setLoadEq(false);
    }
  };

  const exportarPDF = async () => {
    setExportRespLoading(true);
    const params = uebId ? `?uebId=${uebId}` : '';
    await blobDownload(`/exportar/responsables/pdf${params}`, `responsables_ueb${uebId || 'todas'}.pdf`);
    setExportRespLoading(false);
  };

  const exportarResponsablesExcel = async () => {
    setExportRespExcelLoading(true);
    const params = uebId ? `?uebId=${uebId}` : '';
    await blobDownload(`/exportar/responsables/excel${params}`, `responsables_ueb${uebId || 'todas'}.xlsx`);
    setExportRespExcelLoading(false);
  };

  const exportarEquiposExcel = async () => {
    setExportEqLoading(true);
    const estado = filtroEstado && filtroEstado !== 'todos' ? `?estado=${filtroEstado}` : '';
    await blobDownload(`/exportar/equipos/excel${estado}`, `equipos_${filtroEstado || 'todos'}.xlsx`);
    setExportEqLoading(false);
  };

  const exportarEquiposPDF = async () => {
    setExportEqPdfLoading(true);
    const estado = filtroEstado && filtroEstado !== 'todos' ? `?estado=${filtroEstado}` : '';
    await blobDownload(`/exportar/equipos/pdf${estado}`, `equipos_${filtroEstado || 'todos'}.pdf`);
    setExportEqPdfLoading(false);
  };

  return (
    <div style={{ animation: 'fadeInUp 0.35s ease' }}>
      <div className="page-header" style={{ marginBottom: '0.625rem' }}>
        <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <FiBarChart2 size={18} style={{ color: 'var(--primary)' }} /> Reportes
        </h1>
      </div>

      <div style={{ display: 'flex', gap: 0, marginBottom: 0, borderBottom: '1px solid var(--border)' }}>
        {tabs.map(t => (
          <TabButton key={t.key} tab={t} active={pestana === t.key} onClick={() => setPestana(t.key)} />
        ))}
      </div>

      {pestana === 'responsables' && (
        <div className="table-container" style={{ borderTopLeftRadius: 0 }}>
          <div style={{ display: 'flex', gap: '0.6rem', alignItems: 'flex-end', flexWrap: 'wrap', padding: '0.75rem', borderBottom: '1px solid var(--border)' }}>
            <div>
              <label className="form-label">UEB</label>
              <select value={uebId} onChange={(e) => setUebId(e.target.value)} className="form-select">
                <option value="">Todas las UEB</option>
                {uebs.filter(u => u.activo).map(u => <option key={u.id} value={u.id}>{u.nombre}</option>)}
              </select>
            </div>
            <button onClick={generarResponsables} disabled={loadResp} className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              {loadResp ? <span className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} /> : <FiSearch size={15} />}
              {loadResp ? 'Cargando...' : 'Generar'}
            </button>
            <DownloadBtn onClick={exportarResponsablesExcel} label="Exportar Excel" loading={exportRespExcelLoading} icon={FiDownload} disabled={!genResp} />
            <DownloadBtn onClick={exportarPDF} label="Exportar PDF" loading={exportRespLoading} icon={FiDownload} disabled={!genResp} />
          </div>

          {genResp && (
            <div className="table-responsive" style={{ marginTop: 0 }}>
              <table>
                <thead>
                  <tr>
                    <th>Empleado</th>
                    <th>Cargo</th>
                    <th>Teléfono</th>
                    <th>Línea</th>
                    <th>Marca</th>
                    <th>Modelo</th>
                    <th>Fecha Asignación</th>
                  </tr>
                </thead>
                <tbody>
                  {responsables.map((r, i) => (
                    <tr key={i} style={{ animation: `fadeInUp 0.3s ease ${i * 0.03}s both` }}>
                      <td style={{ fontWeight: 600 }}>{r.empleado_nombre}</td>
                      <td>{r.cargo || '-'}</td>
                      <td>{r.empleado_telefono || '-'}</td>
                      <td>{r.numero_linea}</td>
                      <td>{r.marca || '-'}</td>
                      <td>{r.modelo || '-'}</td>
                      <td>{r.fecha_inicio ? formatDate(r.fecha_inicio) : '-'}</td>
                    </tr>
                  ))}
                  {responsables.length === 0 && (
                    <tr><td colSpan={7} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '0.75rem' }}>Sin resultados</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {pestana === 'equipos' && (
        <div className="table-container" style={{ borderTopLeftRadius: 0 }}>
          <div style={{ display: 'flex', gap: '0.6rem', alignItems: 'flex-end', flexWrap: 'wrap', padding: '0.75rem', borderBottom: '1px solid var(--border)' }}>
            <div>
              <label className="form-label">Estado</label>
              <select value={filtroEstado} onChange={(e) => setFiltroEstado(e.target.value)} className="form-select">
                {estados.map(e => <option key={e} value={e}>{e.charAt(0).toUpperCase() + e.slice(1)}</option>)}
              </select>
            </div>
            <button onClick={generarEquipos} disabled={loadEq} className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              {loadEq ? <span className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} /> : <FiSearch size={15} />}
              {loadEq ? 'Cargando...' : 'Generar'}
            </button>
            <DownloadBtn onClick={exportarEquiposExcel} label="Exportar Excel" loading={exportEqLoading} icon={FiDownload} disabled={!genEq} />
            <DownloadBtn onClick={exportarEquiposPDF} label="Exportar PDF" loading={exportEqPdfLoading} icon={FiDownload} disabled={!genEq} />
          </div>

          {genEq && (
            <div className="table-responsive">
              <table>
                <thead>
                  <tr>
                    <th>Número</th>
                    <th>Marca</th>
                    <th>Modelo</th>
                    <th>Estado</th>
                    <th>Observaciones</th>
                  </tr>
                </thead>
                <tbody>
                  {equipos.map((eq, i) => (
                    <tr key={i} style={{ animation: `fadeInUp 0.3s ease ${i * 0.03}s both` }}>
                      <td style={{ fontWeight: 600 }}>{eq.numero_linea}</td>
                      <td>{eq.marca || '-'}</td>
                      <td>{eq.modelo || '-'}</td>
                      <td>
                        <span className={`badge ${
                          eq.estado_fisico === 'nuevo' || eq.estado_fisico === 'bueno' ? 'badge-success' :
                          eq.estado_fisico === 'dañado' ? 'badge-warning' :
                          eq.estado_fisico === 'de baja' ? 'badge-danger' : 'badge-outline'
                        }`}>{eq.estado_fisico || '-'}</span>
                      </td>
                      <td>{eq.observaciones || '-'}</td>
                    </tr>
                  ))}
                  {equipos.length === 0 && (
                    <tr><td colSpan={5} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '0.75rem' }}>Sin resultados</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default Reportes;
