import { useState, useEffect, useMemo } from 'react';
import { toast } from 'sonner';
import { FiPlus, FiTrash2, FiUpload, FiEdit2, FiToggleLeft, FiToggleRight } from 'react-icons/fi';
import { useAuth } from '../contexts/AuthContext';
import api from '../services/api';
import ConfirmModal from '../components/ConfirmModal';
import ImportExcelModal from '../components/ImportExcelModal';
import SkeletonLoader from '../components/SkeletonLoader';
import EmptyState from '../components/EmptyState';

const Empleados = () => {
  const { usuario } = useAuth();
  const [empleados, setEmpleados] = useState([]);
  const [uebs, setUebs] = useState([]);
  const [asignaciones, setAsignaciones] = useState([]);
  const [equipos, setEquipos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filtroUeb, setFiltroUeb] = useState('');
  const [filtroLinea, setFiltroLinea] = useState('');
  const [modalAbierto, setModalAbierto] = useState(false);
  const [modoEdicion, setModoEdicion] = useState(false);
  const [empleadoSeleccionado, setEmpleadoSeleccionado] = useState(null);
  const [form, setForm] = useState({ nombre: '', cargo: '', departamento: '', ueb_id: '' });
  const [errores, setErrores] = useState({});
  const [confirmToggle, setConfirmToggle] = useState({ isOpen: false, id: null, activar: false });
  const [confirmDelete, setConfirmDelete] = useState({ isOpen: false, id: null });
  const [importModalOpen, setImportModalOpen] = useState(false);

  const cargarDatos = async () => {
    try {
      const endpoint = filtroUeb ? `/empleados/ueb/${filtroUeb}` : '/empleados';
      const [empRes, uebRes, asigRes, eqRes] = await Promise.all([
        api.get(endpoint),
        api.get('/ueb'),
        api.get('/asignaciones?estado=activas'),
        api.get('/equipos')
      ]);
      setEmpleados(empRes.data);
      setUebs(uebRes.data);
      setAsignaciones(asigRes.data);
      setEquipos(eqRes.data);
    } catch (err) {
      console.error('Error al cargar empleados:', err);
      toast.error('Error al cargar empleados');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    cargarDatos();
  }, [filtroUeb]);

  const validar = () => {
    const errs = {};
    if (!form.nombre || !form.nombre.trim()) {
      errs.nombre = 'El nombre es obligatorio';
    }
    if (!form.ueb_id) {
      errs.ueb_id = 'Debe seleccionar una UEB';
    }
    setErrores(errs);
    return Object.keys(errs).length === 0;
  };

  const abrirModal = (empleado = null) => {
    if (empleado) {
      setModoEdicion(true);
      setEmpleadoSeleccionado(empleado);
      setForm({ nombre: empleado.nombre, cargo: empleado.cargo || '', departamento: empleado.departamento || '', ueb_id: empleado.ueb_id });
    } else {
      setModoEdicion(false);
      setEmpleadoSeleccionado(null);
      setForm({ nombre: '', cargo: '', departamento: '', ueb_id: '' });
    }
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
    try {
      const { telefono, ...payload } = form;
      if (modoEdicion) {
        await api.put(`/empleados/${empleadoSeleccionado.id}`, payload);
        toast.success('Empleado actualizado exitosamente');
      } else {
        await api.post('/empleados', payload);
        toast.success('Empleado creado exitosamente');
      }
      setModalAbierto(false);
      cargarDatos();
    } catch (err) {
      const msg = err.response?.data?.error || 'Error al guardar empleado';
      setErrores({ general: msg });
      toast.error(msg);
    }
  };

  const confirmarToggle = (id, activar) => {
    setConfirmToggle({ isOpen: true, id, activar });
  };

  const confirmarEliminar = (id) => {
    setConfirmDelete({ isOpen: true, id });
  };

  const ejecutarEliminar = async () => {
    const id = confirmDelete.id;
    setConfirmDelete({ isOpen: false, id: null });
    try {
      await api.delete(`/empleados/${id}`);
      toast.success('Empleado eliminado permanentemente');
      cargarDatos();
    } catch (err) {
      console.error('Error al eliminar empleado:', err);
      toast.error(err.response?.data?.error || 'Error al eliminar empleado');
    }
  };

  const ejecutarToggle = async () => {
    const { id, activar } = confirmToggle;
    setConfirmToggle({ isOpen: false, id: null, activar: false });
    try {
      if (activar) {
        await api.put(`/empleados/${id}/activar`);
        toast.success('Empleado activado exitosamente');
      } else {
        await api.put(`/empleados/${id}/desactivar`);
        toast.success('Empleado desactivado exitosamente');
      }
      cargarDatos();
    } catch (err) {
      console.error('Error al cambiar estado del empleado:', err);
      toast.error(err.response?.data?.error || 'Error al cambiar estado del empleado');
    }
  };

  const equiposMap = useMemo(
    () => new Map(equipos.map(e => [e.id, e.numero_linea])),
    [equipos]
  );

  const telefonoPorEmpleado = useMemo(() => {
    const m = new Map();
    asignaciones.forEach(a => {
      const tel = equiposMap.get(a.equipo_id);
      if (tel) m.set(a.empleado_id, tel);
    });
    return m;
  }, [asignaciones, equiposMap]);

  const empleadosConLinea = useMemo(
    () => new Set(asignaciones.map(a => a.empleado_id)),
    [asignaciones]
  );

  const empleadosFiltrados = useMemo(() => {
    if (!filtroLinea) return empleados;
    const conLinea = filtroLinea === 'con';
    return empleados.filter(emp => empleadosConLinea.has(emp.id) === conLinea);
  }, [empleados, filtroLinea, empleadosConLinea]);

  if (loading) return <SkeletonLoader rows={5} columns={8} />;

  return (
    <div style={{ animation: 'fadeInUp 0.35s ease' }}>
      <div className="page-header">
        <h1>Empleados</h1>
        <div className="filter-bar" style={{ marginBottom: 0 }}>
          <select value={filtroUeb} onChange={(e) => setFiltroUeb(e.target.value)} className="form-select" style={{ width: 200 }}>
            <option value="">Todas las UEB</option>
            {uebs.filter(u => u.activo).map(u => <option key={u.id} value={u.id}>{u.nombre}</option>)}
          </select>
          <select value={filtroLinea} onChange={(e) => setFiltroLinea(e.target.value)} className="form-select" style={{ width: 160 }}>
            <option value="">Todas las líneas</option>
            <option value="con">Con línea</option>
            <option value="sin">Sin línea</option>
          </select>
          <button onClick={() => setImportModalOpen(true)} className="btn btn-primary btn-sm" title="Importar Excel"><FiUpload size={14} /> Importar</button>
          <button onClick={() => abrirModal()} className="btn btn-primary"><FiPlus size={16} /> Nuevo Empleado</button>
        </div>
      </div>

      <div className="table-container">
        <div className="table-responsive">
          <table>
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Cargo</th>
                <th>Departamento</th>
                <th>Teléfono</th>
                <th>UEB</th>
                <th>Asignación</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {empleadosFiltrados.map((emp, i) => (
                <tr key={emp.id} style={{ animation: `fadeInUp 0.25s ease ${i * 0.025}s both` }}>
                  <td style={{ fontWeight: 600 }}>{emp.nombre}</td>
                  <td>{emp.cargo}</td>
                  <td>{emp.departamento}</td>
                  <td>{telefonoPorEmpleado.get(emp.id) || emp.telefono || '—'}</td>
                  <td>{emp.ueb_nombre || uebs.find(u => u.id === emp.ueb_id)?.nombre}</td>
                  <td>
                    <span className={`badge ${empleadosConLinea.has(emp.id) ? 'badge-success' : 'badge-outline'}`}>
                      {empleadosConLinea.has(emp.id) ? '✅ Tiene línea' : '❌ Sin línea'}
                    </span>
                  </td>
                  <td>
                    <span className={`badge ${emp.activo ? 'badge-success' : 'badge-outline'}`}>
                      {emp.activo ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: 4 }}>
                      <button onClick={() => abrirModal(emp)} className="btn btn-secondary btn-sm" title="Editar"><FiEdit2 size={13} /></button>
                      {emp.activo ? (
                        <button onClick={() => confirmarToggle(emp.id, false)} className="btn btn-warning btn-sm" title="Desactivar"><FiToggleLeft size={13} /></button>
                      ) : (
                        <button onClick={() => confirmarToggle(emp.id, true)} className="btn btn-primary btn-sm" title="Activar"><FiToggleRight size={13} /></button>
                      )}
                      {usuario?.rol === 'Administrador' && (
                        <button onClick={() => confirmarEliminar(emp.id)} className="btn btn-danger btn-sm" title="Eliminar"><FiTrash2 size={13} /></button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
              {empleadosFiltrados.length === 0 && <EmptyState message="No hay empleados registrados" />}
            </tbody>
          </table>
        </div>
      </div>

      {modalAbierto && (
        <div className="modal-overlay" onClick={() => setModalAbierto(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>{modoEdicion ? 'Editar Empleado' : 'Nuevo Empleado'}</h2>
            {errores.general && <p style={{ color: 'var(--danger)', margin: '0 0 0.75rem' }}>{errores.general}</p>}
            <form onSubmit={guardar}>
              <label className="form-label">Nombre</label>
              <input name="nombre" value={form.nombre} onChange={handleChange} className={`form-input${errores.nombre ? ' input-error' : ''}`} required />
              {errores.nombre && <span className="field-error">{errores.nombre}</span>}
              <label className="form-label">Cargo</label>
              <input name="cargo" value={form.cargo} onChange={handleChange} className="form-input" />
              <label className="form-label">Departamento</label>
              <input name="departamento" value={form.departamento} onChange={handleChange} className="form-input" />
              <label className="form-label">UEB</label>
              <select name="ueb_id" value={form.ueb_id} onChange={handleChange} className={`form-select${errores.ueb_id ? ' input-error' : ''}`} required>
                <option value="">Seleccione una UEB</option>
                {uebs.filter(u => u.activo).map(u => <option key={u.id} value={u.id}>{u.nombre}</option>)}
              </select>
              {errores.ueb_id && <span className="field-error">{errores.ueb_id}</span>}
              <div style={{ display: 'flex', gap: '0.6rem', justifyContent: 'flex-end', marginTop: '1.25rem' }}>
                <button type="button" onClick={() => setModalAbierto(false)} className="btn btn-secondary">Cancelar</button>
                <button type="submit" className="btn btn-primary">{modoEdicion ? 'Guardar' : 'Crear'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmModal
        isOpen={confirmToggle.isOpen}
        title={confirmToggle.activar ? 'Activar Empleado' : 'Desactivar Empleado'}
        message={confirmToggle.activar ? '¿Estás seguro de que deseas activar este empleado?' : '¿Estás seguro de que deseas desactivar este empleado?'}
        onConfirm={ejecutarToggle}
        onCancel={() => setConfirmToggle({ isOpen: false, id: null, activar: false })}
        confirmText={confirmToggle.activar ? 'Activar' : 'Desactivar'}
        variant={confirmToggle.activar ? 'primary' : 'warning'}
      />

      <ConfirmModal
        isOpen={confirmDelete.isOpen}
        title="Eliminar Empleado"
        message="¿Estás seguro de que deseas eliminar este empleado permanentemente? Esta acción no se puede deshacer."
        onConfirm={ejecutarEliminar}
        onCancel={() => setConfirmDelete({ isOpen: false, id: null })}
        confirmText="Eliminar"
        variant="danger"
      />

      <ImportExcelModal
        isOpen={importModalOpen}
        onClose={() => setImportModalOpen(false)}
        title="Importar Empleados desde Excel"
        endpoint="empleado"
        onSuccess={() => { setImportModalOpen(false); cargarDatos(); }}
      />
    </div>
  );
};

export default Empleados;
