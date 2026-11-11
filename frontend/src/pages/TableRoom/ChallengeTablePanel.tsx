import {
  CHALLENGE_STATE_LABEL,
  Challenge,
  HelpKind,
  challengeAuthor,
  challengeTargetsSentence,
} from '../../types/challenge';
import { HelpReaction } from '../../types/combat';
import { HelpForm, HelpList, RollSummary } from './ChallengeHelps';

interface ChallengeTablePanelProps {
  challenges: Challenge[];
  viewerId: string | null;
  isMaster: boolean;
  reaction: HelpReaction;
  canSend: boolean;
  onHelp: (challengeId: string, targetId: string, kind: HelpKind, faces?: number) => void;
  onRemoveHelp: (challengeId: string, targetId: string, helpId: string) => void;
}

export default function ChallengeTablePanel({
  challenges,
  viewerId,
  isMaster,
  reaction,
  canSend,
  onHelp,
  onRemoveHelp,
}: ChallengeTablePanelProps) {
  const shown = challenges.filter((challenge) => challenge.ativo && String(challenge.criadorId) !== viewerId);
  if (shown.length === 0) return null;

  return (
    <div className="challenge-panel">
      <h3 className="section-title">Desafios da mesa</h3>
      <p className="muted">Qualquer pessoa da mesa pode ajudar ou atrapalhar um teste.</p>
      <ul className="challenge-list">
        {shown.map((challenge) => (
          <li key={challenge.id} className="challenge-card challenge-card--ativo">
            <div className="challenge-card-head">
              <strong>{challenge.descricao ?? 'Desafio'}</strong>
              <span className="challenge-card-dc">{challenge.cd === null ? 'CD oculta' : `CD ${challenge.cd}`}</span>
            </div>
            <p className="muted challenge-announcement">
              {challengeAuthor(challenge)} desafiou {challengeTargetsSentence(challenge)}
            </p>
            <ul className="challenge-players">
              {challenge.jogadores.map((player) => (
                <li key={player.id} className="challenge-player">
                  <div className="challenge-player-head">
                    <span>{player.nomePersonagem}</span>
                    <span className={`challenge-state challenge-state--${player.estado.toLowerCase()}`}>
                      {CHALLENGE_STATE_LABEL[player.estado]}
                    </span>
                  </div>
                  <RollSummary player={player} />
                  <HelpList
                    player={player}
                    viewerId={viewerId}
                    isMaster={isMaster}
                    canSend={canSend}
                    onRemove={(helpId) => onRemoveHelp(challenge.id, player.id, helpId)}
                  />
                  <HelpForm
                    challenge={challenge}
                    player={player}
                    viewerId={viewerId}
                    reaction={reaction}
                    canSend={canSend}
                    onHelp={(kind, faces) => onHelp(challenge.id, player.id, kind, faces)}
                  />
                </li>
              ))}
            </ul>
          </li>
        ))}
      </ul>
    </div>
  );
}
