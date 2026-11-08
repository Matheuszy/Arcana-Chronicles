import { NavLink, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { SessionProvider, useSession } from './context/SessionContext';
import { UserChannelProvider } from './context/UserChannelContext';
import TableInvites from './components/TableInvites';
import Scenery, { sceneryForPath } from './components/Scenery';
import { D20Icon, HelmIcon, PosterIcon, QuillIcon } from './components/ui/Icons';
import ReleaseNotesGate from './components/releaseNotes/ReleaseNotesGate';
import Login from './pages/Login/Login';
import CharacterList from './pages/CharacterList/CharacterList';
import CharacterForm from './pages/CharacterForm/CharacterForm';
import TableList from './pages/TableList/TableList';
import TableRoom from './pages/TableRoom/TableRoom';
import Account from './pages/Account/Account';
import ReleaseNotesPage from './pages/ReleaseNotes/ReleaseNotes';
import './App.css';

/** Redireciona para /login se não houver sessão ativa */
function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { userId, token } = useSession();
  return (userId && token) ? <>{children}</> : <Navigate to="/login" replace />;
}

function Shell() {
  const { role, displayName, userId, clear } = useSession();
  const { pathname } = useLocation();
  const inRoom = /^\/mesas\/[^/]+/.test(pathname);
  const canCreateSheets = !inRoom || role === 'MESTRE';

  return (
    <div className={`shell ${inRoom ? 'shell--room' : ''}`}>
      <Scenery variant={userId ? sceneryForPath(pathname) : 'portal'} />
      <header className="topbar">
        <NavLink to="/" className="brand">
          <span className="brand-glyph">✦</span>
          <span className="brand-name">ARCANA</span>
        </NavLink>

        {userId && (
          <>
            <nav className="nav" aria-label="Principal">
              {canCreateSheets && (
                <NavLink to="/fichas" className={({ isActive }) => (isActive ? 'active' : '')}>
                  <QuillIcon />
                  Fichas
                </NavLink>
              )}
              <NavLink to="/mesas" className={({ isActive }) => (isActive ? 'active' : '')}>
                <D20Icon />
                Mesas
              </NavLink>
              {!inRoom && (
                <NavLink to="/conta" className={({ isActive }) => (isActive ? 'active' : '')}>
                  <HelmIcon />
                  Minha conta
                </NavLink>
              )}
              {!inRoom && (
                <NavLink to="/notas" className={({ isActive }) => (isActive ? 'active' : '')}>
                  <PosterIcon />
                  Notas
                </NavLink>
              )}
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

      {userId && role === 'JOGADOR' && <TableInvites />}
      {userId && <ReleaseNotesGate userId={userId} paused={inRoom} />}

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
          <Route path="/conta" element={
            <PrivateRoute><Account /></PrivateRoute>
          } />
          <Route path="/notas" element={
            <PrivateRoute><ReleaseNotesPage /></PrivateRoute>
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
      <UserChannelProvider>
        <Shell />
      </UserChannelProvider>
    </SessionProvider>
  );
}
