import React, { createContext, useContext, useState } from 'react';

export type UserRole = 'MESTRE' | 'JOGADOR' | null;

const STORAGE_KEY = 'arcana:session';

interface StoredSession {
  role: UserRole;
  displayName: string;
  userId: number | null;
}

interface SessionContextValue {
  role: UserRole;
  displayName: string;
  userId: number | null;
  chooseRole: (role: UserRole, displayName: string, userId: number) => void;
  clear: () => void;
}

function loadInitial(): StoredSession {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return { role: null, displayName: '', userId: null };
  try {
    return JSON.parse(raw);
  } catch {
    return { role: null, displayName: '', userId: null };
  }
}

const SessionContext = createContext<SessionContextValue | undefined>(undefined);

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<StoredSession>(loadInitial);

  const chooseRole = (role: UserRole, displayName: string, userId: number) => {
    const next = { role, displayName, userId };
    setState(next);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  };

  const clear = () => {
    setState({ role: null, displayName: '', userId: null });
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
