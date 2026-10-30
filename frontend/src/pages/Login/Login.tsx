import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AccountRole, authService } from '../../services/authService';
import { useSession } from '../../context/SessionContext';
import Tabs from '../../components/ui/Tabs';
import './Login.css';

type Mode = 'login' | 'register';

export default function Login() {
  const navigate = useNavigate();
  const { chooseRole } = useSession();

  const [mode, setMode] = useState<Mode>('login');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<AccountRole>('JOGADOR');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async () => {
    setError('');
    if (!username.trim() || !password.trim()) {
      setError('Preencha todos os campos.');
      return;
    }
    if (mode === 'register' && !email.trim()) {
      setError('Informe um e-mail para o cadastro.');
      return;
    }

    setLoading(true);
    try {
      if (mode === 'register') {
        await authService.register(username.trim(), email.trim(), password, role);
      }
      const res = await authService.login(username.trim(), password);
      chooseRole(res.papel, res.username, res.id, res.token);
      navigate('/mesas');
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Erro ao conectar com o servidor.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="login-hero">
      <div className="login-brand">
        <span className="login-glyph">✦</span>
        <h1>ARCANA</h1>
        <p className="login-sub">Crônicas de um mundo esquecido</p>
      </div>

      <div className="login-card card">
        <Tabs
          label="Entrar ou criar conta"
          active={mode}
          onChange={(next) => { setMode(next); setError(''); }}
          tabs={[
            { id: 'login', label: 'Entrar' },
            { id: 'register', label: 'Criar conta' },
          ]}
        />

        <form
          onSubmit={(e) => { e.preventDefault(); submit(); }}
          className="login-form"
        >
          <div className="field">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="ex: tharanis_mago"
              autoComplete="username"
            />
          </div>

          {mode === 'register' && (
            <div className="field">
              <label htmlFor="email">E-mail</label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="ex: mago@arcana.com"
                autoComplete="email"
              />
            </div>
          )}

          <div className="field">
            <label htmlFor="password">Senha</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            />
          </div>

          {mode === 'register' && (
          <div className="field">
            <span id="roleLabel" className="role-label">Tipo de conta</span>
            <div className="role-pills" role="radiogroup" aria-labelledby="roleLabel">
              <button
                type="button"
                role="radio"
                aria-checked={role === 'JOGADOR'}
                className={`role-pill ${role === 'JOGADOR' ? 'active' : ''}`}
                onClick={() => setRole('JOGADOR')}
              >
                ⚔ Jogador
              </button>
              <button
                type="button"
                role="radio"
                aria-checked={role === 'MESTRE'}
                className={`role-pill role-pill--mestre ${role === 'MESTRE' ? 'active' : ''}`}
                onClick={() => setRole('MESTRE')}
              >
                ☾ Mestre
              </button>
            </div>
            <p className="role-hint">
              O tipo de conta não pode ser alterado depois. Para jogar com outro papel, crie outra conta.
            </p>
          </div>
          )}

          {error && <p className="login-error" role="alert">{error}</p>}

          <button
            type="submit"
            className="btn btn-primary login-submit"
            disabled={loading}
          >
            {loading
              ? 'Aguarde...'
              : mode === 'login'
              ? 'Abrir o portal'
              : 'Forjar minha conta'}
          </button>
        </form>
      </div>
    </section>
  );
}
