import { useState } from 'react';
import { useConfirm } from '../../components/ui/Confirm';
import Select from '../../components/ui/Select';
import { TableParticipant } from '../../types/table';
import { RollMode } from '../../types/chat';
import {
  CHALLENGE_STATE_LABEL,
  Challenge,
  ChallengeMessageTarget,
  ChallengePlayer,
  ChallengeTarget,
  MAX_CHALLENGE_DC,
  MAX_CHALLENGE_DESCRIPTION,
  MAX_CHALLENGE_MESSAGE,
  MIN_CHALLENGE_DC,
  ROLL_MODE_LABEL,
  isActiveState,
} from '../../types/challenge';

const MODES: RollMode[] = ['NORMAL', 'VANTAGEM', 'DESVANTAGEM'];

interface ChallengeMasterPanelProps {
  challenges: Challenge[];
  participants: TableParticipant[];
  targets: ChallengeTarget[];
  viewerId: string | null;
  title: string;
  createHint?: string;
  canSend: boolean;
  onCreate: (dc: number, description: string, targets: { alvoId: string; modo: RollMode }[]) => void;
  onAllowExtraAttempt: (challengeId: string, targetId: string, mode: RollMode) => void;
  onEndWithFailure: (challengeId: string, targetId: string) => void;
  onCancel: (challengeId: string, targetId?: string) => void;
  onSendMessage: (challengeId: string, target: ChallengeMessageTarget, text: string, targetId?: string) => void;
}

interface Selection {
  selected: boolean;
  mode: RollMode;
}

function ModeSelect({ value, onChange }: { value: RollMode; onChange: (mode: RollMode) => void }) {
  return (
    <Select
      label="Modo da rolagem"
      hideLabel
      compact
      value={value}
      onChange={onChange}
      options={MODES.map((mode) => ({ value: mode, label: ROLL_MODE_LABEL[mode] }))}
    />
  );
}

function PlayerRow({
  challenge,
  player,
  manage,
  canSend,
  onAllowExtraAttempt,
  onEndWithFailure,
  onCancel,
}: {
  challenge: Challenge;
  player: ChallengePlayer;
  manage: boolean;
  canSend: boolean;
  onAllowExtraAttempt: ChallengeMasterPanelProps['onAllowExtraAttempt'];
  onEndWithFailure: ChallengeMasterPanelProps['onEndWithFailure'];
  onCancel: ChallengeMasterPanelProps['onCancel'];
}) {
  const confirm = useConfirm();
  const [extraMode, setExtraMode] = useState<RollMode>(player.modo);
  const active = isActiveState(player.estado);
  const last = player.rolagens[player.rolagens.length - 1];

  return (
    <li className="challenge-player">
      <div className="challenge-player-head">
        <span>
          {player.nomePersonagem} <small className="muted">({player.nomeJogador})</small>
        </span>
        <span className={`challenge-state challenge-state--${player.estado.toLowerCase()}`}>
          {CHALLENGE_STATE_LABEL[player.estado]}
        </span>
      </div>
      <p className="muted challenge-player-info">
        {ROLL_MODE_LABEL[player.modo]}
        {player.rolagens.map((r) => ` · ${r.descricaoEscolha}: ${r.total}${r.sucesso ? ' ✓' : ' ✗'}`).join('')}
      </p>

      {manage && player.estado === 'AGUARDANDO_DECISAO_DO_MESTRE' && last && (
        <div className="challenge-actions">
          <ModeSelect value={extraMode} onChange={setExtraMode} />
          <button
            className="btn btn-primary"
            disabled={!canSend}
            onClick={() => onAllowExtraAttempt(challenge.id, player.id, extraMode)}
          >
            Permitir nova tentativa
          </button>
          <button
            className="btn btn-ghost"
            disabled={!canSend}
            onClick={() => onEndWithFailure(challenge.id, player.id)}
          >
            Encerrar com falha
          </button>
        </div>
      )}

      {manage && active && (
        <button
          className="btn btn-ghost challenge-cancel"
          onClick={async () => {
            const confirmed = await confirm({
              title: 'Cancelar desafio?',
              message: `Cancelar o desafio de ${player.nomePersonagem}?`,
              confirmLabel: 'Cancelar desafio',
              cancelLabel: 'Voltar',
              danger: true,
            });
            if (confirmed) onCancel(challenge.id, player.id);
          }}
        >
          Cancelar este participante
        </button>
      )}
    </li>
  );
}

