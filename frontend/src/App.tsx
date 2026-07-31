import { NavLink, Route, Routes } from 'react-router-dom';
import { SessionProvider, useSession } from './context/SessionContext';
import RoleSelect from './pages/RoleSelect/RoleSelect';
import CharacterList from './pages/CharacterList/CharacterList';
import CharacterForm from './pages/CharacterForm/CharacterForm';
import TableList from './pages/TableList/TableList';
import TableRoom from './pages/TableRoom/TableRoom';
import './App.css';

function Shell() {
  const { role, displayName } = useSession();

  return (
    <div className="shell">
      <header className="topbar">
        <NavLink to="/" className="brand">
          <span className="brand-glyph">✦</span>
          <span className="brand-name">ARCANA</span>
        </NavLink>

        {role && (
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
            </div>
          </>
        )}
      </header>

      <main className="stage">
        <Routes>
          <Route path="/" element={<RoleSelect />} />
          <Route path="/fichas" element={<CharacterList />} />
          <Route path="/fichas/nova" element={<CharacterForm />} />
          <Route path="/fichas/:id/editar" element={<CharacterForm />} />
          <Route path="/mesas" element={<TableList />} />
          <Route path="/mesas/:id" element={<TableRoom />} />
          <Route path="*" element={<RoleSelect />} />
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
