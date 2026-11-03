import { useState, useEffect, useMemo } from 'react';
import { toast } from 'sonner';
import { FiPlus, FiTrash2, FiUpload, FiEdit2, FiToggleLeft, FiToggleRight } from 'react-icons/fi';
import { useAuth } from '../contexts/AuthContext';
import api from '../services/api';
import ConfirmModal from '../components/ConfirmModal';
import ImportExcelModal from '../components/ImportExcelModal';
import SkeletonLoader from '../components/SkeletonLoader';
import EmptyState from '../components/EmptyState';

const estados = ['nuevo', 'bueno', 'dañado', 'de baja'];

const Equipos = () => {
  const { usuario } = useAuth();
  const [equipos, setEquipos] = useState([]);
  const [asignaciones, setAsignaciones] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filtroEstado, setFiltroEstado] = useState('');
  const [filtroDisponibilidad, setFiltroDisponibilidad] = useState('');
  const [modalAbierto, setModalAbierto] = useState(false);
  const [modoEdicion, setModoEdicion] = useState(false);
  const [equipoSeleccionado, setEquipoSeleccionado] = useState(null);
  const [form, setForm] = useState({ numero_linea: '', marca: '', modelo: '', estado_fisico: 'bueno', observaciones: '' });
  const [errores, setErrores] = useState({});
  const [confirmToggle, setConfirmToggle] = useState({ isOpen: false, id: null, activar: false });
  const [confirmDelete, setConfirmDelete] = useState({ isOpen: false, id: null });
  const [importModalOpen, setImportModalOpen] = useState(false);

  const cargarEquipos = async () => {
    try {
      const endpoint = filtroEstado ? `/equipos?estado=${filtroEstado}` : '/equipos';
      const [eqRes, asigRes] = await Promise.all([
        api.get(endpoint),
        api.get('/asignaciones?estado=activas')
      ]);
      setEquipos(eqRes.data);
      setAsignaciones(asigRes.data);
    } catch (err) {
      console.error('Error al cargar equipos:', err);
      toast.error('Error al cargar equipos');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    cargarEquipos();
  }, [filtroEstado]);

  const validar = () => {
    const errs = {};
    if (!form.numero_linea || !form.numero_linea.trim()) {
      errs.numero_linea = 'El número de línea es obligatorio';
    }
    if (!form.estado_fisico) {
      errs.estado_fisico = 'Debe seleccionar un estado físico';
    }
    setErrores(errs);
    return Object.keys(errs).length === 0;
  };

  const abrirModal = (equipo = null) => {
    if (equipo) {
      setModoEdicion(true);
      setEquipoSeleccionado(equipo);
      setForm({ numero_linea: equipo.numero_linea, marca: equipo.marca || '', modelo: equipo.modelo || '', estado_fisico: equipo.estado_fisico, observaciones: equipo.observaciones || '' });
    } else {
      setModoEdicion(false);
      setEquipoSeleccionado(null);
      setForm({ numero_linea: '', marca: '', modelo: '', estado_fisico: 'bueno', observaciones: '' });
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
      if (modoEdicion) {
        await api.put(`/equipos/${equipoSeleccionado.id}`, form);
        toast.success('Equipo actualizado exitosamente');
      } else {
        await api.post('/equipos', form);
        toast.success('Equipo creado exitosamente');
      }
      setModalAbierto(false);
      cargarEquipos();
    } catch (err) {
      const msg = err.response?.data?.error || 'Error al guardar equipo';
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
      await api.delete(`/equipos/${id}`);
      toast.success('Equipo eliminado permanentemente');
      cargarEquipos();
    } catch (err) {
      console.error('Error al eliminar equipo:', err);
      toast.error(err.response?.data?.error || 'Error al eliminar equipo');
    }
  };

  const ejecutarToggle = async () => {
    const { id, activar } = confirmToggle;
    setConfirmToggle({ isOpen: false, id: null, activar: false });
    try {
      if (activar) {
        await api.put(`/equipos/${id}/activar`);
        toast.success('Equipo activado exitosamente');
      } else {
        await api.put(`/equipos/${id}/desactivar`);
        toast.success('Equipo desactivado exitosamente');
      }
      cargarEquipos();
    } catch (err) {
      console.error('Error al cambiar estado del equipo:', err);
      toast.error(err.response?.data?.error || 'Error al cambiar estado del equipo');
    }
  };

  const equiposAsignados = useMemo(
    () => new Set(asignaciones.map(a => a.equipo_id)),
    [asignaciones]
  );

  const equiposFiltrados = useMemo(
    () => {
      let lista = equipos;
      if (filtroDisponibilidad === 'disponibles') {
        lista = lista.filter(e => !equiposAsignados.has(e.id));
      } else if (filtroDisponibilidad === 'asignados') {
        lista = lista.filter(e => equiposAsignados.has(e.id));
      }
      return lista;
    },
    [equipos, filtroDisponibilidad, equiposAsignados]
  );

  if (loading) return <SkeletonLoader rows={5} columns={8} />;

  return (
    <div style={{ animation: 'fadeInUp 0.35s ease' }}>
      <div className="page-header">
        <h1>Equipos Móviles</h1>
        <div className="filter-bar" style={{ marginBottom: 0 }}>
          <select value={filtroEstado} onChange={(e) => setFiltroEstado(e.target.value)} className="form-select" style={{ width: 160 }}>
            <option value="">Todos los estados</option>
            {estados.map(e => <option key={e} value={e}>{e.charAt(0).toUpperCase() + e.slice(1)}</option>)}
          </select>
          <select value={filtroDisponibilidad} onChange={(e) => setFiltroDisponibilidad(e.target.value)} className="form-select" style={{ width: 160 }}>
            <option value="">Todos</option>
            <option value="disponibles">Disponibles</option>
            <option value="asignados">Asignados</option>
          </select>
          <button onClick={() => setImportModalOpen(true)} className="btn btn-primary btn-sm" title="Importar Excel"><FiUpload size={14} /> Importar</button>
          <button onClick={() => abrirModal()} className="btn btn-primary"><FiPlus size={16} /> Nuevo Equipo</button>
        </div>
      </div>

      <div className="table-container">
        <div className="table-responsive">
          <table>
            <thead>
              <tr>
                <th>Número</th>
                <th>Marca</th>
                <th>Modelo</th>
                <th>Estado</th>
                <th>Observaciones</th>
                <th>Disponibilidad</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {equiposFiltrados.map((eq, i) => (
                <tr key={eq.id} style={{ animation: `fadeInUp 0.25s ease ${i * 0.025}s both` }}>
                  <td style={{ fontWeight: 600 }}>{eq.numero_linea}</td>
                  <td>{eq.marca}</td>
                  <td>{eq.modelo}</td>
                  <td>{eq.estado_fisico}</td>
                  <td style={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{eq.observaciones}</td>
                  <td>
                    <span className={`badge ${equiposAsignados.has(eq.id) ? 'badge-outline' : 'badge-success'}`}>
                      {equiposAsignados.has(eq.id) ? '🔴 Asignado' : '🟢 Disponible'}
                    </span>
                  </td>
                  <td>
                    <span className={`badge ${eq.activo ? 'badge-success' : 'badge-outline'}`}>
                      {eq.activo ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: 4 }}>
                      <button onClick={() => abrirModal(eq)} className="btn btn-secondary btn-sm" title="Editar"><FiEdit2 size={13} /></button>
                      {eq.activo ? (
                        <button onClick={() => confirmarToggle(eq.id, false)} className="btn btn-warning btn-sm" title="Desactivar"><FiToggleLeft size={13} /></button>
                      ) : (
                        <button onClick={() => confirmarToggle(eq.id, true)} className="btn btn-primary btn-sm" title="Activar"><FiToggleRight size={13} /></button>
                      )}
                      {usuario?.rol === 'Administrador' && (
                        <button onClick={() => confirmarEliminar(eq.id)} className="btn btn-danger btn-sm" title="Eliminar"><FiTrash2 size={13} /></button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
              {equiposFiltrados.length === 0 && <EmptyState message="No hay equipos registrados" />}
            </tbody>
          </table>
        </div>
      </div>

      {modalAbierto && (
        <div className="modal-overlay" onClick={() => setModalAbierto(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>{modoEdicion ? 'Editar Equipo' : 'Nuevo Equipo'}</h2>
            {errores.general && <p style={{ color: 'var(--danger)', margin: '0 0 0.75rem' }}>{errores.general}</p>}
            <form onSubmit={guardar}>
              <label className="form-label">Número de Línea</label>
              <input name="numero_linea" value={form.numero_linea} onChange={handleChange} className={`form-input${errores.numero_linea ? ' input-error' : ''}`} required />
              {errores.numero_linea && <span className="field-error">{errores.numero_linea}</span>}
              <label className="form-label">Marca</label>
              <input name="marca" value={form.marca} onChange={handleChange} className="form-input" />
              <label className="form-label">Modelo</label>
              <input name="modelo" value={form.modelo} onChange={handleChange} className="form-input" />
              <label className="form-label">Estado Físico</label>
              <select name="estado_fisico" value={form.estado_fisico} onChange={handleChange} className={`form-select${errores.estado_fisico ? ' input-error' : ''}`}>
                {estados.map(e => <option key={e} value={e}>{e.charAt(0).toUpperCase() + e.slice(1)}</option>)}
              </select>
              {errores.estado_fisico && <span className="field-error">{errores.estado_fisico}</span>}
              <label className="form-label">Observaciones</label>
              <textarea name="observaciones" value={form.observaciones} onChange={handleChange} className="form-textarea" rows={3} />
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
        title={confirmToggle.activar ? 'Activar Equipo' : 'Desactivar Equipo'}
        message={confirmToggle.activar ? '¿Estás seguro de que deseas activar este equipo?' : '¿Estás seguro de que deseas desactivar este equipo?'}
        onConfirm={ejecutarToggle}
        onCancel={() => setConfirmToggle({ isOpen: false, id: null, activar: false })}
        confirmText={confirmToggle.activar ? 'Activar' : 'Desactivar'}
        variant={confirmToggle.activar ? 'primary' : 'warning'}
      />

      <ConfirmModal
        isOpen={confirmDelete.isOpen}
        title="Eliminar Equipo"
        message="¿Estás seguro de que deseas eliminar este equipo permanentemente? Esta acción no se puede deshacer."
        onConfirm={ejecutarEliminar}
        onCancel={() => setConfirmDelete({ isOpen: false, id: null })}
        confirmText="Eliminar"
        variant="danger"
      />

      <ImportExcelModal
        isOpen={importModalOpen}
        onClose={() => setImportModalOpen(false)}
        title="Importar Equipos desde Excel"
        endpoint="equipo"
        onSuccess={() => { setImportModalOpen(false); cargarEquipos(); }}
      />
    </div>
  );
};

export default Equipos;