function MessageComposer({
  challenge,
  participants,
  canSend,
  onSendMessage,
}: {
  challenge: Challenge;
  participants: TableParticipant[];
  canSend: boolean;
  onSendMessage: ChallengeMasterPanelProps['onSendMessage'];
}) {
  const resolved = challenge.jogadores.filter((p) => p.estado === 'SUCESSO' || p.estado === 'FALHA');
  const winners = resolved.filter((p) => p.estado === 'SUCESSO');
  const losers = resolved.filter((p) => p.estado === 'FALHA');
  const [target, setTarget] = useState<ChallengeMessageTarget>('VENCEDORES');
  const [targetId, setTargetId] = useState('');
  const [text, setText] = useState('');

  if (resolved.length === 0) return null;

  const nameOf = (id: number) =>
    participants.find((p) => String(p.userId) === String(id))?.displayName ?? String(id);
  const recipientCount = target === 'VENCEDORES' ? winners.length : target === 'DERROTADOS' ? losers.length : targetId ? 1 : 0;
  const trimmed = text.trim();

  const send = () => {
    if (!trimmed || recipientCount === 0) return;
    onSendMessage(challenge.id, target, trimmed, target === 'JOGADOR' ? targetId : undefined);
    setText('');
  };

  return (
    <div className="challenge-composer">
      <h4 className="section-title">Mensagem privada</h4>
      <div className="field">
        <Select
          label="Enviar para"
          value={target}
          onChange={setTarget}
          options={[
            { value: 'VENCEDORES', label: `Todos que venceram (${winners.length})` },
            { value: 'DERROTADOS', label: `Todos que falharam (${losers.length})` },
            { value: 'JOGADOR', label: 'Um participante específico' },
          ]}
        />
      </div>
      {target === 'JOGADOR' && (
        <div className="field">
          <Select
            label="Participante"
            value={targetId}
            onChange={setTargetId}
            options={resolved.map((p) => ({
              value: p.id,
              label: `${p.nomePersonagem} — ${p.estado === 'SUCESSO' ? 'venceu' : 'falhou'}`,
            }))}
          />
        </div>
      )}
      <div className="field">
        <label htmlFor={`text-${challenge.id}`}>
          Mensagem ({text.length}/{MAX_CHALLENGE_MESSAGE})
        </label>
        <textarea
          id={`text-${challenge.id}`}
          rows={2}
          maxLength={MAX_CHALLENGE_MESSAGE}
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="Só os jogadores escolhidos verão esta mensagem."
        />
      </div>
      <button className="btn btn-primary" disabled={!canSend || !trimmed || recipientCount === 0} onClick={send}>
        Enviar mensagem
      </button>

      {challenge.mensagens.length > 0 && (
        <ul className="challenge-sent">
          {challenge.mensagens.map((message) => (
            <li key={message.id}>
              <span className="muted">
                Para{' '}
                {message.destinatarios && message.destinatarios.length > 0
                  ? message.destinatarios.map(nameOf).join(', ')
                  : 'ninguém'}
                :
              </span>{' '}
              {message.texto}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default function ChallengeMasterPanel({
  challenges,
  participants,
  targets,
  viewerId,
  title,
  createHint,
  canSend,
  onCreate,
  onAllowExtraAttempt,
  onEndWithFailure,
  onCancel,
  onSendMessage,
}: ChallengeMasterPanelProps) {
  const confirm = useConfirm();
  const [formOpen, setFormOpen] = useState(false);
  const [dc, setDc] = useState('12');
  const [description, setDescription] = useState('');
  const [selection, setSelection] = useState<Record<string, Selection>>({});
  const [formError, setFormError] = useState<string | null>(null);

  const busyTargetIds = new Set(
    challenges.flatMap((c) => c.jogadores.filter((p) => isActiveState(p.estado)).map((p) => p.id)),
  );
  const chosen = targets.filter((t) => selection[t.id]?.selected);

  const toggle = (userId: string) =>
    setSelection((prev) => ({
      ...prev,
      [userId]: { mode: prev[userId]?.mode ?? 'NORMAL', selected: !prev[userId]?.selected },
    }));

  const changeMode = (userId: string, mode: RollMode) =>
    setSelection((prev) => ({ ...prev, [userId]: { selected: prev[userId]?.selected ?? false, mode } }));

  const create = () => {
    const parsedDc = Number(dc);
    if (!Number.isInteger(parsedDc) || parsedDc < MIN_CHALLENGE_DC || parsedDc > MAX_CHALLENGE_DC) {
      setFormError(`A CD deve estar entre ${MIN_CHALLENGE_DC} e ${MAX_CHALLENGE_DC}.`);
      return;
    }
    if (chosen.length === 0) {
      setFormError('Selecione ao menos um alvo.');
      return;
    }
    onCreate(
      parsedDc,
      description.trim(),
      chosen.map((t) => ({ alvoId: t.id, modo: selection[t.id]?.mode ?? 'NORMAL' })),
    );
    setFormError(null);
    setDescription('');
    setSelection({});
    setFormOpen(false);
  };

  const ordered = challenges.slice().reverse();

  return (
    <div className="challenge-panel">
      <h3 className="section-title">
        {title}
        {challenges.some((c) => c.ativo) && <span className="badge">{challenges.filter((c) => c.ativo).length}</span>}
      </h3>

      {!formOpen && createHint && <p className="muted">{createHint}</p>}
      {!formOpen && !createHint ? (
        <button className="btn btn-primary challenge-new" onClick={() => setFormOpen(true)}>
          Novo desafio
        </button>
      ) : null}

      {formOpen && !createHint && (
        <div className="challenge-form">
          <div className="row">
            <div className="field">
              <label htmlFor="challengeDc">CD</label>
              <input
                id="challengeDc"
                type="number"
                min={MIN_CHALLENGE_DC}
                max={MAX_CHALLENGE_DC}
                value={dc}
                onChange={(e) => setDc(e.target.value)}
              />
            </div>
            <div className="field grow">
              <label htmlFor="challengeDescription">
                Descrição ({description.length}/{MAX_CHALLENGE_DESCRIPTION})
              </label>
              <input
                id="challengeDescription"
                type="text"
                maxLength={MAX_CHALLENGE_DESCRIPTION}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="ex: Escalar o muro"
              />
            </div>
          </div>

          <p className="muted">Alvos do desafio e a rolagem de cada um:</p>
          {targets.length === 0 && <p className="muted">Nenhum alvo disponível.</p>}
          <ul className="challenge-pick-list">
            {targets.map((target) => {
              const busy = busyTargetIds.has(target.id);
              const reason = busy ? 'em desafio' : target.disabledReason;
              const blocked = Boolean(reason);
              const current = selection[target.id];
              return (
                <li key={target.id} className="challenge-pick">
                  <label>
                    <input
                      type="checkbox"
                      disabled={blocked}
                      checked={Boolean(current?.selected) && !blocked}
                      onChange={() => toggle(target.id)}
                    />
                    {target.online !== undefined && (
                      <span className={`presence-dot ${target.online ? 'online' : ''}`} aria-hidden="true" />
                    )}
                    {target.label}
                    {target.detail && <small className="muted"> {target.detail}</small>}
                  </label>
                  {blocked ? (
                    <small className="muted">{reason}</small>
                  ) : target.kind === 'MONSTRO' ? (
                    <small className="muted">o mestre escolhe a rolagem</small>
                  ) : (
                    <ModeSelect
                      value={current?.mode ?? 'NORMAL'}
                      onChange={(mode) => changeMode(target.id, mode)}
                    />
                  )}
                </li>
              );
            })}
          </ul>

          {formError && <p className="roll-error">{formError}</p>}
          <div className="roll-panel-actions">
            <button className="btn btn-ghost" onClick={() => setFormOpen(false)}>
              Cancelar
            </button>
            <button className="btn btn-primary" disabled={!canSend} onClick={create}>
              Enviar desafio
            </button>
          </div>
        </div>
      )}

      {ordered.length === 0 && !formOpen && <p className="muted">Nenhum desafio nesta sessão.</p>}

      <ul className="challenge-list">
        {ordered.map((challenge) => {
          const own = String(challenge.criadorId) === viewerId;
          return (
          <li key={challenge.id} className={`challenge-card ${challenge.ativo ? 'challenge-card--ativo' : ''}`}>
            <div className="challenge-card-head">
              <strong>{challenge.descricao ?? 'Desafio'}</strong>
              <span className="challenge-card-dc">CD {challenge.cd}</span>
            </div>
            {!own && <p className="muted">Lançado por {challenge.criadorNome}</p>}
            <ul className="challenge-players">
              {challenge.jogadores.map((player) => (
                <PlayerRow
                  key={player.id}
                  challenge={challenge}
                  player={player}
                  manage={own}
                  canSend={canSend}
                  onAllowExtraAttempt={onAllowExtraAttempt}
                  onEndWithFailure={onEndWithFailure}
                  onCancel={onCancel}
                />
              ))}
            </ul>
            {own && challenge.ativo && challenge.jogadores.length > 1 && (
              <button
                className="btn btn-danger challenge-cancel"
                onClick={async () => {
                  const confirmed = await confirm({
                    title: 'Cancelar para todos?',
                    message: 'Cancelar o desafio para todos os jogadores em andamento?',
                    confirmLabel: 'Cancelar para todos',
                    cancelLabel: 'Voltar',
                    danger: true,
                  });
                  if (confirmed) onCancel(challenge.id);
                }}
              >
                Cancelar para todos
              </button>
            )}
            {own && (
              <MessageComposer
                challenge={challenge}
                participants={participants}
                canSend={canSend}
                onSendMessage={onSendMessage}
              />
            )}
          </li>
          );
        })}
      </ul>
    </div>
  );
}
