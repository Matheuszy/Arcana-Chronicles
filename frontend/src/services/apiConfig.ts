function resolveWsUrl(value: string): string {
  if (value.startsWith('ws://') || value.startsWith('wss://')) {
    return value;
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}${value}`;
}

export const API_BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';
export const WS_BASE_URL = resolveWsUrl(import.meta.env.VITE_WS_URL ?? 'ws://localhost:8080/ws');
