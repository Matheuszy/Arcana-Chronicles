import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useSession } from '../../context/SessionContext';
import { MIN_PASSWORD_LENGTH, accountService } from '../../services/accountService';
import { characterService } from '../../services/characterService';
import { tableService } from '../../services/tableService';
import { Character, KIND_LABEL } from '../../types/character';
import { GameTable, TABLE_STATUS_LABEL } from '../../types/table';
import Badge from '../../components/ui/Badge';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import { ErrorState, LoadingState } from '../../components/ui/States';
import { useToast } from '../../components/ui/Toast';
import './Account.css';

const errorMessage = (e: unknown, fallback: string) => (e instanceof Error ? e.message : fallback);

export default function Account() {
  const navigate = useNavigate();
  const { userId, role, chooseRole, clear } = useSession();
  const { notify } = useToast();

  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [characters, setCharacters] = useState<Character[]>([]);
  const [tables, setTables] = useState<GameTable[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const load = useCallback(() => {
    if (userId === null) return;
    setLoading(true);
    setLoadError(null);
    Promise.all([accountService.get(userId), characterService.list(), tableService.list()])
      .then(([profile, ownCharacters, allTables]) => {
        setUsername(profile.username);
        setEmail(profile.email);
        setCharacters(ownCharacters);
        setTables(
          allTables.filter(
            (t) =>
              String(t.masterId) === String(userId) ||
              t.participants?.some((p) => String(p.userId) === String(userId)),
          ),
        );
      })
      .catch((e: unknown) => setLoadError(errorMessage(e, 'Não foi possível carregar sua conta.')))
      .finally(() => setLoading(false));
  }, [userId]);

  useEffect(load, [load]);

  const passwordTooShort = password.length > 0 && password.length < MIN_PASSWORD_LENGTH;
  const canSave = username.trim() !== '' && email.trim() !== '' && !passwordTooShort && !saving;

  const save = async () => {
    if (userId === null || !canSave) return;
    setSaving(true);
    setSaveError(null);
    try {
      const updated = await accountService.update(userId, {
        username: username.trim(),
        email: email.trim(),
        password: password || undefined,
      });
      chooseRole(updated.papel, updated.username, updated.id, updated.token);
      setPassword('');
      notify('Dados da conta atualizados.', 'success');
    } catch (e: unknown) {
      setSaveError(errorMessage(e, 'Não foi possível salvar os dados.'));
    } finally {
      setSaving(false);
    }
  };

  const deleteAccount = async () => {
    if (userId === null) return;
    setDeleting(true);
    try {
      await accountService.delete(userId);
      clear();
      navigate('/login');
    } catch (e: unknown) {
      setConfirmingDelete(false);
      notify(errorMessage(e, 'Não foi possível excluir a conta.'), 'danger');
    } finally {
      setDeleting(false);
    }
  };

  const deleteWarning =
    role === 'MESTRE'
      ? 'Suas fichas e todas as mesas que você conduz serão apagadas, e as sessões em andamento terminam para todos.'
      : 'Suas fichas serão apagadas e você sai de todas as mesas.';

  if (loading) {
    return (
      <section className="page">
        <LoadingState label="Abrindo sua conta..." />
      </section>
    );
  }

  if (loadError) {
    return (
      <section className="page">
        <ErrorState title="Não foi possível abrir sua conta" message={loadError} onRetry={load} />
      </section>
    );
  }

  return (
    <section className="page account-page">
      <header className="page-head">
        <div>
          <p className="eyebrow">Seu cadastro na estalagem</p>
          <h1>Minha conta</h1>
        </div>
        <Badge tone={role === 'MESTRE' ? 'gold' : 'info'}>{role === 'MESTRE' ? 'Mestre' : 'Jogador'}</Badge>
      </header>

      <form
        className="card account-form"
        onSubmit={(e) => {
          e.preventDefault();
          save();
        }}
      >
        <h2>Meus dados</h2>
        <div className="field">
          <label htmlFor="accountUsername">Nome de usuário</label>
          <input
            id="accountUsername"
            type="text"
            value={username}
            maxLength={100}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
          />
        </div>
        <div className="field">
          <label htmlFor="accountEmail">E-mail</label>
          <input
            id="accountEmail"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"
          />
        </div>
        <div className="field">
          <label htmlFor="accountPassword">Nova senha</label>
          <input
            id="accountPassword"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Deixe em branco para manter a atual"
            autoComplete="new-password"
            aria-describedby="accountPasswordHint"
          />
          <p id="accountPasswordHint" className={passwordTooShort ? 'form-error' : 'muted account-hint'}>
            A senha precisa ter ao menos {MIN_PASSWORD_LENGTH} caracteres.
          </p>
        </div>
        <p className="muted account-hint">O tipo de conta não pode ser alterado.</p>
        {saveError && (
          <p className="form-error" role="alert">
            {saveError}
          </p>
        )}
        <div>
          <button type="submit" className="btn btn-primary" disabled={!canSave}>
            {saving ? 'Salvando...' : 'Salvar alterações'}
          </button>
        </div>
      </form>

      <div className="account-columns">
        <section className="card" aria-labelledby="accountSheets">
          <h2 id="accountSheets">Minhas fichas</h2>
          {characters.length === 0 ? (
            <p className="muted">Você ainda não criou fichas.</p>
          ) : (
            <ul className="account-list">
              {characters.map((c) => (
                <li key={c.id}>
                  <Link to={`/fichas/${c.id}/editar`}>{c.name}</Link>
                  <Badge>{KIND_LABEL[c.kind]}</Badge>
                </li>
              ))}
            </ul>
          )}
          <Link to="/fichas" className="btn btn-ghost account-link">
            Ver todas as fichas
          </Link>
        </section>

        <section className="card" aria-labelledby="accountTables">
          <h2 id="accountTables">{role === 'MESTRE' ? 'Mesas que conduzo' : 'Mesas em que participo'}</h2>
          {tables.length === 0 ? (
            <p className="muted">Nenhuma mesa por enquanto.</p>
          ) : (
            <ul className="account-list">
              {tables.map((t) => (
                <li key={t.id}>
                  {t.status === 'ENCERRADA' ? (
                    <span>{t.name}</span>
                  ) : (
                    <Link to={`/mesas/${t.id}`}>{t.name}</Link>
                  )}
                  <Badge tone={t.status === 'ENCERRADA' ? 'danger' : 'positive'}>{TABLE_STATUS_LABEL[t.status]}</Badge>
                </li>
              ))}
            </ul>
          )}
          <Link to="/mesas" className="btn btn-ghost account-link">
            Ver todas as mesas
          </Link>
        </section>
      </div>

      <section className="card account-danger" aria-labelledby="accountDelete">
        <h2 id="accountDelete">Excluir conta</h2>
        <p className="muted">{deleteWarning} Essa ação não pode ser desfeita.</p>
        <button type="button" className="btn btn-danger" onClick={() => setConfirmingDelete(true)}>
          Excluir minha conta
        </button>
      </section>

      {confirmingDelete && (
        <ConfirmDialog
          title="Excluir sua conta?"
          confirmLabel="Excluir conta"
          danger
          busy={deleting}
          onConfirm={deleteAccount}
          onCancel={() => setConfirmingDelete(false)}
        >
          {deleteWarning} Essa ação não pode ser desfeita.
        </ConfirmDialog>
      )}
    </section>
  );
}
