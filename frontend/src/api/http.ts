import axios from 'axios';

const API_PREFIX = '/api';
const DEFAULT_API_BASE_URL = API_PREFIX;

const trimTrailingSlashes = (value: string): string => value.replace(/\/+$/, '');

const normalizeApiBaseUrl = (value?: string): string => {
  const raw = value?.trim();
  if (!raw) return DEFAULT_API_BASE_URL;

  const normalized = trimTrailingSlashes(raw);
  if (!normalized) return DEFAULT_API_BASE_URL;

  return normalized.endsWith(API_PREFIX) ? normalized : `${normalized}${API_PREFIX}`;
};

const normalizeRequestUrl = (baseURL: string | undefined, url: string | undefined): string | undefined => {
  if (!url || /^https?:\/\//i.test(url)) return url;

  const normalizedBase = baseURL ? trimTrailingSlashes(baseURL) : '';
  if (!normalizedBase.endsWith(API_PREFIX)) return url;

  return url.startsWith(`${API_PREFIX}/`) ? url.slice(API_PREFIX.length) : url;
};

export const apiBaseUrl = normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL);

export const http = axios.create({
  baseURL: apiBaseUrl,
  timeout: 15000,
});

http.interceptors.request.use((config) => {
  config.url = normalizeRequestUrl(config.baseURL ?? apiBaseUrl, config.url);

  const token = localStorage.getItem('authToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;

  const id = localStorage.getItem('devUserId');
  const allowDevFallback = import.meta.env.VITE_ALLOW_DEV_X_USER_ID === 'true';
  if (allowDevFallback && id) config.headers['X-User-Id'] = id;

  return config;
});

export const getApiErrorMessage = (error: unknown): string => {
  if (!axios.isAxiosError(error)) return 'Error inesperado.';
  if (error.code === 'ECONNABORTED') return 'La solicitud tardó demasiado. Intentá nuevamente.';
  if (!error.response) return 'No se pudo conectar con el backend. Verificá si está corriendo y si CORS está configurado.';

  const status = error.response.status;
  const data = error.response.data as { message?: string; error?: string } | undefined;
  const backendMessage = data?.message ?? data?.error;
  if (backendMessage) return backendMessage;
  if (status === 401) return 'Sesión inválida o expirada. Iniciá sesión nuevamente.';
  if (status === 403) return 'No tenés permisos para acceder a este recurso.';
  if (status === 404) return 'Recurso no encontrado (404).';
  if (status === 400) return 'Solicitud inválida (400). Revisá los campos.';
  if (status === 500) return 'Error interno del servidor (500).';
  return `Error HTTP ${status}.`;
};

export const getApiErrorCode = (error: unknown): string | null => {
  if (!axios.isAxiosError(error)) return null;

  const data = error.response?.data as { code?: string } | undefined;
  return data?.code ?? null;
};

const domainErrorMessages: Record<string, string> = {
  TRANSACTION_EXACT_DUPLICATE:
    'Ya cargaste este movimiento. Abrí el parecido o guardalo como nuevo solo si estás seguro.',
  TRANSACTION_SOURCE_DUPLICATE:
    'Ese movimiento ya vino de una importación anterior. No lo volvemos a contar.',
  TRANSACTION_POSSIBLE_INTERNAL_TRANSFER:
    'Esto parece plata movida entre tus cuentas. Conviene vincularlo para que no infle ingresos o gastos.',
  CATEGORY_REQUIRED:
    'Elegí una categoría o dejalo pendiente para revisar después.',
  ACCOUNT_NOT_FOUND:
    'La cuenta elegida no existe en este perfil.',
  CATEGORY_INCOMPATIBLE:
    'La categoría no coincide con el tipo de movimiento. Cambiala antes de guardar.',
};

export const getDomainErrorMessage = (error: unknown): string => {
  const code = getApiErrorCode(error);
  if (code && domainErrorMessages[code]) return domainErrorMessages[code];

  return getApiErrorMessage(error);
};
