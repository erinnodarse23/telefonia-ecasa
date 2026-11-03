import { useState, useEffect } from 'react';
import { toast } from 'sonner';
import { FiPlus, FiTrash2, FiUpload, FiEdit2, FiToggleLeft, FiToggleRight } from 'react-icons/fi';
import { useAuth } from '../contexts/AuthContext';
import api from '../services/api';
import ConfirmModal from '../components/ConfirmModal';
import ImportExcelModal from '../components/ImportExcelModal';
import SkeletonLoader from '../components/SkeletonLoader';
import EmptyState from '../components/EmptyState';

const UEB = () => {
  const { usuario } = useAuth();
  const [uebs, setUebs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalAbierto, setModalAbierto] = useState(false);
  const [modoEdicion, setModoEdicion] = useState(false);
  const [uebSeleccionada, setUebSeleccionada] = useState(null);
  const [form, setForm] = useState({ codigo: '', nombre: '', descripcion: '' });
  const [errores, setErrores] = useState({});
  const [confirmToggle, setConfirmToggle] = useState({ isOpen: false, id: null, activar: false });
  const [confirmDelete, setConfirmDelete] = useState({ isOpen: false, id: null });
  const [importModalOpen, setImportModalOpen] = useState(false);

  const cargarUEB = async () => {
    try {
      const { data } = await api.get('/ueb');
      setUebs(data);
    } catch (err) {
      console.error('Error al cargar UEB:', err);
      toast.error('Error al cargar UEB');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    cargarUEB();
  }, []);

  const validar = () => {
    const errs = {};
    if (!form.codigo || !form.codigo.trim()) {
      errs.codigo = 'El código es obligatorio';
    } else if (form.codigo.trim().length < 2 || form.codigo.trim().length > 10) {
      errs.codigo = 'El código debe tener entre 2 y 10 caracteres';
    } else if (!/^[a-zA-Z0-9]+$/.test(form.codigo.trim())) {
      errs.codigo = 'El código solo puede contener letras y números';
    }
    if (!form.nombre || !form.nombre.trim()) {
      errs.nombre = 'El nombre es obligatorio';
    } else if (form.nombre.trim().length > 100) {
      errs.nombre = 'El nombre no puede exceder los 100 caracteres';
    }
    setErrores(errs);
    return Object.keys(errs).length === 0;
  };

  const abrirModal = (ueb = null) => {
    if (ueb) {
      setModoEdicion(true);
      setUebSeleccionada(ueb);
      setForm({ codigo: ueb.codigo, nombre: ueb.nombre, descripcion: ueb.descripcion || '' });
    } else {
      setModoEdicion(false);
      setUebSeleccionada(null);
      setForm({ codigo: '', nombre: '', descripcion: '' });
    }
    setErrores({});
    setModalAbierto(true);
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setErrores({ ...errores, [e.target.name]: '' });
  };

  const crearUEB = async (e) => {
    e.preventDefault();
    if (!validar()) return;
    try {
      await api.post('/ueb', form);
      setModalAbierto(false);
      toast.success('UEB creada exitosamente');
      cargarUEB();
    } catch (err) {
      const msg = err.response?.data?.error || 'Error al crear UEB';
      setErrores({ general: msg });
      toast.error(msg);
    }
  };

  const actualizarUEB = async (e) => {
    e.preventDefault();
    if (!validar()) return;
    try {
      await api.put(`/ueb/${uebSeleccionada.id}`, form);
      setModalAbierto(false);
      toast.success('UEB actualizada exitosamente');
      cargarUEB();
    } catch (err) {
      const msg = err.response?.data?.error || 'Error al actualizar UEB';
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
      await api.delete(`/ueb/${id}`);
      toast.success('UEB eliminada permanentemente');
      cargarUEB();
    } catch (err) {
      console.error('Error al eliminar UEB:', err);
      toast.error(err.response?.data?.error || 'Error al eliminar UEB');
    }
  };

  const ejecutarToggle = async () => {
    const { id, activar } = confirmToggle;
    setConfirmToggle({ isOpen: false, id: null, activar: false });
    try {
      if (activar) {
        await api.put(`/ueb/${id}/activar`);
        toast.success('UEB activada exitosamente');
      } else {
        await api.put(`/ueb/${id}/desactivar`);
        toast.success('UEB desactivada exitosamente');
      }
      cargarUEB();
    } catch (err) {
      console.error('Error al cambiar estado de UEB:', err);
      toast.error(err.response?.data?.error || 'Error al cambiar estado de la UEB');
    }
  };

  if (loading) return <SkeletonLoader rows={5} columns={5} />;

  return (
    <div style={{ animation: 'fadeInUp 0.35s ease' }}>
      <div className="page-header">
        <h1>Unidades Empresariales</h1>
        <div style={{ display: 'flex', gap: '0.6rem' }}>
          <button onClick={() => setImportModalOpen(true)} className="btn btn-primary btn-sm" title="Importar Excel"><FiUpload size={14} /> Importar</button>
          <button onClick={() => abrirModal()} className="btn btn-primary"><FiPlus size={16} /> Nueva UEB</button>
        </div>
      </div>

      <div className="table-container">
        <div className="table-responsive">
          <table>
            <thead>
              <tr>
                <th>Código</th>
                <th>Nombre</th>
                <th>Descripción</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {uebs.map((ueb, i) => (
                <tr key={ueb.id} style={{ animation: `fadeInUp 0.25s ease ${i * 0.025}s both` }}>
                  <td style={{ fontWeight: 600 }}>{ueb.codigo}</td>
                  <td>{ueb.nombre}</td>
                  <td style={{ maxWidth: 250, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{ueb.descripcion}</td>
                  <td>
                    <span className={`badge ${ueb.activo ? 'badge-success' : 'badge-outline'}`}>
                      {ueb.activo ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: 4 }}>
                      <button onClick={() => abrirModal(ueb)} className="btn btn-secondary btn-sm" title="Editar"><FiEdit2 size={13} /></button>
                      {ueb.activo ? (
                        <button onClick={() => confirmarToggle(ueb.id, false)} className="btn btn-warning btn-sm" title="Desactivar"><FiToggleLeft size={13} /></button>
                      ) : (
                        <button onClick={() => confirmarToggle(ueb.id, true)} className="btn btn-primary btn-sm" title="Activar"><FiToggleRight size={13} /></button>
                      )}
                      {usuario?.rol === 'Administrador' && (
                        <button onClick={() => confirmarEliminar(ueb.id)} className="btn btn-danger btn-sm" title="Eliminar"><FiTrash2 size={13} /></button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
              {uebs.length === 0 && <EmptyState message="No hay UEB registradas" />}
            </tbody>
          </table>
        </div>
      </div>

      {modalAbierto && (
        <div className="modal-overlay" onClick={() => setModalAbierto(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>{modoEdicion ? 'Editar UEB' : 'Nueva UEB'}</h2>
            {errores.general && <p style={{ color: 'var(--danger)', margin: '0 0 0.75rem' }}>{errores.general}</p>}
            <form onSubmit={modoEdicion ? actualizarUEB : crearUEB}>
              <label className="form-label">Código</label>
              <input name="codigo" value={form.codigo} onChange={handleChange} className={`form-input${errores.codigo ? ' input-error' : ''}`} required />
              {errores.codigo && <span className="field-error">{errores.codigo}</span>}
              <label className="form-label">Nombre</label>
              <input name="nombre" value={form.nombre} onChange={handleChange} className={`form-input${errores.nombre ? ' input-error' : ''}`} required />
              {errores.nombre && <span className="field-error">{errores.nombre}</span>}
              <label className="form-label">Descripción</label>
              <textarea name="descripcion" value={form.descripcion} onChange={handleChange} className="form-textarea" rows={3} />
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
        title={confirmToggle.activar ? 'Activar UEB' : 'Desactivar UEB'}
        message={confirmToggle.activar ? '¿Estás seguro de que deseas activar esta UEB?' : '¿Estás seguro de que deseas desactivar esta UEB?'}
        onConfirm={ejecutarToggle}
        onCancel={() => setConfirmToggle({ isOpen: false, id: null, activar: false })}
        confirmText={confirmToggle.activar ? 'Activar' : 'Desactivar'}
        variant={confirmToggle.activar ? 'primary' : 'warning'}
      />

      <ConfirmModal
        isOpen={confirmDelete.isOpen}
        title="Eliminar UEB"
        message="¿Estás seguro de que deseas eliminar esta UEB permanentemente? Esta acción no se puede deshacer."
        onConfirm={ejecutarEliminar}
        onCancel={() => setConfirmDelete({ isOpen: false, id: null })}
        confirmText="Eliminar"
        variant="danger"
      />

      <ImportExcelModal
        isOpen={importModalOpen}
        onClose={() => setImportModalOpen(false)}
        title="Importar UEB desde Excel"
        endpoint="ueb"
        onSuccess={() => { setImportModalOpen(false); cargarUEB(); }}
      />
    </div>
  );
};

export default UEB;
