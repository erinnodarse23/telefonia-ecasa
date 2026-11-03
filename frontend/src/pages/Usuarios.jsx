import { useState, useEffect, useMemo } from 'react';
import { toast } from 'sonner';
import { FiPlus, FiTrash2, FiEdit2, FiToggleLeft, FiToggleRight, FiSearch, FiUpload } from 'react-icons/fi';
import api from '../services/api';
import ConfirmModal from '../components/ConfirmModal';
import ImportExcelModal from '../components/ImportExcelModal';

const roles = ['Administrador', 'GrupoComunicaciones', 'JefeUnidad'];

const Usuarios = () => {
  const [usuarios, setUsuarios] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filtroRol, setFiltroRol] = useState('');
  const [filtroEstado, setFiltroEstado] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [modalAbierto, setModalAbierto] = useState(false);
  const [modoEdicion, setModoEdicion] = useState(false);
  const [usuarioSeleccionado, setUsuarioSeleccionado] = useState(null);
  const [uebs, setUebs] = useState([]);
  const [form, setForm] = useState({ login: '', password: '', rol: 'Administrador', ueb_id: '' });
  const [resetPass, setResetPass] = useState(false);
  const [errores, setErrores] = useState({});
  const [confirm, setConfirm] = useState({ isOpen: false, id: null, action: '' });
  const [importModalOpen, setImportModalOpen] = useState(false);

  const cargarDatos = async () => {
    try {
      const [usersRes, uebsRes] = await Promise.all([
        api.get('/usuarios'),
        api.get('/ueb'),
      ]);
      setUsuarios(usersRes.data);
      setUebs(uebsRes.data);
    } catch (err) {
      console.error('Error al cargar datos:', err);
      toast.error('Error al cargar datos');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    cargarDatos();
  }, []);

  const filtrados = useMemo(() => {
    let items = [...usuarios];
    if (filtroRol) items = items.filter((u) => u.rol === filtroRol);
    if (filtroEstado === 'activo') items = items.filter((u) => u.activo);
    if (filtroEstado === 'inactivo') items = items.filter((u) => !u.activo);
    if (busqueda.trim()) {
      const q = busqueda.trim().toLowerCase();
      items = items.filter((u) => u.login.toLowerCase().includes(q));
    }
    return items;
  }, [usuarios, filtroRol, filtroEstado, busqueda]);

  const validar = () => {
    const errs = {};
    if (!form.login || form.login.trim().length < 3) {
      errs.login = 'El login debe tener al menos 3 caracteres';
    }
    if (!form.rol) {
      errs.rol = 'Debe seleccionar un rol';
    }
    if (form.rol === 'JefeUnidad' && !form.ueb_id) {
      errs.ueb_id = 'Debe seleccionar una UEB para el rol JefeUnidad';
    }
    if (!modoEdicion && (!form.password || form.password.length < 6)) {
      errs.password = 'La contraseña debe tener al menos 6 caracteres';
    }
    if (modoEdicion && resetPass && form.password && form.password.length < 6) {
      errs.password = 'La contraseña debe tener al menos 6 caracteres';
    }
    setErrores(errs);
    return Object.keys(errs).length === 0;
  };

  const abrirModal = (usuario = null) => {
    if (usuario) {
      setModoEdicion(true);
      setUsuarioSeleccionado(usuario);
      setForm({ login: usuario.login, password: '', rol: usuario.rol, ueb_id: usuario.ueb_id || '' });
      setResetPass(false);
    } else {
      setModoEdicion(false);
      setUsuarioSeleccionado(null);
      setForm({ login: '', password: '', rol: 'Administrador', ueb_id: '' });
      setResetPass(false);
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
        const payload = { login: form.login, rol: form.rol, ueb_id: form.ueb_id || null };
        if (resetPass && form.password) payload.password = form.password;
        await api.put(`/usuarios/${usuarioSeleccionado.id}`, payload);
        toast.success('Usuario actualizado exitosamente');
      } else {
        if (!form.password) { setErrores({ password: 'La contraseña es obligatoria' }); return; }
        await api.post('/usuarios', form);
        toast.success('Usuario creado exitosamente');
      }
      setModalAbierto(false);
      cargarDatos();
    } catch (err) {
      const msg = err.response?.data?.error || 'Error al guardar usuario';
      setErrores({ general: msg });
      toast.error(msg);
    }
  };

  const abrirConfirmacion = (id, action) => {
    setConfirm({ isOpen: true, id, action });
  };

  const ejecutarAccion = async () => {
    const { id, action } = confirm;
    setConfirm({ isOpen: false, id: null, action: '' });
    try {
      if (action === 'desactivar') {
        await api.put(`/usuarios/${id}/desactivar`);
        toast.success('Usuario desactivado correctamente');
      } else if (action === 'activar') {
        await api.put(`/usuarios/${id}/activar`);
        toast.success('Usuario activado correctamente');
      } else if (action === 'eliminar') {
        await api.delete(`/usuarios/${id}`);
        toast.success('Usuario eliminado permanentemente');
      }
      cargarDatos();
    } catch (err) {
      console.error('Error al ejecutar acción:', err);
      toast.error(err.response?.data?.error || 'Error al ejecutar acción');
    }
  };

  const confirmLabel = () => {
    if (confirm.action === 'desactivar') return 'Desactivar';
    if (confirm.action === 'activar') return 'Activar';
    return 'Eliminar';
  };

  const confirmTitle = () => {
    if (confirm.action === 'desactivar') return 'Desactivar Usuario';
    if (confirm.action === 'activar') return 'Activar Usuario';
    return 'Eliminar Usuario';
  };

  const confirmMessage = () => {
    if (confirm.action === 'desactivar') return '¿Desactivar usuario? El usuario no podrá iniciar sesión.';
    if (confirm.action === 'activar') return '¿Activar usuario? El usuario podrá iniciar sesión nuevamente.';
    return '¿Eliminar usuario permanentemente? Esta acción no se puede deshacer.';
  };

  const confirmVariant = () => {
    if (confirm.action === 'desactivar') return 'warning';
    if (confirm.action === 'activar') return 'primary';
    return 'danger';
  };

  if (loading) return (
    <div style={{ animation: 'fadeInUp 0.35s ease' }}>
      <h1 style={{ margin: '0 0 1.5rem', fontSize: '1.2rem', fontWeight: 800 }}>Usuarios del Sistema</h1>
      <div className="card" style={{ height: 200 }}>
        <div className="empty-state" style={{ paddingTop: '2rem' }}><p>Cargando usuarios...</p></div>
      </div>
    </div>
  );

  return (
    <div style={{ animation: 'fadeInUp 0.35s ease' }}>
      <div className="page-header">
        <h1>Usuarios del Sistema</h1>
        <div style={{ display: 'flex', gap: '0.6rem' }}>
          <button
            onClick={() => abrirModal()}
            className="btn btn-primary"
            style={{ display: 'flex', alignItems: 'center', gap: 6 }}
          >
            <FiPlus size={16} /> Nuevo Usuario
          </button>
          <button
            onClick={() => { setImportModalOpen(true); }}
            className="btn btn-primary btn-sm"
            title="Importar Excel"
          >
            <FiUpload size={14} /> Importar
          </button>
        </div>
      </div>

      <div className="card" style={{ marginBottom: '1rem' }}>
        <div style={{ display: 'flex', gap: '0.6rem', flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <div style={{ flex: 1, minWidth: 180 }}>
            <label className="form-label">Buscar</label>
            <div style={{ position: 'relative' }}>
              <FiSearch size={14} style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)', pointerEvents: 'none' }} />
              <input
                className="form-input"
                type="text"
                placeholder="Buscar por login..."
                value={busqueda}
                onChange={(e) => setBusqueda(e.target.value)}
                style={{ paddingLeft: 30, height: 38 }}
              />
            </div>
          </div>
          <div style={{ minWidth: 160 }}>
            <label className="form-label">Rol</label>
            <select className="form-select" value={filtroRol} onChange={(e) => setFiltroRol(e.target.value)} style={{ height: 38 }}>
              <option value="">Todos los roles</option>
              {roles.map((r) => <option key={r} value={r}>{r}</option>)}
            </select>
          </div>
          <div style={{ minWidth: 140 }}>
            <label className="form-label">Estado</label>
            <select className="form-select" value={filtroEstado} onChange={(e) => setFiltroEstado(e.target.value)} style={{ height: 38 }}>
              <option value="">Todos</option>
              <option value="activo">Activo</option>
              <option value="inactivo">Inactivo</option>
            </select>
          </div>
        </div>
      </div>

      <div className="table-container">
        <div className="table-responsive">
          <table>
            <thead>
              <tr>
                <th>Login</th>
                <th>Rol</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {filtrados.map((u, i) => (
                <tr key={u.id} style={{ animation: `fadeInUp 0.25s ease ${i * 0.025}s both`, transition: 'background 0.15s' }}
                  onMouseEnter={(e) => e.currentTarget.style.background = 'var(--bg-hover)'}
                  onMouseLeave={(e) => e.currentTarget.style.background = ''}
                >
                  <td style={{ fontWeight: 600 }}>{u.login}</td>
                  <td>{u.rol}</td>
                  <td>
                    <span className={`badge ${u.activo ? 'badge-success' : 'badge-outline'}`}>
                      {u.activo ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: 4 }}>
                      <button
                        onClick={() => abrirModal(u)}
                        className="btn btn-secondary btn-sm"
                        title="Editar"
                      >
                        <FiEdit2 size={13} />
                      </button>
                      {u.activo ? (
                        <button
                          onClick={() => abrirConfirmacion(u.id, 'desactivar')}
                          className="btn btn-warning btn-sm"
                          title="Desactivar"
                        >
                          <FiToggleLeft size={13} />
                        </button>
                      ) : (
                        <button
                          onClick={() => abrirConfirmacion(u.id, 'activar')}
                          className="btn btn-primary btn-sm"
                          title="Activar"
                        >
                          <FiToggleRight size={13} />
                        </button>
                      )}
                      <button
                        onClick={() => abrirConfirmacion(u.id, 'eliminar')}
                        className="btn btn-danger btn-sm"
                        title="Eliminar"
                      >
                        <FiTrash2 size={13} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {filtrados.length === 0 && (
                <tr><td colSpan={4} className="empty-state"><p>No hay usuarios que coincidan con los filtros</p></td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {modalAbierto && (
        <div className="modal-overlay" onClick={() => setModalAbierto(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 520 }}>
            <h2 style={{ marginBottom: 4 }}>{modoEdicion ? 'Editar Usuario' : 'Nuevo Usuario'}</h2>
            <p style={{ color: 'var(--text-muted)', margin: '0 0 1rem' }}>
              {modoEdicion ? 'Modifique los campos del usuario' : 'Complete los datos para crear un nuevo usuario'}
            </p>
            {errores.general && <p style={{ color: 'var(--danger)', margin: '0 0 0.75rem', background: 'var(--danger-light)', padding: '0.5rem 0.75rem', borderRadius: 6 }}>{errores.general}</p>}
            <form onSubmit={guardar}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 1rem' }}>
                <div>
                  <label className="form-label">Login</label>
                  <input name="login" value={form.login} onChange={handleChange} className={`form-input${errores.login ? ' input-error' : ''}`} style={{ height: 38 }} />
                  {errores.login && <span className="field-error">{errores.login}</span>}
                </div>
                <div>
                  <label className="form-label">Rol</label>
                  <select name="rol" value={form.rol} onChange={handleChange} className={`form-select${errores.rol ? ' input-error' : ''}`} style={{ height: 38 }}>
                    {roles.map(r => <option key={r} value={r}>{r}</option>)}
                  </select>
                  {errores.rol && <span className="field-error">{errores.rol}</span>}
                </div>
                <div style={{ gridColumn: '1 / -1' }}>
                  <label className="form-label">
                    UEB {form.rol === 'JefeUnidad' && <span style={{ color: 'var(--danger)' }}>*</span>}
                  </label>
                  <select name="ueb_id" value={form.ueb_id} onChange={handleChange} className={`form-select${errores.ueb_id ? ' input-error' : ''}`} style={{ height: 38 }}>
                    <option value="">{form.rol === 'JefeUnidad' ? '-- Seleccione una UEB --' : 'Sin UEB'}</option>
                    {uebs.map((u) => (
                      <option key={u.id} value={u.id}>{u.codigo} — {u.nombre}</option>
                    ))}
                  </select>
                  {errores.ueb_id && <span className="field-error">{errores.ueb_id}</span>}
                </div>
                {modoEdicion && (
                  <div style={{ gridColumn: '1 / -1', marginTop: 4 }}>
                    <label className="form-label" style={{ cursor: 'pointer', fontWeight: 400, display: 'flex', alignItems: 'center', gap: 6 }}>
                      <input type="checkbox" checked={resetPass} onChange={(e) => setResetPass(e.target.checked)} />
                      Resetear contraseña
                    </label>
                  </div>
                )}
                {(resetPass || !modoEdicion) && (
                  <div style={{ gridColumn: '1 / -1' }}>
                    <label className="form-label">Contraseña</label>
                    <input name="password" type="password" value={form.password} onChange={handleChange} className={`form-input${errores.password ? ' input-error' : ''}`} style={{ height: 38 }} />
                    {errores.password && <span className="field-error">{errores.password}</span>}
                  </div>
                )}
              </div>
              <div style={{ display: 'flex', gap: '0.6rem', justifyContent: 'flex-end', marginTop: '1.25rem' }}>
                <button type="button" onClick={() => setModalAbierto(false)} className="btn btn-secondary">Cancelar</button>
                <button type="submit" className="btn btn-primary">{modoEdicion ? 'Guardar' : 'Crear'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmModal
        isOpen={confirm.isOpen}
        title={confirmTitle()}
        message={confirmMessage()}
        onConfirm={ejecutarAccion}
        onCancel={() => setConfirm({ isOpen: false, id: null, action: '' })}
        confirmText={confirmLabel()}
        variant={confirmVariant()}
      />

      <ImportExcelModal
        isOpen={importModalOpen}
        onClose={() => setImportModalOpen(false)}
        title="Importar Usuarios desde Excel"
        endpoint="usuario"
        onSuccess={() => { setImportModalOpen(false); cargarDatos(); }}
      />
    </div>
  );
};

export default Usuarios;
