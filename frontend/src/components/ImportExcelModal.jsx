import { useState } from 'react';
import { FiDownload, FiUpload, FiX, FiCheckCircle, FiAlertCircle } from 'react-icons/fi';
import api, { downloadBlob } from '../services/api';

const ImportExcelModal = ({ isOpen, onClose, title, endpoint, onSuccess }) => {
  const [archivo, setArchivo] = useState(null);
  const [loading, setLoading] = useState(false);
  const [resultado, setResultado] = useState(null);
  const [errores, setErrores] = useState([]);

  if (!isOpen) return null;

  const descargarPlantilla = () => {
    downloadBlob(`/importacion/plantilla/${endpoint}`, `plantilla_${endpoint}.xlsx`);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!archivo) return;

    const formData = new FormData();
    formData.append('archivo', archivo);

    setLoading(true);
    setResultado(null);
    setErrores([]);

    try {
      const { data } = await api.post(`/importacion/cargar/${endpoint}`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setResultado(data);
      setErrores(data.errores || []);
      if (!data.errores || data.errores.length === 0) {
        onSuccess?.();
      }
    } catch (err) {
      console.error('[ImportExcelModal] Error al importar:', err);
      const errData = err.response?.data;
      if (errData?.errores) {
        setErrores(errData.errores);
        setResultado(errData);
      } else {
        setErrores([{ fila: '-', error: errData?.error || 'Error al procesar archivo' }]);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" style={{ maxWidth: 520 }} onClick={(e) => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
          <h2 style={{ margin: 0, fontSize: '1.125rem', fontWeight: 700 }}>{title}</h2>
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: 4 }}>
            <FiX size={18} />
          </button>
        </div>

        <p style={{ margin: '0 0 1rem', fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
          Descargue la plantilla, complétela con los datos y luego súbala para importar.
        </p>

        <button onClick={descargarPlantilla} className="btn btn-secondary" style={{ width: '100%', marginBottom: '1rem' }}>
          <FiDownload size={15} /> Descargar plantilla Excel
        </button>

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '1rem' }}>
            <label className="form-label">Archivo Excel</label>
            <input
              type="file"
              accept=".xlsx,.xls"
              onChange={(e) => { setArchivo(e.target.files[0]); setResultado(null); setErrores([]); }}
              className="form-input"
              style={{ padding: '0.5rem' }}
            />
          </div>

          <button type="submit" disabled={!archivo || loading} className="btn btn-primary" style={{ width: '100%' }}>
            {loading ? 'Importando...' : <><FiUpload size={15} /> Importar datos</>}
          </button>
        </form>

        {resultado && (
          <div style={{
            marginTop: '1rem', padding: '0.75rem 1rem', borderRadius: 8,
            border: `1px solid ${resultado.errores?.length > 0 ? 'var(--warning)' : 'var(--success)'}`,
            background: resultado.errores?.length > 0 ? 'var(--warning-light)' : 'var(--success-light)',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontWeight: 600, fontSize: 14, color: resultado.errores?.length > 0 ? 'var(--warning)' : 'var(--success)' }}>
              {resultado.errores?.length > 0 ? <FiAlertCircle size={16} /> : <FiCheckCircle size={16} />}
              {resultado.mensaje}
            </div>
          </div>
        )}

        {errores.length > 0 && (
          <div style={{ marginTop: '0.75rem', maxHeight: 200, overflowY: 'auto' }}>
            <table style={{ width: '100%', fontSize: 13, borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border)' }}>
                  <th style={{ padding: '0.4rem 0.5rem', textAlign: 'left', color: 'var(--text-muted)', fontWeight: 600, width: 60 }}>Fila</th>
                  <th style={{ padding: '0.4rem 0.5rem', textAlign: 'left', color: 'var(--text-muted)', fontWeight: 600 }}>Error</th>
                </tr>
              </thead>
              <tbody>
                {errores.map((err, i) => (
                  <tr key={i} style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '0.4rem 0.5rem', color: 'var(--text-secondary)' }}>{err.fila}</td>
                    <td style={{ padding: '0.4rem 0.5rem', color: 'var(--danger)' }}>{err.error}</td>
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

export default ImportExcelModal;
