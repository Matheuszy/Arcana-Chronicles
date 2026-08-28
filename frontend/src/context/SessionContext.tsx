import React, { createContext, useContext, useState } from 'react';

export type UserRole = 'MESTRE' | 'JOGADOR' | null;

const STORAGE_KEY = 'arcana:session';

interface StoredSession {
  role: UserRole;
  displayName: string;
  userId: number | null;
  token: string | null;
}

interface SessionContextValue {
  role: UserRole;
  displayName: string;
  userId: number | null;
  token: string | null;
  chooseRole: (role: UserRole, displayName: string, userId?: number | null, token?: string | null) => void;
  clear: () => void;
}

function loadInitial(): StoredSession {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return { role: null, displayName: '', userId: null, token: null };
  try {
    const parsed = JSON.parse(raw);
    return {
      role: parsed.role ?? null,
      displayName: parsed.displayName ?? '',
      userId: parsed.userId ?? null,
      token: parsed.token ?? null,
    };
  } catch {
    return { role: null, displayName: '', userId: null, token: null };
  }
}

const SessionContext = createContext<SessionContextValue | undefined>(undefined);

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<StoredSession>(loadInitial);

  const chooseRole = (role: UserRole, displayName: string, userId?: number | null, token?: string | null) => {
    const next: StoredSession = {
      role,
      displayName,
      userId: userId !== undefined ? userId : state.userId,
      token: token !== undefined ? token : state.token,
    };
    setState(next);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  };

  const clear = () => {
    setState({ role: null, displayName: '', userId: null, token: null });
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
