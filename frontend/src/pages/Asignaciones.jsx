import { useState, useEffect, useMemo } from 'react';
import { toast } from 'sonner';
import { FiPlus, FiTrash2, FiUpload, FiCheckSquare } from 'react-icons/fi';
import { useAuth } from '../contexts/AuthContext';
import api from '../services/api';
import ConfirmModal from '../components/ConfirmModal';
import ImportExcelModal from '../components/ImportExcelModal';
import SkeletonLoader from '../components/SkeletonLoader';
import EmptyState from '../components/EmptyState';
import { formatDate, hoyLocal } from '../utils/formatDate';

const Asignaciones = () => {
  const { usuario } = useAuth();
  const [asignaciones, setAsignaciones] = useState([]);
  const [empleados, setEmpleados] = useState([]);
  const [equipos, setEquipos] = useState([]);
  const [uebs, setUebs] = useState([]);
  const [loading, setLoading] = useState(true);

  const [filtroEstado, setFiltroEstado] = useState('activas');
  const [filtroEquipo, setFiltroEquipo] = useState('');
  const [filtroEmpleado, setFiltroEmpleado] = useState('');

  const [modalAbierto, setModalAbierto] = useState(false);
  const [modalFinalizar, setModalFinalizar] = useState(false);
  const [asignacionSeleccionada, setAsignacionSeleccionada] = useState(null);
  const [form, setForm] = useState({ equipo_id: '', empleado_id: '', fecha_inicio: hoyLocal(), estado_equipo_entrega: 'bueno' });
  const [formFinalizar, setFormFinalizar] = useState({ fecha_fin: hoyLocal(), estado_equipo_devolucion: 'bueno' });
  const [errores, setErrores] = useState({});
  const [confirmFinalizar, setConfirmFinalizar] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState({ isOpen: false, id: null });
  const [importModalOpen, setImportModalOpen] = useState(false);

  const cargarAsignaciones = async () => {
    try {
      let url = `/asignaciones?estado=${filtroEstado}`;
      if (filtroEquipo) {
        url = `/asignaciones/equipo/${filtroEquipo}?estado=${filtroEstado}`;
      } else if (filtroEmpleado) {
        url = `/asignaciones/empleado/${filtroEmpleado}?estado=${filtroEstado}`;
      }
      const { data } = await api.get(url);
      setAsignaciones(Array.isArray(data) ? data : [data].filter(Boolean));
    } catch (err) {
      console.error('Error al cargar asignaciones:', err);
      toast.error('Error al cargar asignaciones');
      setAsignaciones([]);
    } finally {
      setLoading(false);
    }
  };

  const cargarSelectores = async () => {
    try {
      const [empRes, eqRes, uebRes] = await Promise.all([
        api.get('/empleados'),
        api.get('/equipos'),
        api.get('/ueb')
      ]);
      setEmpleados(empRes.data);
      setEquipos(eqRes.data);
      setUebs(uebRes.data);
    } catch (err) {
      console.error('Error al cargar datos de selección:', err);
      toast.error('Error al cargar datos de selección');
    }
  };

  useEffect(() => {
    setLoading(true);
    cargarAsignaciones();
  }, [filtroEstado, filtroEquipo, filtroEmpleado]);

  const idsAsignados = useMemo(
    () => new Set(asignaciones.filter(a => !a.fecha_fin).map(a => a.equipo_id)),
    [asignaciones]
  );

  const idsEmpleadosAsignados = useMemo(
    () => new Set(asignaciones.filter(a => !a.fecha_fin).map(a => a.empleado_id)),
    [asignaciones]
  );

  useEffect(() => {
    cargarSelectores();
  }, []);

  const equipoLabel = (id) => {
    const eq = equipos.find(e => e.id === id);
    return eq ? `${eq.numero_linea} - ${eq.marca} ${eq.modelo}` : `#${id}`;
  };

  const empleadoLabel = (id) => {
    const emp = empleados.find(e => e.id === id);
    return emp ? emp.nombre : `#${id}`;
  };

  const uebDeEmpleado = (empleadoId) => {
    const emp = empleados.find(e => e.id === empleadoId);
    if (!emp) return '';
    const ueb = uebs.find(u => u.id === emp.ueb_id);
    return ueb ? ueb.nombre : '';
  };

  const validar = () => {
    const errs = {};
    if (!form.equipo_id) {
      errs.equipo_id = 'Debe seleccionar un equipo';
    }
    if (!form.empleado_id) {
      errs.empleado_id = 'Debe seleccionar un empleado';
    }
    if (form.fecha_inicio) {
      const hoy = new Date();
      hoy.setHours(23, 59, 59, 999);
      if (new Date(form.fecha_inicio) > hoy) {
        errs.fecha_inicio = 'La fecha de inicio no puede ser futura';
      }
    }
    setErrores(errs);
    return Object.keys(errs).length === 0;
  };

  const abrirModalCrear = () => {
    setForm({ equipo_id: '', empleado_id: '', fecha_inicio: hoyLocal(), estado_equipo_entrega: 'bueno' });
    setErrores({});
    setModalAbierto(true);
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setErrores({ ...errores, [e.target.name]: '' });
  };

  const crear = async (e) => {
    e.preventDefault();
    if (!validar()) return;
    const payload = {
      ...form,
      equipo_id: Number(form.equipo_id),
      empleado_id: Number(form.empleado_id),
    };
    console.log('[Asignaciones] Payload enviado:', payload);
    try {
      await api.post('/asignaciones', payload);
      setModalAbierto(false);
      toast.success('Asignación creada exitosamente');
      cargarAsignaciones();
    } catch (err) {
      const msg = err.response?.data?.error || 'Error al crear asignación';
      setErrores({ general: msg });
      toast.error(msg);
    }
  };

  const abrirFinalizar = (asignacion) => {
    setAsignacionSeleccionada(asignacion);
    setFormFinalizar({ fecha_fin: hoyLocal(), estado_equipo_devolucion: 'bueno' });
    setErrores({});
    setModalFinalizar(true);
  };

  const solicitarConfirmacionFinalizar = (e) => {
    e.preventDefault();
    setConfirmFinalizar(true);
  };

  const ejecutarFinalizar = async () => {
    setConfirmFinalizar(false);
    try {
      await api.put(`/asignaciones/${asignacionSeleccionada.id}/finalizar`, formFinalizar);
      setModalFinalizar(false);
      setAsignacionSeleccionada(null);
      toast.success('Asignación finalizada exitosamente');
      cargarAsignaciones();
    } catch (err) {
      console.error('Error al finalizar asignación:', err);
      setErrores({ general: err.response?.data?.error || 'Error al finalizar asignación' });
      toast.error(err.response?.data?.error || 'Error al finalizar asignación');
    }
  };

  const confirmarEliminar = (id) => {
    setConfirmDelete({ isOpen: true, id });
  };

  const ejecutarEliminar = async () => {
    const id = confirmDelete.id;
    setConfirmDelete({ isOpen: false, id: null });
    try {
      await api.delete(`/asignaciones/${id}`);
      toast.success('Asignación eliminada permanentemente');
      cargarAsignaciones();
    } catch (err) {
      console.error('Error al eliminar asignación:', err);
      toast.error(err.response?.data?.error || 'Error al eliminar asignación');
    }
  };

  if (loading) return <SkeletonLoader rows={6} columns={9} />;

  return (
    <div style={{ animation: 'fadeInUp 0.35s ease' }}>
      <div className="page-header">
        <h1>Asignaciones</h1>
        <div style={{ display: 'flex', gap: '0.6rem' }}>
          <button onClick={() => setImportModalOpen(true)} className="btn btn-primary btn-sm" title="Importar Excel"><FiUpload size={14} /> Importar</button>
          <button onClick={abrirModalCrear} className="btn btn-primary"><FiPlus size={16} /> Nueva Asignación</button>
        </div>
      </div>

      <div className="filter-bar">
        <select value={filtroEstado} onChange={(e) => { setFiltroEstado(e.target.value); setLoading(true); }} className="form-select">
          <option value="activas">Activas</option>
          <option value="inactivas">Inactivas</option>
          <option value="todas">Todas</option>
        </select>
        <select value={filtroEquipo} onChange={(e) => { setFiltroEquipo(e.target.value); setFiltroEmpleado(''); }} className="form-select">
          <option value="">Todos los equipos</option>
          {equipos.map(eq => (
            <option key={eq.id} value={eq.id}>{eq.numero_linea} - {eq.marca} {eq.modelo}</option>
          ))}
        </select>
        <select value={filtroEmpleado} onChange={(e) => { setFiltroEmpleado(e.target.value); setFiltroEquipo(''); }} className="form-select">
          <option value="">Todos los empleados</option>
          {empleados.map(emp => (
            <option key={emp.id} value={emp.id}>{emp.nombre}</option>
          ))}
        </select>
      </div>

      <div className="table-container">
        <div className="table-responsive">
          <table>
            <thead>
              <tr>
                <th>Equipo</th>
                <th>Empleado</th>
                <th>UEB</th>
                <th>Inicio</th>
                <th>Estado Entrega</th>
                <th>Fin</th>
                <th>Estado</th>
                <th>Estado Devolución</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {asignaciones.map((a, i) => (
                <tr key={a.id} style={{ animation: `fadeInUp 0.25s ease ${i * 0.025}s both` }}>
                  <td style={{ fontWeight: 600 }}>{equipoLabel(a.equipo_id)}</td>
                  <td>{empleadoLabel(a.empleado_id)}</td>
                  <td>{uebDeEmpleado(a.empleado_id) || '-'}</td>
                  <td>{a.fecha_inicio ? formatDate(a.fecha_inicio) : '-'}</td>
                  <td>{a.estado_equipo_entrega || '-'}</td>
                  <td>{a.fecha_fin ? formatDate(a.fecha_fin) : '-'}</td>
                  <td>
                    <span className={`badge ${!a.fecha_fin ? 'badge-success' : 'badge-outline'}`}>
                      {a.fecha_fin ? 'Finalizada' : 'Activa'}
                    </span>
                  </td>
                  <td>{a.estado_equipo_devolucion || '-'}</td>
                  <td>
                    <div style={{ display: 'flex', gap: 4 }}>
                      {!a.fecha_fin && (
                        <button onClick={() => abrirFinalizar(a)} className="btn btn-warning btn-sm" title="Finalizar"><FiCheckSquare size={13} /></button>
                      )}
                      {usuario?.rol === 'Administrador' && (
                        <button onClick={() => confirmarEliminar(a.id)} className="btn btn-danger btn-sm" title="Eliminar"><FiTrash2 size={13} /></button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
              {asignaciones.length === 0 && <EmptyState message="No hay asignaciones registradas" />}
            </tbody>
          </table>
        </div>
      </div>

      {modalAbierto && (
        <div className="modal-overlay" onClick={() => setModalAbierto(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>Nueva Asignación</h2>
            {errores.general && <p style={{ color: 'var(--danger)', margin: '0 0 0.75rem' }}>{errores.general}</p>}
            <form onSubmit={crear}>
              <label className="form-label">Equipo (solo disponibles)</label>
              <select name="equipo_id" value={form.equipo_id} onChange={handleChange} className={`form-select${errores.equipo_id ? ' input-error' : ''}`} required>
                <option value="">Seleccione un equipo</option>
                {equipos.filter(e => e.activo !== false && !idsAsignados.has(e.id)).map(e => (
                  <option key={e.id} value={e.id}>{e.numero_linea} - {e.marca} {e.modelo}</option>
                ))}
              </select>
              {errores.equipo_id && <span className="field-error">{errores.equipo_id}</span>}
              <label className="form-label">Empleado (solo sin línea activa)</label>
              <select name="empleado_id" value={form.empleado_id} onChange={handleChange} className={`form-select${errores.empleado_id ? ' input-error' : ''}`} required>
                <option value="">Seleccione un empleado</option>
                {empleados.filter(e => e.activo !== false && !idsEmpleadosAsignados.has(e.id)).map(e => (
                  <option key={e.id} value={e.id}>{e.nombre}</option>
                ))}
              </select>
              {errores.empleado_id && <span className="field-error">{errores.empleado_id}</span>}
              <label className="form-label">Fecha de Inicio</label>
              <input name="fecha_inicio" type="date" value={form.fecha_inicio} onChange={handleChange} className={`form-input${errores.fecha_inicio ? ' input-error' : ''}`} required />
              {errores.fecha_inicio && <span className="field-error">{errores.fecha_inicio}</span>}
              <label className="form-label">Estado del Equipo (Entrega)</label>
              <select name="estado_equipo_entrega" value={form.estado_equipo_entrega} onChange={handleChange} className="form-select">
                <option value="nuevo">Nuevo</option>
                <option value="bueno">Bueno</option>
                <option value="dañado">Dañado</option>
              </select>
              <div style={{ display: 'flex', gap: '0.6rem', justifyContent: 'flex-end', marginTop: '1.25rem' }}>
                <button type="button" onClick={() => setModalAbierto(false)} className="btn btn-secondary">Cancelar</button>
                <button type="submit" className="btn btn-primary">Crear</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {modalFinalizar && (
        <div className="modal-overlay" onClick={() => setModalFinalizar(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>Finalizar Asignación</h2>
            {errores.general && <p style={{ color: 'var(--danger)', margin: '0 0 0.75rem' }}>{errores.general}</p>}
            <p style={{ color: 'var(--text-secondary)', marginBottom: '0.75rem' }}>
              Equipo: {equipoLabel(asignacionSeleccionada?.equipo_id)} | Empleado: {empleadoLabel(asignacionSeleccionada?.empleado_id)}
            </p>
            <form onSubmit={solicitarConfirmacionFinalizar}>
              <label className="form-label">Fecha de Fin</label>
              <input name="fecha_fin" type="date" value={formFinalizar.fecha_fin} onChange={(e) => setFormFinalizar({ ...formFinalizar, fecha_fin: e.target.value })} className="form-input" required />
              <label className="form-label">Estado del Equipo (Devolución)</label>
              <select name="estado_equipo_devolucion" value={formFinalizar.estado_equipo_devolucion} onChange={(e) => setFormFinalizar({ ...formFinalizar, estado_equipo_devolucion: e.target.value })} className="form-select">
                <option value="nuevo">Nuevo</option>
                <option value="bueno">Bueno</option>
                <option value="dañado">Dañado</option>
                <option value="de baja">De baja</option>
              </select>
              <div style={{ display: 'flex', gap: '0.6rem', justifyContent: 'flex-end', marginTop: '1.25rem' }}>
                <button type="button" onClick={() => setModalFinalizar(false)} className="btn btn-secondary">Cancelar</button>
                <button type="submit" className="btn btn-warning">Finalizar</button>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmModal
        isOpen={confirmFinalizar}
        title="Finalizar Asignación"
        message={`¿Estás seguro de que deseas finalizar la asignación del equipo ${equipoLabel(asignacionSeleccionada?.equipo_id)} a ${empleadoLabel(asignacionSeleccionada?.empleado_id)}?`}
        onConfirm={ejecutarFinalizar}
        onCancel={() => setConfirmFinalizar(false)}
        confirmText="Finalizar"
        variant="warning"
      />

      <ConfirmModal
        isOpen={confirmDelete.isOpen}
        title="Eliminar Asignación"
        message="¿Estás seguro de que deseas eliminar esta asignación permanentemente? Esta acción no se puede deshacer."
        onConfirm={ejecutarEliminar}
        onCancel={() => setConfirmDelete({ isOpen: false, id: null })}
        confirmText="Eliminar"
        variant="danger"
      />

      <ImportExcelModal
        isOpen={importModalOpen}
        onClose={() => setImportModalOpen(false)}
        title="Importar Asignaciones desde Excel"
        endpoint="asignacion"
        onSuccess={() => { setImportModalOpen(false); cargarAsignaciones(); }}
      />
    </div>
  );
};

export default Asignaciones;
