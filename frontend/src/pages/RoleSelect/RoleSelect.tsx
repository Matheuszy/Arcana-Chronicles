import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSession, UserRole } from '../../context/SessionContext';
import './RoleSelect.css';

export default function RoleSelect() {
  const [selected, setSelected] = useState<UserRole>(null);
  const [displayName, setDisplayName] = useState('');
  const { chooseRole } = useSession();
  const navigate = useNavigate();

  const confirm = () => {
    if (!selected || !displayName.trim()) return;
    chooseRole(selected, displayName.trim());
    navigate('/mesas');
  };

  return (
    <section className="hero">
      <p className="eyebrow">Antes de abrir o portal</p>
      <h1>Quem senta à mesa hoje?</h1>
      <p className="sub">
        Escolha seu papel nesta sessão. Você pode assumir o outro papel em outra mesa depois.
      </p>

      <div className="sigils">
        <button
          type="button"
          className={`sigil ${selected === 'JOGADOR' ? 'picked' : ''}`}
          onClick={() => setSelected('JOGADOR')}
        >
          <span className="sigil-mark">⚔</span>
          <span className="sigil-title">Jogador</span>
          <span className="sigil-desc">Crie sua ficha, entre em mesas e viva a história.</span>
        </button>

        <button
          type="button"
          className={`sigil sigil--mestre ${selected === 'MESTRE' ? 'picked' : ''}`}
          onClick={() => setSelected('MESTRE')}
        >
          <span className="sigil-mark">☾</span>
          <span className="sigil-title">Mestre</span>
          <span className="sigil-desc">Crie NPCs, monstros e conduza a mesa como narrador.</span>
        </button>
      </div>

      {selected && (
        <div className="confirm card">
          <div className="field">
            <label htmlFor="displayName">Como devemos te chamar?</label>
            <input
              id="displayName"
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              onKeyUp={(e) => e.key === 'Enter' && confirm()}
              placeholder="ex: Theron, o Bardo"
            />
          </div>
          <button className="btn btn-primary" onClick={confirm} disabled={!displayName.trim()}>
            Entrar como {selected === 'MESTRE' ? 'Mestre' : 'Jogador'}
          </button>
        </div>
      )}
    </section>
  );
}
