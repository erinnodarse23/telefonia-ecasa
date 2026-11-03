import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:5001/api',
});

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  config.__authToken = token;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const url = error.config?.url || '';
      const tokenActual = sessionStorage.getItem('token');
      const solicitudUsabaTokenActual = error.config?.__authToken === tokenActual;
      if (solicitudUsabaTokenActual && !url.includes('/login') && !url.includes('/auth/')) {
        window.dispatchEvent(new CustomEvent('auth:unauthorized'));
      }
    }
    return Promise.reject(error);
  }
);

export const login = async (login, password, rememberMe = false, captchaId = null, captchaAnswer = null) => {
  const body = { login, password, rememberMe };
  if (captchaId) body.captchaId = captchaId;
  if (captchaAnswer !== null) body.captchaAnswer = captchaAnswer;
  const { data } = await api.post('/usuarios/login', body);
  return data;
};

export const fetchCaptcha = async () => {
  const { data } = await api.get('/usuarios/captcha');
  return data;
};

export const get = async (endpoint) => {
  const { data } = await api.get(endpoint);
  return data;
};

export const post = async (endpoint, data) => {
  const { data: result } = await api.post(endpoint, data);
  return result;
};

export const put = async (endpoint, data) => {
  const { data: result } = await api.put(endpoint, data);
  return result;
};

export const del = async (endpoint) => {
  const { data } = await api.delete(endpoint);
  return data;
};

export const downloadBlob = async (url, filename) => {
  const response = await api.get(url, { responseType: 'blob' });
  const blobUrl = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = blobUrl;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(blobUrl);
};

export const verifyToken = async () => {
  const { data } = await api.get('/auth/verify');
  return data;
};

export default api;