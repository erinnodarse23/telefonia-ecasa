import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import Presupuesto from './Presupuesto';

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({ usuario: { rol: 'Administrador', login: 'admin' } }),
}));

vi.mock('../services/api', () => ({
  get: vi.fn(),
  post: vi.fn(),
}));

vi.mock('sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}));

import { get, post } from '../services/api';

describe('Presupuesto', () => {
  beforeEach(() => {
    get.mockReset();
    post.mockReset();
    get.mockResolvedValue([
      {
        ueb_id: 1,
        codigo: 'NC',
        ueb: 'Nivel Central',
        anio: 2025,
        asignado: 62400.0,
        ejecutado: 57102.0,
        disponible: 5298.0,
        porcentaje_ejecucion: 91.51,
        estado: 'amarillo',
        umbrales: [70, 85, 100],
      },
    ]);
  });

  it('renderiza el título Presupuesto Anual', async () => {
    render(<Presupuesto />);
    expect(screen.getByRole('heading', { name: /presupuesto anual/i })).toBeInTheDocument();
  });

  it('llama al endpoint /presupuesto con anio', async () => {
    render(<Presupuesto />);
    await waitFor(() => {
      expect(get).toHaveBeenCalledWith(expect.stringMatching(/^\/presupuesto\?anio=\d+/));
    });
  });

  it('muestra las UEBs devueltas por la API', async () => {
    render(<Presupuesto />);
    await waitFor(() => {
      expect(screen.getByText(/NC - Nivel Central/)).toBeInTheDocument();
    });
  });
});