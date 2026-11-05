import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import Monitoreo from './Monitoreo';

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({ usuario: { rol: 'Administrador', login: 'admin' } }),
}));

vi.mock('../services/api', () => ({
  get: vi.fn(),
}));

vi.mock('sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}));

import { get } from '../services/api';

describe('Monitoreo', () => {
  beforeEach(() => {
    get.mockReset();
    get.mockResolvedValue([
      {
        equipo_id: 1,
        numero_linea: '53001001',
        ueb_id: 1,
        ueb: 'Nivel Central',
        consumo_actual: 5620.0,
        umbral: 1500.0,
        umbral_ueb: 20,
        porcentaje: 374.67,
        estado: 'rojo',
        anio: 2025,
        mes: 3,
      },
    ]);
  });

  it('renderiza el título Monitoreo', async () => {
    render(<Monitoreo />);
    expect(screen.getByRole('heading', { name: /monitoreo/i })).toBeInTheDocument();
  });

  it('llama al endpoint /monitoreo/estado con anio y mes', async () => {
    render(<Monitoreo />);
    await waitFor(() => {
      expect(get).toHaveBeenCalled();
      const url = get.mock.calls[0][0];
      expect(url).toMatch(/\/monitoreo\/estado\?anio=\d+&mes=\d+/);
    });
  });

  it('muestra los equipos devueltos por la API', async () => {
    render(<Monitoreo />);
    await waitFor(() => {
      expect(screen.getByText('53001001')).toBeInTheDocument();
      expect(screen.getByText('Nivel Central')).toBeInTheDocument();
      expect(screen.getByText('rojo')).toBeInTheDocument();
    });
  });
});