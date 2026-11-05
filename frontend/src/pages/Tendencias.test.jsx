import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import Tendencias from './Tendencias';

vi.mock('../contexts/ThemeContext', () => ({
  useTheme: () => ({ theme: 'light' }),
}));

vi.mock('../services/api', () => ({
  get: vi.fn(),
}));

vi.mock('sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}));

// Recharts falla en jsdom incluso con ResizeObserver: mockeamos el módulo entero
vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }) => <div data-testid="recharts-container">{children}</div>,
  AreaChart: ({ children }) => <div data-testid="area-chart">{children}</div>,
  Area: () => null,
  BarChart: ({ children }) => <div data-testid="bar-chart">{children}</div>,
  Bar: () => null,
  XAxis: () => null,
  YAxis: () => null,
  CartesianGrid: () => null,
  Tooltip: () => null,
  Legend: () => null,
}));

import { get } from '../services/api';

const mockData = {
  anio: 2025,
  consumos_mensuales: Array.from({ length: 12 }, (_, i) => ({
    mes: i + 1,
    total_voz: 100,
    total_datos: 200,
    total_sms: 10,
    total: 310,
  })),
  ranking_top: [
    { equipo_id: 1, numero_linea: '53001001', ueb_id: 1, ueb: 'NC', total_voz: 100, total_datos: 500, total_sms: 10, total: 610 },
  ],
  ranking_bottom: [
    { equipo_id: 2, numero_linea: '53001002', ueb_id: null, ueb: null, total_voz: 5, total_datos: 0, total_sms: 0, total: 5 },
  ],
  comparativa_anual: {
    anio_actual: 2025,
    anio_anterior: 2024,
    actual: Array.from({ length: 12 }, (_, i) => ({ mes: i + 1, total_voz: 100, total_datos: 200, total_sms: 10, total: 310 })),
    anterior: Array.from({ length: 12 }, (_, i) => ({ mes: i + 1, total_voz: 50, total_datos: 100, total_sms: 5, total: 155 })),
  },
};

describe('Tendencias', () => {
  beforeEach(() => {
    get.mockReset();
    get.mockResolvedValue(mockData);
  });

  it('renderiza el título Tendencias', async () => {
    render(<Tendencias />);
    expect(screen.getByRole('heading', { name: /tendencias/i })).toBeInTheDocument();
  });

  it('llama al endpoint /reportes/tendencias con anio', async () => {
    render(<Tendencias />);
    await waitFor(() => {
      expect(get).toHaveBeenCalledWith(expect.stringMatching(/^\/reportes\/tendencias\?anio=\d+/));
    });
  });

  it('muestra los rankings top y bottom', async () => {
    render(<Tendencias />);
    await waitFor(() => {
      expect(screen.getByText('53001001')).toBeInTheDocument();
      expect(screen.getByText('53001002')).toBeInTheDocument();
    });
  });

  it('dibuja los gráficos (AreaChart y BarChart)', async () => {
    render(<Tendencias />);
    await waitFor(() => {
      expect(screen.getByTestId('area-chart')).toBeInTheDocument();
      expect(screen.getByTestId('bar-chart')).toBeInTheDocument();
    });
  });
});