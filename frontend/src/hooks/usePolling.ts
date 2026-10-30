import { useEffect, useRef } from 'react';

export const POLLING_INTERVAL_MS = 2000;

export function usePolling(callback: () => void, enabled = true, intervalMs = POLLING_INTERVAL_MS) {
  const latestCallback = useRef(callback);
  latestCallback.current = callback;

  useEffect(() => {
    if (!enabled) return;

    let timer: number | undefined;

    const start = () => {
      if (timer === undefined) timer = window.setInterval(() => latestCallback.current(), intervalMs);
    };
    const stop = () => {
      if (timer !== undefined) {
        window.clearInterval(timer);
        timer = undefined;
      }
    };
    const onVisibilityChange = () => {
      if (document.hidden) {
        stop();
        return;
      }
      latestCallback.current();
      start();
    };

    if (!document.hidden) start();
    document.addEventListener('visibilitychange', onVisibilityChange);
    return () => {
      stop();
      document.removeEventListener('visibilitychange', onVisibilityChange);
    };
  }, [enabled, intervalMs]);
}
