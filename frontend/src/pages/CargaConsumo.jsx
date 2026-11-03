import { useState, useEffect, useRef } from 'react';
import { toast } from 'sonner';
import api, { downloadBlob } from '../services/api';
import { FiUpload, FiDownload, FiFileText, FiX } from 'react-icons/fi';

const meses = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];

const CargaConsumo = () => {
  const [mes, setMes] = useState(new Date().getMonth());
  const [anio, setAnio] = useState(new Date().getFullYear());
  const [aniosDisponibles, setAniosDisponibles] = useState([]);
  const [archivo, setArchivo] = useState(null);
  const [loading, setLoading] = useState(false);
  const fileRef = useRef(null);

  useEffect(() => {
    api.get('/dashboard/anios-disponibles').then(({ data }) => {
      setAniosDisponibles(data.anios || []);
    }).catch(() => {
      setAniosDisponibles([new Date().getFullYear() - 1, new Date().getFullYear()]);
    });
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!archivo) { toast.warning('Seleccione un archivo Excel'); return; }
    const formData = new FormData();
    formData.append('archivo', archivo);
    formData.append('mes', mes + 1);
    formData.append('anio', anio);
    setLoading(true);
    try {
      const { data } = await api.post('/carga', formData);
      if (data.procesados > 0 && (!data.errores || data.errores.length === 0)) {
        toast.success(`${data.procesados} registros procesados correctamente`);
      } else if (data.procesados > 0) {
        toast.warning(`${data.procesados} registro(s) procesado(s) con ${(data.errores || []).length} error(es)`);
      }
      if (data.errores?.length > 0) {
        data.errores.forEach(err => toast.error(`Fila ${err.fila}: ${err.error}`, { duration: 5000 }));
      }
    } catch (err) {
      console.error('Error al cargar consumo:', err);
      toast.error(err.response?.data?.error || 'Error al procesar archivo');
    } finally { setLoading(false); setArchivo(null); if (fileRef.current) fileRef.current.value = ''; }
  };

  const descargarPlantilla = () => {
    downloadBlob('/carga/plantilla', 'plantilla_carga_consumo.xlsx');
  };

  return (
    <div style={{ animation: 'fadeInUp 0.35s ease' }}>
      <div className="page-header" style={{ marginBottom: '0.75rem' }}>
        <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <FiUpload size={18} style={{ color: 'var(--primary)' }} /> Importar Consumo Mensual
        </h1>
      </div>

      <div className="card" style={{ maxWidth: 760 }}>
        <form onSubmit={handleSubmit}>
          <div style={{ display: 'flex', gap: '0.6rem', alignItems: 'center', flexWrap: 'wrap' }}>
            <select value={mes} onChange={(e) => setMes(parseInt(e.target.value))} className="form-select" style={{ width: 140 }}>
              {meses.map((m, i) => <option key={i} value={i}>{m}</option>)}
            </select>
            <select value={anio} onChange={(e) => setAnio(parseInt(e.target.value))} className="form-select" style={{ width: 110 }}>
              {aniosDisponibles.map(a => <option key={a} value={a}>{a}</option>)}
            </select>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', flex: 1, minWidth: 240 }}>
              {!archivo ? (
                <button type="button" onClick={() => fileRef.current?.click()} className="btn btn-secondary" style={{ whiteSpace: 'nowrap' }}
                  onMouseEnter={(e) => { e.currentTarget.style.background = 'var(--primary-light)'; e.currentTarget.style.borderColor = 'var(--primary)'; e.currentTarget.style.color = 'var(--primary)'; }}
                  onMouseLeave={(e) => { e.currentTarget.style.background = ''; e.currentTarget.style.borderColor = ''; e.currentTarget.style.color = ''; }}
                >
                  <FiFileText size={14} /> Seleccionar archivo
                </button>
              ) : (
                <button type="button" onClick={() => fileRef.current?.click()} className="btn btn-secondary" style={{ whiteSpace: 'nowrap', display: 'flex', alignItems: 'center', gap: 6 }}
                  onMouseEnter={(e) => { e.currentTarget.style.background = 'var(--primary-light)'; e.currentTarget.style.borderColor = 'var(--primary)'; e.currentTarget.style.color = 'var(--primary)'; }}
                  onMouseLeave={(e) => { e.currentTarget.style.background = ''; e.currentTarget.style.borderColor = ''; e.currentTarget.style.color = ''; }}
                >
                  <FiFileText size={14} />
                  <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: 140 }}>{archivo.name}</span>
                  <span style={{ fontSize: 12, opacity: 0.6 }}>({(archivo.size / 1024).toFixed(0)} KB)</span>
                  <span onClick={(e) => { e.stopPropagation(); setArchivo(null); if (fileRef.current) fileRef.current.value = ''; }} style={{ marginLeft: 4, opacity: 0.5 }}><FiX size={13} /></span>
                </button>
              )}
              <input ref={fileRef} type="file" accept=".xlsx,.xls" onChange={(e) => { setArchivo(e.target.files[0]); }} style={{ display: 'none' }} />
              <button type="button" onClick={descargarPlantilla} className="btn btn-secondary" style={{ whiteSpace: 'nowrap' }}>
                <FiDownload size={14} /> Plantilla
              </button>
              <button type="submit" disabled={loading || !archivo} className="btn btn-primary" style={{ whiteSpace: 'nowrap' }}>
                {loading ? (
                  <span style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
                    <span className="spinner" style={{ width: 13, height: 13, borderWidth: 2 }} /> Cargando
                  </span>
                ) : (
                  <><FiUpload size={14} /> Cargar</>
                )}
              </button>
            </div>
          </div>
        </form>

        <div style={{ marginTop: '0.5rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
          Formatos permitidos: .xlsx, .xls
        </div>
      </div>
    </div>
  );
};

export default CargaConsumo;
