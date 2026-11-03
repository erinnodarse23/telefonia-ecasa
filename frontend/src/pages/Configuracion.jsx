import { useState, useEffect } from 'react';
import { FiSave, FiRefreshCw, FiCalendar } from 'react-icons/fi';
import api from '../services/api';
import NumberInputWithButtons from '../components/NumberInputWithButtons';

const Configuracion = () => {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState(null);
  const [umbralGlobal, setUmbralGlobal] = useState(20);
  const [guardandoUmbral, setGuardandoUmbral] = useState(false);
  const [aniosDisponibles, setAniosDisponibles] = useState([]);
  const [anioSeleccionado, setAnioSeleccionado] = useState(null);
  const [dirty, setDirty] = useState(false);

  const anioActual = new Date().getFullYear();

  const showToast = (type, text) => {
    setToast({ type, text });
    setTimeout(() => setToast(null), 4000);
  };

  const cargarTodo = async (anio) => {
    setLoading(true);
    try {
      const [uebsRes, planesRes, umbralesRes, umbralGlobalRes] = await Promise.all([
        api.get('/ueb'),
        api.get(`/configuracion/planes?anio=${anio}`).then(r => r.data).catch(() => []),
        api.get('/configuracion/umbrales').then(r => r.data).catch(() => []),
        api.get('/configuracion/umbral').then(r => r.data).catch(() => null),
      ]);

      const uebs = Array.isArray(uebsRes.data)
        ? uebsRes.data.filter((u) => u.activo !== false)
        : [];

      const mapPlanes = {};
      for (const p of Array.isArray(planesRes) ? planesRes : []) {
        mapPlanes[p.ueb_id] = p.monto;
      }

      const mapUmbrales = {};
      for (const u of Array.isArray(umbralesRes) ? umbralesRes : []) {
        mapUmbrales[u.ueb_id] = u.porcentaje;
      }

      setRows(uebs.map((u) => ({
        ueb_id: u.id,
        ueb_nombre: u.nombre,
        plan: mapPlanes[u.id] ?? '',
        umbral: mapUmbrales[u.id] ?? '',
      })));

      if (umbralGlobalRes?.porcentaje_sobregasto !== undefined) {
        setUmbralGlobal(umbralGlobalRes.porcentaje_sobregasto);
      }

      setDirty(false);
    } catch (err) {
      showToast('error', `Error al cargar: ${err.response?.data?.error || err.message}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const inicializar = async () => {
      setLoading(true);
      try {
        const aniosRes = await api.get('/dashboard/anios-disponibles').then(r => r.data).catch(() => []);
        const anios = Array.isArray(aniosRes) && aniosRes.length > 0
          ? aniosRes
          : [anioActual - 1, anioActual, anioActual + 1];
        setAniosDisponibles(anios);

        const anioInicial = anios.includes(anioActual) ? anioActual : anios[0];
        setAnioSeleccionado(anioInicial);

        await cargarTodo(anioInicial);
      } catch (err) {
        showToast('error', `Error al cargar: ${err.message}`);
        setLoading(false);
      }
    };

    inicializar();
  }, []);

  useEffect(() => {
    if (anioSeleccionado === null) return;
    cargarTodo(anioSeleccionado);
  }, [anioSeleccionado]);

  const handleChange = (uebId, field, value) => {
    setRows((prev) => prev.map((r) =>
      r.ueb_id === uebId ? { ...r, [field]: value } : r
    ));
    setDirty(true);
  };

  const guardarCambios = async () => {
    setSaving(true);
    try {
      const ops = [];
      const planesBody = [];
      const umbralesBody = [];

      for (const r of rows) {
        if (r.plan !== '' && r.plan !== null && r.plan !== undefined) {
          planesBody.push({ uebId: r.ueb_id, anio: anioSeleccionado, monto: Number(r.plan) });
        }
        umbralesBody.push({ uebId: r.ueb_id, porcentaje: r.umbral !== '' && r.umbral !== null && r.umbral !== undefined ? Number(r.umbral) : null });
      }

      ops.push(Promise.all(planesBody.map((p) => api.post('/configuracion/planes', p))));
      ops.push(api.post('/configuracion/umbrales', { umbrales: umbralesBody }));

      await Promise.all(ops);
      setDirty(false);
      showToast('success', 'Planes y umbrales guardados correctamente');
    } catch (err) {
      showToast('error', `Error al guardar: ${err.response?.data?.error || err.message}`);
    } finally {
      setSaving(false);
    }
  };

  const guardarUmbralGlobal = async () => {
    setGuardandoUmbral(true);
    try {
      await api.post('/configuracion/umbral', { valor: { porcentaje_sobregasto: Number(umbralGlobal) } });
      showToast('success', 'Umbral global guardado correctamente');
    } catch (err) {
      showToast('error', `Error al guardar umbral global: ${err.response?.data?.error || err.message}`);
    } finally {
      setGuardandoUmbral(false);
    }
  };

  const limpiarCampos = () => {
    setRows((prev) => prev.map((r) => ({ ...r, plan: '', umbral: '' })));
    setDirty(true);
  };

  const hayValores = rows.some((r) => r.plan !== '' && r.plan !== null && r.plan !== undefined);

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', animation: 'fadeInUp 0.35s ease' }}>
      <h1 style={{ margin: '0 0 6px', fontSize: '1.2rem', fontWeight: 800, color: 'var(--text-primary)' }}>Parámetros del Sistema</h1>
      <p style={{ margin: '0 0 ' + (toast ? '12px' : '1.5rem'), color: 'var(--text-muted)', lineHeight: 1.5 }}>
        Configure el presupuesto anual y el umbral de sobregasto de cada UEB.
      </p>

      {toast && (
        <div style={{
          padding: '0.5rem 0.75rem', borderRadius: 6, marginBottom: 16,
          background: toast.type === 'error' ? 'rgba(239,68,68,0.08)' : 'rgba(16,185,129,0.08)',
          color: 'var(--text-primary)',
          borderLeft: `3px solid ${toast.type === 'error' ? 'var(--danger)' : 'var(--success)'}`,
        }}>
          {toast.text}
        </div>
      )}

      <div className="card" style={{ marginBottom: '1rem' }}>
        <div style={{ display: 'flex', gap: '0.6rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <FiCalendar size={16} style={{ color: 'var(--azul-claro)' }} />
          <span style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Año:</span>
          <select
            value={anioSeleccionado ?? ''}
            onChange={(e) => setAnioSeleccionado(Number(e.target.value))}
            className="form-select"
            style={{ width: 110 }}
          >
            {aniosDisponibles.map((a) => (
              <option key={a} value={a}>{a}</option>
            ))}
          </select>
          <span style={{ color: 'var(--text-muted)' }}>
            Los valores del plan afectan los cálculos del Dashboard.
          </span>
        </div>
      </div>

      <div className="card" style={{ marginBottom: '1rem' }}>
        <div style={{ display: 'flex', gap: '0.6rem', marginBottom: 16, flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h2 style={{ margin: 0, fontSize: '1rem', fontWeight: 700, color: 'var(--text-primary)' }}>Presupuesto y Umbral por UEB</h2>
            <p style={{ margin: '2px 0 0', color: 'var(--text-muted)' }}>
              Defina el plan anual y el porcentaje de sobregasto permitido para cada UEB.
            </p>
          </div>
          <div style={{ display: 'flex', gap: '0.6rem' }}>
            <button onClick={limpiarCampos} disabled={loading || !hayValores} className="btn btn-secondary" style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
              <FiRefreshCw size={13} /> Limpiar
            </button>
            <button onClick={guardarCambios} disabled={saving || loading || !dirty || !anioSeleccionado} className="btn btn-primary" style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
              {saving ? <span className="spinner-sm" /> : <FiSave size={14} />}
              {saving ? 'Guardando...' : 'Guardar Cambios'}
            </button>
          </div>
        </div>

        {loading && rows.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>
            <div className="spinner" style={{ margin: '0 auto 12px' }} />
            Cargando...
          </div>
        ) : (
          <div className="table-responsive">
            <table>
              <thead>
                <tr>
                  <th>UEB</th>
                  <th>Plan {anioSeleccionado} ($)</th>
                  <th>Umbral (%)</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.ueb_id} style={{ transition: 'background 0.15s' }}
                    onMouseEnter={(e) => e.currentTarget.style.background = 'var(--bg-hover)'}
                    onMouseLeave={(e) => e.currentTarget.style.background = ''}
                  >
                    <td style={{ fontWeight: 500 }}>{r.ueb_nombre}</td>
                    <td>
                      <NumberInputWithButtons
                        value={r.plan}
                        onChange={(v) => handleChange(r.ueb_id, 'plan', v)}
                        step={1000} min={0}
                      />
                    </td>
                    <td>
                      <NumberInputWithButtons
                        value={r.umbral}
                        onChange={(v) => handleChange(r.ueb_id, 'umbral', v)}
                        step={1} min={0} max={100}
                      />
                    </td>
                  </tr>
                ))}
                {rows.length === 0 && (
                  <tr><td colSpan={3} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '0.75rem' }}>No hay UEB activas</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="card">
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8, marginBottom: 12 }}>
          <h2 style={{ margin: 0, fontSize: '1rem', fontWeight: 700, color: 'var(--text-primary)' }}>Umbral Global</h2>
        </div>
        <p style={{ margin: '0 0 12px', color: 'var(--text-muted)', lineHeight: 1.5 }}>
          Porcentaje de sobregasto general que se aplica a las UEB que no tienen un umbral personalizado definido arriba.
        </p>
        <div style={{ display: 'flex', gap: '0.6rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <NumberInputWithButtons value={umbralGlobal} onChange={(v) => setUmbralGlobal(v)} step={1} min={0} max={100} />
          <span style={{ color: 'var(--text-secondary)' }}>%</span>
          <button onClick={guardarUmbralGlobal} disabled={guardandoUmbral} className="btn btn-primary" style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
            {guardandoUmbral ? <span className="spinner-sm" /> : <FiSave size={14} />}
            Guardar
          </button>
        </div>
      </div>
    </div>
  );
};

export default Configuracion;
