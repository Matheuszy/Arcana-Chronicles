import React, { createContext, useContext, useState } from 'react';

export type UserRole = 'MESTRE' | 'JOGADOR' | null;

const STORAGE_KEY = 'arcana:session';

interface StoredSession {
  role: UserRole;
  displayName: string;
}

interface SessionContextValue {
  role: UserRole;
  displayName: string;
  chooseRole: (role: UserRole, displayName: string) => void;
  clear: () => void;
}

function loadInitial(): StoredSession {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return { role: null, displayName: '' };
  try {
    return JSON.parse(raw);
  } catch {
    return { role: null, displayName: '' };
  }
}

const SessionContext = createContext<SessionContextValue | undefined>(undefined);

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<StoredSession>(loadInitial);

  const chooseRole = (role: UserRole, displayName: string) => {
    const next = { role, displayName };
    setState(next);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  };

  const clear = () => {
    setState({ role: null, displayName: '' });
    localStorage.removeItem(STORAGE_KEY);
  };

  return (
    <SessionContext.Provider value={{ ...state, chooseRole, clear }}>
      {children}
    </SessionContext.Provider>
  );
}

export function useSession(): SessionContextValue {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error('useSession precisa estar dentro de <SessionProvider>');
  return ctx;
}
