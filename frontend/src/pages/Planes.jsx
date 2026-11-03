import { useState, useEffect, useMemo } from 'react';
import { toast } from 'sonner';
import { FiPlus, FiTrash2, FiUpload, FiEdit2, FiCheckSquare } from 'react-icons/fi';
import { useAuth } from '../contexts/AuthContext';
import api from '../services/api';
import ConfirmModal from '../components/ConfirmModal';
import ImportExcelModal from '../components/ImportExcelModal';
import SkeletonLoader from '../components/SkeletonLoader';
import EmptyState from '../components/EmptyState';
import { formatDate, hoyLocal, toInputDate } from '../utils/formatDate';

const Planes = () => {
  const { usuario } = useAuth();
  const [planes, setPlanes] = useState([]);
  const [equipos, setEquipos] = useState([]);
  const [loading, setLoading] = useState(true);

  const [filtroEquipo, setFiltroEquipo] = useState('');

  const [modalAbierto, setModalAbierto] = useState(false);
  const [modoEdicion, setModoEdicion] = useState(false);
  const [planSeleccionado, setPlanSeleccionado] = useState(null);
  const [modalFinalizar, setModalFinalizar] = useState(false);
  const [form, setForm] = useState({ equipo_id: '', voz_plan: '', datos_plan: '', sms_plan: '', cuota: '', fecha_inicio: hoyLocal() });
  const [formFinalizar, setFormFinalizar] = useState({ fecha_fin: hoyLocal() });
  const [errores, setErrores] = useState({});
  const [confirmFinalizar, setConfirmFinalizar] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState({ isOpen: false, id: null });
  const [importModalOpen, setImportModalOpen] = useState(false);

  const cargarDatos = async () => {
    try {
      const [planesRes, equiposRes] = await Promise.all([
        api.get('/planes'),
        api.get('/equipos')
      ]);
      setPlanes(planesRes.data);
      setEquipos(equiposRes.data);
    } catch (err) {
      console.error('Error al cargar datos:', err);
      toast.error('Error al cargar datos');
    } finally {
      setLoading(false);
    }
  };

  const planesFiltrados = useMemo(
    () => filtroEquipo
      ? planes.filter(p => p.equipo_id === Number(filtroEquipo))
      : planes,
    [planes, filtroEquipo]
  );

  const equiposConPlanActivo = useMemo(
    () => new Set(planes.filter(p => !p.fecha_fin).map(p => p.equipo_id)),
    [planes]
  );

  useEffect(() => {
    cargarDatos();
  }, []);

  const equipoNombre = (id) => {
    const eq = equipos.find(e => e.id === id);
    return eq ? `${eq.numero_linea} - ${eq.marca} ${eq.modelo}` : `#${id}`;
  };

  const validar = () => {
    const errs = {};
    if (!modoEdicion && !form.equipo_id) {
      errs.equipo_id = 'Debe seleccionar un equipo';
    }
    if (form.voz_plan !== '' && form.voz_plan !== null && form.voz_plan !== undefined) {
      const num = Number(form.voz_plan);
      if (isNaN(num) || num < 0) errs.voz_plan = 'Debe ser un número no negativo';
    }
    if (form.datos_plan !== '' && form.datos_plan !== null && form.datos_plan !== undefined) {
      const num = Number(form.datos_plan);
      if (isNaN(num) || num < 0) errs.datos_plan = 'Debe ser un número no negativo';
    }
    if (form.sms_plan !== '' && form.sms_plan !== null && form.sms_plan !== undefined) {
      const num = Number(form.sms_plan);
      if (isNaN(num) || num < 0) errs.sms_plan = 'Debe ser un número no negativo';
    }
    if (form.cuota !== '' && form.cuota !== null && form.cuota !== undefined) {
      const num = Number(form.cuota);
      if (isNaN(num) || num <= 0) errs.cuota = 'La cuota debe ser mayor que 0';
    }
    setErrores(errs);
    return Object.keys(errs).length === 0;
  };

  const abrirModalCrear = () => {
    setModoEdicion(false);
    setPlanSeleccionado(null);
    setForm({ equipo_id: '', voz_plan: '', datos_plan: '', sms_plan: '', cuota: '', fecha_inicio: hoyLocal() });
    setErrores({});
    setModalAbierto(true);
  };

  const abrirModalEditar = (plan) => {
    setModoEdicion(true);
    setPlanSeleccionado(plan);
    setForm({
      equipo_id: plan.equipo_id,
      voz_plan: plan.voz_plan ?? '',
      datos_plan: plan.datos_plan ?? '',
      sms_plan: plan.sms_plan ?? '',
      cuota: plan.cuota ?? '',
      fecha_inicio: plan.fecha_inicio ? toInputDate(plan.fecha_inicio) : ''
    });
    setErrores({});
    setModalAbierto(true);
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setErrores({ ...errores, [e.target.name]: '' });
  };

  const guardar = async (e) => {
    e.preventDefault();
    if (!validar()) return;
    const payload = {
      ...form,
      equipo_id: Number(form.equipo_id),
      voz_plan: form.voz_plan === '' || form.voz_plan === null || form.voz_plan === undefined ? undefined : Number(form.voz_plan),
      datos_plan: form.datos_plan === '' || form.datos_plan === null || form.datos_plan === undefined ? undefined : Number(form.datos_plan),
      sms_plan: form.sms_plan === '' || form.sms_plan === null || form.sms_plan === undefined ? undefined : Number(form.sms_plan),
      cuota: form.cuota === '' || form.cuota === null || form.cuota === undefined ? undefined : Number(form.cuota),
    };
    console.log('[Planes] Payload enviado:', payload);
    try {
      if (modoEdicion) {
        await api.put(`/planes/${planSeleccionado.id}`, payload);
        toast.success('Plan actualizado exitosamente');
      } else {
        await api.post('/planes', payload);
        toast.success('Plan creado exitosamente');
      }
      setModalAbierto(false);
      const { data } = await api.get('/planes');
      setPlanes(data);
    } catch (err) {
      const msg = err.response?.data?.error || 'Error al guardar plan';
      setErrores({ general: msg });
      toast.error(msg);
    }
  };

  const abrirFinalizar = (plan) => {
    setPlanSeleccionado(plan);
    setFormFinalizar({ fecha_fin: hoyLocal() });
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
      await api.put(`/planes/${planSeleccionado.id}/finalizar`, formFinalizar);
      setModalFinalizar(false);
      setPlanSeleccionado(null);
      toast.success('Plan finalizado exitosamente');
      const { data } = await api.get('/planes');
      setPlanes(data);
    } catch (err) {
      console.error('Error al finalizar plan:', err);
      setErrores({ general: err.response?.data?.error || 'Error al finalizar plan' });
      toast.error(err.response?.data?.error || 'Error al finalizar plan');
    }
  };

  const confirmarEliminar = (id) => {
    setConfirmDelete({ isOpen: true, id });
  };

  const ejecutarEliminar = async () => {
    const id = confirmDelete.id;
    setConfirmDelete({ isOpen: false, id: null });
    try {
      await api.delete(`/planes/${id}`);
      toast.success('Plan eliminado permanentemente');
      cargarDatos();
    } catch (err) {
      console.error('Error al eliminar plan:', err);
      toast.error(err.response?.data?.error || 'Error al eliminar plan');
    }
  };

  if (loading) return <SkeletonLoader rows={6} columns={9} />;

  return (
    <div style={{ animation: 'fadeInUp 0.35s ease' }}>
      <div className="page-header">
        <h1>Planes</h1>
        <div style={{ display: 'flex', gap: '0.6rem' }}>
          <button onClick={() => setImportModalOpen(true)} className="btn btn-primary btn-sm" title="Importar Excel"><FiUpload size={14} /> Importar</button>
          <button onClick={abrirModalCrear} className="btn btn-primary"><FiPlus size={16} /> Nuevo Plan</button>
        </div>
      </div>

      <div className="filter-bar">
        <select value={filtroEquipo} onChange={(e) => setFiltroEquipo(e.target.value)} className="form-select" style={{ width: 'auto', minWidth: 200, flex: 'none' }}>
          <option value="">Todos los equipos</option>
          {equipos.map(eq => (
            <option key={eq.id} value={eq.id}>
              {eq.numero_linea} - {eq.marca} {eq.modelo}
              {equiposConPlanActivo.has(eq.id) ? ' (con plan activo)' : ''}
            </option>
          ))}
        </select>
      </div>

      <div className="table-container">
        <div className="table-responsive">
          <table>
            <thead>
              <tr>
                <th>Equipo</th>
                <th>Voz (min)</th>
                <th>Datos (MB)</th>
                <th>SMS</th>
                <th>Cuota</th>
                <th>Inicio</th>
                <th>Fin</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {planesFiltrados.map((p, i) => (
                <tr key={p.id} style={{ animation: `fadeInUp 0.25s ease ${i * 0.025}s both` }}>
                  <td style={{ fontWeight: 600 }}>{equipoNombre(p.equipo_id)}</td>
                  <td>{p.voz_plan ?? '-'}</td>
                  <td>{p.datos_plan ?? '-'}</td>
                  <td>{p.sms_plan ?? '-'}</td>
                  <td>{p.cuota ? `$${parseFloat(p.cuota).toFixed(2)}` : '-'}</td>
                  <td>{p.fecha_inicio ? formatDate(p.fecha_inicio) : '-'}</td>
                  <td>{p.fecha_fin ? formatDate(p.fecha_fin) : '-'}</td>
                  <td>{p.fecha_fin ? <span className="badge badge-danger">Inactivo</span> : <span className="badge badge-success">Activo</span>}</td>
                  <td>
                    <div style={{ display: 'flex', gap: 4 }}>
                      <button onClick={() => abrirModalEditar(p)} className="btn btn-secondary btn-sm" title="Editar"><FiEdit2 size={13} /></button>
                      {!p.fecha_fin && (
                        <button onClick={() => abrirFinalizar(p)} className="btn btn-warning btn-sm" title="Finalizar"><FiCheckSquare size={13} /></button>
                      )}
                      {usuario?.rol === 'Administrador' && (
                        <button onClick={() => confirmarEliminar(p.id)} className="btn btn-danger btn-sm" title="Eliminar"><FiTrash2 size={13} /></button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
              {planesFiltrados.length === 0 && <EmptyState message="No hay planes registrados" />}
            </tbody>
          </table>
        </div>
      </div>

      {modalAbierto && (
        <div className="modal-overlay" onClick={() => setModalAbierto(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>{modoEdicion ? 'Editar Plan' : 'Nuevo Plan'}</h2>
            {errores.general && <p style={{ color: 'var(--danger)', margin: '0 0 0.75rem' }}>{errores.general}</p>}
            <form onSubmit={guardar}>
              {!modoEdicion && (
                <>
                  <label className="form-label">Equipo</label>
                  <select name="equipo_id" value={form.equipo_id} onChange={handleChange} className={`form-select${errores.equipo_id ? ' input-error' : ''}`} required>
                    <option value="">Seleccione un equipo</option>
                    {equipos
                      .filter(e => e.activo !== false)
                      .map(e => {
                        const conPlan = equiposConPlanActivo.has(e.id);
                        return (
                          <option key={e.id} value={e.id} disabled={conPlan}>
                            {e.numero_linea} - {e.marca} {e.modelo}{conPlan ? ' (ya tiene plan activo)' : ''}
                          </option>
                        );
                      })}
                  </select>
                  {errores.equipo_id && <span className="field-error">{errores.equipo_id}</span>}
                  {equiposConPlanActivo.has(Number(form.equipo_id)) && (
                    <p style={{ color: 'var(--warning)', fontSize: 12, margin: '4px 0 0' }}>
                      Este equipo ya tiene un plan activo. Al crear uno nuevo se cerrará automáticamente.
                    </p>
                  )}
                </>
              )}
              <label className="form-label">Voz (minutos)</label>
              <input name="voz_plan" type="number" value={form.voz_plan} onChange={handleChange} className={`form-input${errores.voz_plan ? ' input-error' : ''}`} />
              {errores.voz_plan && <span className="field-error">{errores.voz_plan}</span>}
              <label className="form-label">Datos (MB)</label>
              <input name="datos_plan" type="number" value={form.datos_plan} onChange={handleChange} className={`form-input${errores.datos_plan ? ' input-error' : ''}`} />
              {errores.datos_plan && <span className="field-error">{errores.datos_plan}</span>}
              <label className="form-label">SMS</label>
              <input name="sms_plan" type="number" value={form.sms_plan} onChange={handleChange} className={`form-input${errores.sms_plan ? ' input-error' : ''}`} />
              {errores.sms_plan && <span className="field-error">{errores.sms_plan}</span>}
              <label className="form-label">Cuota ($)</label>
              <input name="cuota" type="number" step="0.01" value={form.cuota} onChange={handleChange} className={`form-input${errores.cuota ? ' input-error' : ''}`} />
              {errores.cuota && <span className="field-error">{errores.cuota}</span>}
              <label className="form-label">Fecha de Inicio</label>
              <input name="fecha_inicio" type="date" value={form.fecha_inicio} onChange={handleChange} className="form-input" required />
              <div style={{ display: 'flex', gap: '0.6rem', justifyContent: 'flex-end', marginTop: '1.25rem' }}>
                <button type="button" onClick={() => setModalAbierto(false)} className="btn btn-secondary">Cancelar</button>
                <button type="submit" className="btn btn-primary">{modoEdicion ? 'Guardar' : 'Crear'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {modalFinalizar && (
        <div className="modal-overlay" onClick={() => setModalFinalizar(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>Finalizar Plan</h2>
            {errores.general && <p style={{ color: 'var(--danger)', margin: '0 0 0.75rem' }}>{errores.general}</p>}
            <p style={{ color: 'var(--text-secondary)', marginBottom: '0.75rem' }}>
              Equipo: {equipoNombre(planSeleccionado?.equipo_id)}
            </p>
            <form onSubmit={solicitarConfirmacionFinalizar}>
              <label className="form-label">Fecha de Fin</label>
              <input name="fecha_fin" type="date" value={formFinalizar.fecha_fin} onChange={(e) => setFormFinalizar({ ...formFinalizar, fecha_fin: e.target.value })} className="form-input" required />
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
        title="Finalizar Plan"
        message={`¿Estás seguro de que deseas finalizar el plan del equipo ${equipoNombre(planSeleccionado?.equipo_id)}?`}
        onConfirm={ejecutarFinalizar}
        onCancel={() => setConfirmFinalizar(false)}
        confirmText="Finalizar"
        variant="warning"
      />

      <ConfirmModal
        isOpen={confirmDelete.isOpen}
        title="Eliminar Plan"
        message="¿Estás seguro de que deseas eliminar este plan permanentemente? Esta acción no se puede deshacer."
        onConfirm={ejecutarEliminar}
        onCancel={() => setConfirmDelete({ isOpen: false, id: null })}
        confirmText="Eliminar"
        variant="danger"
      />

      <ImportExcelModal
        isOpen={importModalOpen}
        onClose={() => setImportModalOpen(false)}
        title="Importar Planes desde Excel"
        endpoint="plan"
        onSuccess={() => { setImportModalOpen(false); cargarDatos(); }}
      />
    </div>
  );
};

export default Planes;
