import '@testing-library/jest-dom';
import { vi } from 'vitest';

// Recharts necesita ResizeObserver, que jsdom no implementa
global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
};

// Silenciar warnings de React Router / sonner en tests
global.window.matchMedia = global.window.matchMedia || function () {
  return { matches: false, addListener() {}, removeListener() {}, addEventListener() {}, removeEventListener() {} };
};