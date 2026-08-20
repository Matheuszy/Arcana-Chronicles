import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import { SessionProvider, useSession } from './context/SessionContext';
import Login from './pages/Login/Login';
import CharacterList from './pages/CharacterList/CharacterList';
import CharacterForm from './pages/CharacterForm/CharacterForm';
import TableList from './pages/TableList/TableList';
import TableRoom from './pages/TableRoom/TableRoom';
import './App.css';

/** Redireciona para /login se não houver sessão ativa */
function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { userId } = useSession();
  return userId ? <>{children}</> : <Navigate to="/login" replace />;
}

function Shell() {
  const { role, displayName, userId, clear } = useSession();

  return (
    <div className="shell">
      <header className="topbar">
        <NavLink to="/" className="brand">
          <span className="brand-glyph">✦</span>
          <span className="brand-name">ARCANA</span>
        </NavLink>

        {userId && (
          <>
            <nav className="nav">
              <NavLink to="/fichas" className={({ isActive }) => (isActive ? 'active' : '')}>
                Fichas
              </NavLink>
              <NavLink to="/mesas" className={({ isActive }) => (isActive ? 'active' : '')}>
                Mesas
              </NavLink>
            </nav>

            <div className="who">
              <span className={`who-role ${role === 'MESTRE' ? 'master' : ''}`}>
                {role === 'MESTRE' ? 'Mestre' : 'Jogador'}
              </span>
              <span className="who-name">{displayName}</span>
              <button
                className="btn btn-ghost who-logout"
                onClick={() => { clear(); }}
                title="Sair"
              >
                Sair
              </button>
            </div>
          </>
        )}
      </header>

      <main className="stage">
        <Routes>
          <Route path="/login" element={<Login />} />

          <Route path="/" element={
            <PrivateRoute><Navigate to="/mesas" replace /></PrivateRoute>
          } />
          <Route path="/fichas" element={
            <PrivateRoute><CharacterList /></PrivateRoute>
          } />
          <Route path="/fichas/nova" element={
            <PrivateRoute><CharacterForm /></PrivateRoute>
          } />
          <Route path="/fichas/:id/editar" element={
            <PrivateRoute><CharacterForm /></PrivateRoute>
          } />
          <Route path="/mesas" element={
            <PrivateRoute><TableList /></PrivateRoute>
          } />
          <Route path="/mesas/:id" element={
            <PrivateRoute><TableRoom /></PrivateRoute>
          } />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </main>
    </div>
  );
}

export default function App() {
  return (
    <SessionProvider>
      <Shell />
    </SessionProvider>
  );
}
