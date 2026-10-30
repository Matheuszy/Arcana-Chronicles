import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useSession } from '../../context/SessionContext';
import { tableService } from '../../services/tableService';
import { characterService } from '../../services/characterService';
import { chatService, ConnectionState, ConnectionStatus, MAX_RECONNECT_ATTEMPTS } from '../../services/chatService';
import { diceService, isRollCommand } from '../../services/diceService';
import { CharacterEditor } from '../CharacterForm/CharacterForm';
import { GameTable, TABLE_STATUS_LABEL, TableStatus } from '../../types/table';
import { Character, Attributes } from '../../types/character';
import { CHANNEL_LABEL, CHANNELS, Channel, ChatMessage, Presence, RollRequest } from '../../types/chat';
import { ChallengeTarget, isActiveState } from '../../types/challenge';
import { BLOCKED_ROLL_DURING_COMBAT, isMyTurn } from '../../types/combat';
import { useConfirm } from '../../components/ui/Confirm';
import Select from '../../components/ui/Select';
import Tabs, { panelId, tabId } from '../../components/ui/Tabs';
import { useToast } from '../../components/ui/Toast';
import NpcLinksPanel from './NpcLinksPanel';
import ChallengeMasterPanel from './ChallengeMasterPanel';
import ChallengePlayerPanel from './ChallengePlayerPanel';
import ChallengeMonsterRolls from './ChallengeMonsterRolls';
import ChallengeRollOverlay from './ChallengeRollOverlay';
import CombatStartPanel from './CombatStartPanel';
import CombatTracker from './CombatTracker';
import DiceRoller from './DiceRoller';
import DiceRollMessage from './DiceRollMessage';
import { useChallenges } from './useChallenges';
import { useCombat } from './useCombat';
import ImageComposer from './ImageComposer';
import NotesPanel from './NotesPanel';
import ReceivedNoteDialog from './ReceivedNoteDialog';
import SharedItemsView from './SharedItemsView';
import StatusControls from './StatusControls';
import StatusView from './StatusView';
import { useRoomResources } from './useRoomResources';
import JoinRequestsPanel from './JoinRequestsPanel';
import ParticipantsPanel from './ParticipantsPanel';
import PrivateSheetsPanel from './PrivateSheetsPanel';
import './TableRoom.css';
import './Challenge.css';
import './Combat.css';
import './RoomResources.css';
import './RoomTheme.css';

const ATTRIBUTE_KEYS: (keyof Attributes)[] = [
  'forca',
  'destreza',
  'constituicao',
  'inteligencia',
  'sabedoria',
  'carisma',
];

const MASTER_STATUS_OPTIONS: TableStatus[] = ['ABERTA', 'EM_ANDAMENTO', 'PAUSADA'];

type RoomTab = Channel | 'STATUS' | 'ITENS';

const ROOM_TABS: RoomTab[] = [...CHANNELS, 'STATUS', 'ITENS'];

type SideTab = 'MESA' | 'GENTE' | 'FICHAS' | 'DESAFIO' | 'COMBATE' | 'DADOS';

const SIDE_TABS: SideTab[] = ['MESA', 'GENTE', 'FICHAS', 'DESAFIO', 'COMBATE', 'DADOS'];

const SIDE_LABEL: Record<SideTab, string> = {
  MESA: 'Mesa',
  GENTE: 'Gente',
  FICHAS: 'Fichas',
  DESAFIO: 'Desafio',
  COMBATE: 'Combate',
  DADOS: 'Dados',
};

const SIDE_ICON: Record<SideTab, string> = {
  MESA: '🏠',
  GENTE: '👥',
  FICHAS: '📜',
  DESAFIO: '⚔',
  COMBATE: '🛡',
  DADOS: '🎲',
};

const TABS_ID = 'room-tabs';

const TAB_ICON: Record<RoomTab, string> = {
  CONVERSA: '💬',
  ROLAGENS: '🎲',
  IMAGENS: '🖼',
  STATUS: '❖',
  ITENS: '🎒',
};

const TAB_LABEL: Record<RoomTab, string> = {
  ...CHANNEL_LABEL,
  STATUS: 'Status',
  ITENS: 'Itens',
};

const isChannel = (tab: RoomTab): tab is Channel => (CHANNELS as string[]).includes(tab);

type MessagesByChannel = Record<Channel, ChatMessage[]>;
type UnreadByChannel = Record<Channel, number>;

const EMPTY_MESSAGES: MessagesByChannel = { CONVERSA: [], ROLAGENS: [], IMAGENS: [] };
const NO_UNREAD: UnreadByChannel = { CONVERSA: 0, ROLAGENS: 0, IMAGENS: 0 };

const CONNECTION_LABEL: Record<ConnectionState, string> = {
  CONECTANDO: 'conectando...',
  CONECTADO: 'conectado',
  RECONECTANDO: 'reconectando...',
  DESCONECTADO: 'desconectado',
};

const EMPTY_CHANNEL_TEXT: Record<Channel, string> = {
  CONVERSA: 'A mesa está silenciosa. Diga olá para o grupo.',
  ROLAGENS: 'Nenhuma rolagem ainda. Use o painel de dados ou digite /r 1d20+2 na conversa.',
  IMAGENS: 'Nenhuma imagem compartilhada nesta sessão.',
};

export default function TableRoom() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { userId, token } = useSession();
  const { notify } = useToast();
  const confirm = useConfirm();

  const [table, setTable] = useState<GameTable | null>(null);
  const [tableError, setTableError] = useState<string | null>(null);
  const [characters, setCharacters] = useState<Character[]>([]);
  const [charactersLoading, setCharactersLoading] = useState(false);
  const [charactersError, setCharactersError] = useState<string | null>(null);
  const [characterError, setCharacterError] = useState<string | null>(null);
  const [expandedImage, setExpandedImage] = useState<string | null>(null);
  const [messages, setMessages] = useState<MessagesByChannel>(EMPTY_MESSAGES);
  const [activeChannel, setActiveChannel] = useState<RoomTab>('CONVERSA');
  const [unread, setUnread] = useState<UnreadByChannel>(NO_UNREAD);
  const activeChannelRef = useRef<RoomTab>('CONVERSA');
  const historyIds = useRef(new Set<string>());
  const [onlineUsers, setOnlineUsers] = useState<Presence[]>([]);
  const [sessionEndedReason, setSessionEndedReason] = useState<string | null>(null);
  const [removedReason, setRemovedReason] = useState<string | null>(null);
  const [leaving, setLeaving] = useState(false);
  const [leaveError, setLeaveError] = useState<string | null>(null);
  const [connectionAttempt, setConnectionAttempt] = useState(0);
  const [statusError, setStatusError] = useState<string | null>(null);
  const [connection, setConnection] = useState<ConnectionStatus>({ state: 'CONECTANDO', attempt: 0 });
  const [sideTab, setSideTab] = useState<SideTab>('MESA');
  const [editingSheet, setEditingSheet] = useState<{ id?: string } | null>(null);
  const [draft, setDraft] = useState('');
  const [composerError, setComposerError] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  const me = table?.participants.find((p) => String(p.userId) === String(userId));
  const isParticipant = Boolean(me);
  const isMaster = table !== null && String(table.masterId) === String(userId);
  const isClosed = table?.status === 'ENCERRADA';
  const isPaused = table?.status === 'PAUSADA';
  const activeCharacterId = me?.characterId ? String(me.characterId) : '';
  const activeCharacter = characters.find((c) => String(c.id) === activeCharacterId);
  const viewerId = userId !== null ? String(userId) : null;
  const {
    challenges,
    reveal,
    incomingMessage,
    handleMessage: handleChallengeMessage,
    reset: resetChallenges,
    dismissReveal,
    dismissMessage,
  } = useChallenges(isMaster);
  const {
    note,
    status: roomStatus,
    items: sharedItems,
    receivedNotes,
    dismissReceivedNote,
    handleMessage: handleRoomMessage,
    reset: resetRoomResources,
  } = useRoomResources((recipients) =>
    notify(`Trecho compartilhado com ${recipients}.`, 'success'),
  );
  const { combat, handleMessage: handleCombatMessage, reset: resetCombat } = useCombat();
  const rollBlockedByChallenge =
    !isMaster &&
    challenges.some((c) =>
      c.jogadores.some((p) => p.tipo === 'JOGADOR' && String(p.jogadorId) === viewerId && isActiveState(p.estado)),
    );
  const rollBlockedByCombat = combat !== null && !isMyTurn(combat, viewerId);
  const rollBlocked = rollBlockedByChallenge || rollBlockedByCombat;
  const rollBlockedReason = rollBlockedByChallenge
    ? 'Você tem um desafio em andamento. A única rolagem permitida é a do desafio.'
    : BLOCKED_ROLL_DURING_COMBAT;
  const sideTabs = isMaster ? SIDE_TABS : SIDE_TABS.filter((tab) => tab !== 'COMBATE');
  const turnKey = combat && isMyTurn(combat, viewerId) ? `${combat.id}-${combat.rodada}` : null;

  const activeChallengeCount = challenges.filter((c) => c.jogadores.some((p) => isActiveState(p.estado))).length;
  const previousChallengeCount = useRef(0);

  useEffect(() => {
    if (!isMaster && activeChallengeCount > previousChallengeCount.current) {
      setSideTab('DESAFIO');
    }
    previousChallengeCount.current = activeChallengeCount;
  }, [activeChallengeCount, isMaster]);

  useEffect(() => {
    if (turnKey) notify('É o seu turno!', 'success');
  }, [turnKey, notify]);

  const loadTable = useCallback(() => {
    if (!id) return;
    tableService
      .getById(id)
      .then((loaded) => {
        setTable(loaded);
        setTableError(null);
      })
      .catch((e: unknown) => setTableError(e instanceof Error ? e.message : 'Erro ao carregar a mesa.'));
  }, [id]);

  useEffect(loadTable, [loadTable]);

  useEffect(() => {
    if (!id || connection.state !== 'RECONECTANDO') return;
    tableService
      .getById(id)
      .then(setTable)
      .catch(() => undefined);
  }, [id, connection.state, connection.attempt]);

  useEffect(() => {
    if (!isParticipant) return;
    setCharactersLoading(true);
    setCharactersError(null);
    characterService
      .list(isMaster ? undefined : 'PLAYER')
      .then(setCharacters)
      .catch((e: unknown) => setCharactersError(e instanceof Error ? e.message : 'Erro ao carregar suas fichas.'))
      .finally(() => setCharactersLoading(false));
  }, [isParticipant, isMaster]);

  useEffect(() => {
    if (!id || !isParticipant || !token || isClosed) return;

    setSessionEndedReason(null);
    chatService.connect(id, token);
    const offMessage = chatService.onMessage((msg) => {
      if (handleChallengeMessage(msg) || handleRoomMessage(msg) || handleCombatMessage(msg)) return;
      if (msg.type === 'HISTORICO') {
        const restored: MessagesByChannel = {
          CONVERSA: msg.historico?.CONVERSA ?? [],
          ROLAGENS: msg.historico?.ROLAGENS ?? [],
          IMAGENS: msg.historico?.IMAGENS ?? [],
        };
        historyIds.current = new Set(
          CHANNELS.flatMap((c) => restored[c]).flatMap((m) => (m.id ? [m.id] : [])),
        );
        setMessages(restored);
        setUnread(NO_UNREAD);
        setOnlineUsers(msg.presentes ?? []);
      } else if (msg.type === 'PRESENCA') {
        setOnlineUsers(msg.presentes ?? []);
      } else if (msg.type === 'SESSAO_ENCERRADA') {
        setSessionEndedReason(msg.content);
        setOnlineUsers([]);
        resetChallenges();
        resetRoomResources();
        resetCombat();
        loadTable();
      } else if (msg.type === 'REMOVIDO_DA_MESA') {
        setRemovedReason(msg.content);
        setOnlineUsers([]);
        resetChallenges();
        resetRoomResources();
        resetCombat();
        loadTable();
      } else if (msg.type === 'STATUS_MESA' || msg.type === 'PARTICIPANTES') {
        loadTable();
      } else {
        const channel = msg.canal ?? 'CONVERSA';
        setMessages((prev) => ({ ...prev, [channel]: [...prev[channel], msg] }));
        if (channel !== activeChannelRef.current) {
          setUnread((prev) => ({ ...prev, [channel]: prev[channel] + 1 }));
        }
      }
    });
    const offStatus = chatService.onStatusChange(setConnection);

    return () => {
      offMessage();
      offStatus();
      chatService.disconnect();
    };
  }, [
    id,
    isParticipant,
    token,
    isClosed,
    connectionAttempt,
    loadTable,
    handleChallengeMessage,
    handleRoomMessage,
    handleCombatMessage,
    resetChallenges,
    resetRoomResources,
    resetCombat,
  ]);

  const changeStatus = async (newStatus: TableStatus) => {
    if (!id) return;
    setStatusError(null);
    try {
      setTable(await tableService.updateStatus(id, newStatus));
    } catch (e: unknown) {
      setStatusError(e instanceof Error ? e.message : 'Erro ao alterar o status da mesa.');
    }
  };

  const closeTable = async () => {
    const confirmed = await confirm({
      title: 'Encerrar a mesa?',
      message: 'A sessão termina para todos, o histórico do chat é apagado e a mesa não poderá ser reaberta.',
      confirmLabel: 'Encerrar mesa',
      danger: true,
    });
    if (confirmed) changeStatus('ENCERRADA');
  };

  const leaveTable = async () => {
    if (!id) return;
    const confirmed = await confirm({
      title: 'Sair da mesa?',
      message: 'Você será desconectado e precisará pedir para entrar de novo ao mestre.',
      confirmLabel: 'Sair da mesa',
      danger: true,
    });
    if (!confirmed) return;

    setLeaving(true);
    setLeaveError(null);
    try {
      await tableService.leave(id);
      navigate('/mesas');
    } catch (e: unknown) {
      setLeaveError(e instanceof Error ? e.message : 'Erro ao sair da mesa.');
      setLeaving(false);
    }
  };

  const changeCharacter = async (characterId: string) => {
    if (!id) return;
    setCharacterError(null);
    try {
      setTable(await tableService.changeCharacter(id, characterId || undefined));
    } catch (e: unknown) {
      setCharacterError(e instanceof Error ? e.message : 'Erro ao trocar de personagem.');
    }
  };

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, activeChannel]);

  const connected = connection.state === 'CONECTADO' && !sessionEndedReason;
  const canSend = connected && !isPaused;
  const reconnecting = connection.state === 'RECONECTANDO' && !sessionEndedReason;
  const connectionLost = connection.state === 'DESCONECTADO' && !sessionEndedReason && !removedReason;

  const selectChannel = (tab: RoomTab) => {
    activeChannelRef.current = tab;
    setActiveChannel(tab);
    if (isChannel(tab)) {
      setUnread((prev) => ({ ...prev, [tab]: 0 }));
    }
  };

  const linkNpc = async (characterId: string) => {
    if (!id) return;
    try {
      setTable(await tableService.linkNpc(id, characterId));
    } catch (e: unknown) {
      notify(e instanceof Error ? e.message : 'Não foi possível vincular o NPC.', 'danger');
    }
  };

  const unlinkNpc = async (characterId: string) => {
    if (!id) return;
    try {
      setTable(await tableService.unlinkNpc(id, characterId));
    } catch (e: unknown) {
      notify(e instanceof Error ? e.message : 'Não foi possível desvincular o NPC.', 'danger');
    }
  };

  const handleSheetSaved = (saved: Character) => {
    const created = !editingSheet?.id;
    if (created && isMaster && saved.id && saved.kind !== 'PLAYER') {
      linkNpc(saved.id).then(() => notify(`"${saved.name}" foi criado e vinculado à mesa.`, 'success'));
    }
    setCharacters((prev) =>
      prev.some((c) => String(c.id) === String(saved.id))
        ? prev.map((c) => (String(c.id) === String(saved.id) ? saved : c))
        : [...prev, saved],
    );
    setEditingSheet(null);
  };

  const sendRoll = (request: RollRequest) => {
    chatService.sendRoll(request);
    selectChannel('ROLAGENS');
  };

  const sendDraft = () => {
    const text = draft.trim();
    if (!id || !text) return;

    if (isRollCommand(text)) {
      if (rollBlocked) {
        setComposerError(rollBlockedReason);
        return;
      }
      try {
        sendRoll(diceService.parseCommand(text));
      } catch (e) {
        setComposerError((e as Error).message);
        return;
      }
    } else {
      chatService.sendText(text);
    }
    setDraft('');
    setComposerError(null);
  };

  const rollAttribute = (key: keyof Attributes) => {
    if (!activeCharacter) return;
    sendRoll(diceService.attributeCheckRequest(activeCharacter, key));
  };

  if (tableError) {
    return (
      <section className="room-blocked card">
        <h2>Não foi possível abrir a mesa</h2>
        <p className="muted">{tableError}</p>
        <Link className="btn btn-ghost" to="/mesas">Voltar para as mesas</Link>
      </section>
    );
  }

  if (!table) {
    return (
      <section className="room-blocked card">
        <p className="muted">Carregando mesa...</p>
      </section>
    );
  }

  if (!isParticipant) {
    return (
      <section className="room-blocked card">
        <h2>{table.name}</h2>
        {removedReason ? (
          <p className="muted">{removedReason} Para voltar, peça para entrar de novo na lista de mesas.</p>
        ) : (
          <p className="muted">
            Você ainda não participa desta mesa. Peça para entrar na lista de mesas e aguarde o mestre aceitar.
          </p>
        )}
        <Link className="btn btn-ghost" to="/mesas">Voltar para as mesas</Link>
      </section>
    );
  }

  if (isClosed) {
    return (
      <section className="room-blocked card">
        <h2>{table.name}</h2>
        <p className="muted">{sessionEndedReason ?? 'Esta mesa foi encerrada.'}</p>
        <p className="muted">O histórico do chat foi apagado e a mesa não aceita mais conexões.</p>
        <Link className="btn btn-ghost" to="/mesas">Voltar para as mesas</Link>
      </section>
    );
  }

  const masterChallengeTargets: ChallengeTarget[] = table.participants
    .filter((p) => p.role === 'JOGADOR')
    .map((p) => {
      const online = onlineUsers.some((u) => String(u.userId) === String(p.userId));
      return {
        id: String(p.userId),
        kind: 'JOGADOR',
        label: p.displayName,
        detail: online ? '(online)' : '(offline)',
        online,
        disabledReason: p.characterId ? undefined : 'sem personagem',
      };
    });
  const combatChallengeTargets: ChallengeTarget[] = (combat?.participantes ?? [])
    .filter((entry) => !(entry.tipo === 'JOGADOR' && String(entry.controladorId) === viewerId))
    .map((entry) => ({
      id: entry.id,
      kind: entry.tipo,
      label: entry.nomePersonagem,
      detail: entry.tipo === 'JOGADOR' ? `(${entry.nomeJogador})` : '(monstro do mestre)',
    }));
  const myCreatedChallenges = challenges.filter((c) => String(c.criadorId) === viewerId);
  const playerChallengeHint =
    combat === null
      ? 'Desafiar outros jogadores ou monstros só é possível durante o combate.'
      : !isMyTurn(combat, viewerId)
        ? 'Você só pode desafiar no seu turno.'
        : undefined;

  return (
    <section className="room">
      <aside className="room-side card">
        <header className="room-side-head">
          <h2>{table.name}</h2>
          <p className={`conn conn--${connection.state.toLowerCase()}`} role="status">
            ● {CONNECTION_LABEL[connection.state]}
          </p>
        </header>
        <div className="side-tabs">
          <Tabs
            label="Painéis da mesa"
            idPrefix="side-tabs"
            active={sideTab}
            onChange={setSideTab}
            tabs={sideTabs.map((tab) => ({
              id: tab,
              label: SIDE_LABEL[tab],
              icon: SIDE_ICON[tab],
              badge: tab === 'DESAFIO' ? activeChallengeCount : undefined,
            }))}
          />
        </div>
        <div className="room-side-body">
        <div className="side-section" hidden={sideTab !== 'MESA'}>
        {!isMaster && <p className="muted table-status">Mesa {TABLE_STATUS_LABEL[table.status].toLowerCase()}</p>}

        {!isMaster && (
          <div className="field">
            <Select
              label="Seu personagem nesta mesa"
              value={activeCharacterId}
              onChange={changeCharacter}
              options={characters.flatMap((c) => (c.id ? [{ value: String(c.id), label: c.name }] : []))}
            />
            {characterError && <p className="roll-error">{characterError}</p>}
            <button className="btn btn-danger leave-table" onClick={leaveTable} disabled={leaving}>
              {leaving ? 'Saindo...' : 'Sair da mesa'}
            </button>
            {leaveError && <p className="roll-error">{leaveError}</p>}
          </div>
        )}

        {isMaster && (
          <div className="master-actions">
            <Select
              label="Status da mesa"
              value={table.status}
              onChange={changeStatus}
              options={MASTER_STATUS_OPTIONS.map((option) => ({ value: option, label: TABLE_STATUS_LABEL[option] }))}
            />
            <button className="btn btn-danger" onClick={closeTable}>
              Encerrar mesa
            </button>
            {statusError && <p className="roll-error">{statusError}</p>}
          </div>
        )}

        {isMaster && id && <JoinRequestsPanel tableId={id} onAccepted={loadTable} />}

        </div>
        <div className="side-section" hidden={sideTab !== 'GENTE'}>
        {id && (
          <ParticipantsPanel
            tableId={id}
            participants={table.participants}
            onlineUserIds={onlineUsers.map((u) => String(u.userId))}
            isMaster={isMaster}
            onParticipantRemoved={setTable}
          />
        )}

        {isMaster && (
          <NpcLinksPanel
            characters={characters}
            linkedIds={table.npcIds.map(String)}
            canChange={connected}
            onLink={linkNpc}
            onUnlink={unlinkNpc}
          />
        )}

        </div>
        <div className="side-section" hidden={sideTab !== 'FICHAS'}>
        <PrivateSheetsPanel
          title={isMaster ? 'Meus personagens e NPCs' : 'Minha ficha'}
          characters={isMaster ? characters : activeCharacter ? [activeCharacter] : []}
          loading={charactersLoading}
          error={charactersError}
          emptyText={
            isMaster
              ? 'Você ainda não criou personagens ou NPCs.'
              : 'Selecione seu personagem nesta mesa para consultar a ficha.'
          }
          onEdit={(character) => setEditingSheet({ id: String(character.id) })}
          onCreate={isMaster ? () => setEditingSheet({}) : undefined}
        />

        {isMaster && (
          <StatusControls
            status={roomStatus}
            participants={table.participants}
            masterCharacters={characters}
            canSend={connected}
            onSet={chatService.setCharacterStatus.bind(chatService)}
            onRemove={chatService.removeCharacterStatus.bind(chatService)}
          />
        )}


        </div>
        <div className="side-section" hidden={sideTab !== 'DESAFIO'}>
        {isMaster && (
          <>
          <ChallengeMonsterRolls
            challenges={challenges}
            paused={Boolean(isPaused)}
            canSend={canSend}
            onRoll={chatService.rollMonsterChallenge.bind(chatService)}
          />
          <ChallengeMasterPanel
            challenges={challenges}
            participants={table.participants}
            targets={masterChallengeTargets}
            viewerId={viewerId}
            title="Desafios de CD"
            canSend={canSend}
            onCreate={chatService.createChallenge.bind(chatService)}
            onAllowExtraAttempt={chatService.allowExtraAttempt.bind(chatService)}
            onEndWithFailure={chatService.endChallengeWithFailure.bind(chatService)}
            onCancel={chatService.cancelChallenge.bind(chatService)}
            onSendMessage={chatService.sendChallengeMessage.bind(chatService)}
          />
          </>
        )}

        {!isMaster && (combat !== null || myCreatedChallenges.length > 0) && (
          <ChallengeMasterPanel
            challenges={myCreatedChallenges}
            participants={table.participants}
            targets={combatChallengeTargets}
            viewerId={viewerId}
            title="Meus desafios de combate"
            createHint={playerChallengeHint}
            canSend={canSend}
            onCreate={chatService.createChallenge.bind(chatService)}
            onAllowExtraAttempt={chatService.allowExtraAttempt.bind(chatService)}
            onEndWithFailure={chatService.endChallengeWithFailure.bind(chatService)}
            onCancel={chatService.cancelChallenge.bind(chatService)}
            onSendMessage={chatService.sendChallengeMessage.bind(chatService)}
          />
        )}

        {!isMaster && (
          <ChallengePlayerPanel
            challenges={challenges}
            userId={viewerId}
            character={activeCharacter}
            paused={Boolean(isPaused)}
            canSend={connected}
            incomingMessage={incomingMessage}
            onRoll={chatService.rollChallenge.bind(chatService)}
            onDismissMessage={dismissMessage}
          />
        )}

        </div>
        {isMaster && (
          <div className="side-section" hidden={sideTab !== 'COMBATE'}>
            <CombatStartPanel
              participants={table.participants}
              npcs={characters.filter((c) => c.kind !== 'PLAYER' && table.npcIds.includes(String(c.id)))}
              onlineUserIds={onlineUsers.map((u) => String(u.userId))}
              combatActive={combat !== null}
              canSend={canSend}
              onStart={chatService.startCombat.bind(chatService)}
            />
          </div>
        )}
        <div className="side-section" hidden={sideTab !== 'DADOS'}>
        <h3 className="section-title">Rolar dados</h3>
        <DiceRoller disabled={!canSend || rollBlocked} onRoll={sendRoll} />
        {rollBlocked && <p className="roll-error">{rollBlockedReason}</p>}
        <p className="muted roll-hint">
          Ou digite no chat: <code>/r 1d20+2</code>, <code>/r 1d20 vantagem</code>
        </p>

        {activeCharacter && (
          <>
            <h3 className="section-title">Teste de atributo</h3>
            <div className="quick-dice">
              {ATTRIBUTE_KEYS.map((key) => (
                <button
                  key={key}
                  className="btn btn-ghost"
                  onClick={() => rollAttribute(key)}
                  disabled={!canSend || rollBlocked}
                >
                  {key.slice(0, 3).toUpperCase()}
                </button>
              ))}
            </div>
          </>
        )}
        </div>
        </div>
      </aside>

      <div className="room-chat card">
        {sessionEndedReason && (
          <div className="session-ended">
            <span>{sessionEndedReason} O histórico do chat foi apagado.</span>
            <button className="btn btn-ghost" onClick={() => setConnectionAttempt((n) => n + 1)}>
              Conectar de novo
            </button>
          </div>
        )}
        {reconnecting && (
          <div className="connection-banner connection-banner--reconnecting" role="alert">
            <span>
              Conexão com a mesa perdida. Tentando reconectar ({connection.attempt} de {MAX_RECONNECT_ATTEMPTS})...
            </span>
            <button className="btn btn-ghost" onClick={() => chatService.reconnectNow()}>
              Tentar agora
            </button>
          </div>
        )}
        {connectionLost && (
          <div className="session-ended" role="alert">
            <span>Não foi possível reconectar à mesa. Verifique sua conexão e tente de novo.</span>
            <button className="btn btn-ghost" onClick={() => setConnectionAttempt((n) => n + 1)}>
              Tentar de novo
            </button>
          </div>
        )}
        {isPaused && !sessionEndedReason && (
          <div className="session-paused">
            A mesa está pausada pelo mestre. Mensagens e rolagens ficam bloqueadas até ela ser retomada.
          </div>
        )}
        {combat && (
          <CombatTracker
            combat={combat}
            viewerId={viewerId}
            isMaster={isMaster}
            canSend={canSend}
            onRollInitiative={chatService.rollInitiative.bind(chatService)}
            onRollMonsters={chatService.rollMonsterInitiative.bind(chatService)}
            onPassTurn={chatService.passTurn.bind(chatService)}
            onAllowReroll={chatService.allowInitiativeReroll.bind(chatService)}
            onEnd={chatService.endCombat.bind(chatService)}
          />
        )}
        <Tabs
          label="Canais da mesa"
          idPrefix={TABS_ID}
          active={activeChannel}
          onChange={selectChannel}
          tabs={ROOM_TABS.map((tab) => ({
            id: tab,
            label: TAB_LABEL[tab],
            icon: TAB_ICON[tab],
            badge: isChannel(tab) ? unread[tab] : undefined,
          }))}
        />
        <div className="messages" ref={scrollRef}>
          {CHANNELS.map((channel) => (
            <div
              key={channel}
              id={panelId(TABS_ID, channel)}
              role="tabpanel"
              aria-labelledby={tabId(TABS_ID, channel)}
              hidden={activeChannel !== channel}
              className="channel-panel"
            >
              {messages[channel].length === 0 && (
                <p className="muted center">{EMPTY_CHANNEL_TEXT[channel]}</p>
              )}
              {messages[channel].map((m, i) => (
                <div key={m.id ?? i} className={`msg msg--${m.type.toLowerCase()}`}>
                  <div className="msg-head">
                    <span className="author">{m.authorName}</span>
                    <span className="time">
                      {new Date(m.createdAt).toLocaleTimeString('pt-BR', {
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </span>
                  </div>

                  {m.type === 'ROLAGEM' && m.diceResult ? (
                    <DiceRollMessage
                      result={m.diceResult}
                      animate={!m.id || !historyIds.current.has(m.id)}
                    />
                  ) : m.type === 'IMAGEM' && m.image ? (
                    <button
                      className="chat-image-button"
                      onClick={() => setExpandedImage(m.image?.dataUrl ?? null)}
                      title="Ampliar imagem"
                    >
                      <img className="chat-image" src={m.image.dataUrl} alt={`Imagem enviada por ${m.authorName}`} />
                    </button>
                  ) : (
                    <p className="content">{m.content}</p>
                  )}
                </div>
              ))}
            </div>
          ))}
          <div
            id={panelId(TABS_ID, 'STATUS')}
            role="tabpanel"
            aria-labelledby={tabId(TABS_ID, 'STATUS')}
            hidden={activeChannel !== 'STATUS'}
            className="channel-panel"
          >
            <StatusView status={roomStatus} />
          </div>
          <div
            id={panelId(TABS_ID, 'ITENS')}
            role="tabpanel"
            aria-labelledby={tabId(TABS_ID, 'ITENS')}
            hidden={activeChannel !== 'ITENS'}
            className="channel-panel"
          >
            <SharedItemsView
              items={sharedItems}
              viewerId={viewerId}
              isMaster={isMaster}
              canSend={canSend}
              onCreate={chatService.createItem.bind(chatService)}
              onRemove={chatService.removeItem.bind(chatService)}
            />
          </div>
        </div>

        {activeChannel === 'CONVERSA' && (
          <>
            {composerError && <p className="roll-error composer-error">{composerError}</p>}
            <div className="composer">
              <input
                type="text"
                value={draft}
                onChange={(e) => {
                  setDraft(e.target.value);
                  setComposerError(null);
                }}
                onKeyUp={(e) => e.key === 'Enter' && canSend && sendDraft()}
                disabled={!canSend}
                placeholder={
                  !connected
                    ? 'Aguardando conexão com a mesa...'
                    : isPaused
                      ? 'Mesa pausada pelo mestre'
                      : 'Fale com a mesa ou role com /r 1d20+2'
                }
              />
              <button className="btn btn-primary" onClick={sendDraft} disabled={!canSend || !draft.trim()}>
                Enviar
              </button>
            </div>
          </>
        )}

        {activeChannel === 'IMAGENS' && (
          <ImageComposer disabled={!canSend} onSend={(dataUrl) => chatService.sendImage(dataUrl)} />
        )}
      </div>

      <aside className="room-notes card" aria-label="Caderno privado">
        <NotesPanel
          savedNote={note}
          canSave={connected}
          recipients={onlineUsers.filter((person) => String(person.userId) !== viewerId)}
          onSave={chatService.saveNote.bind(chatService)}
          onShare={chatService.shareNote.bind(chatService)}
        />
      </aside>

      {editingSheet && (
        <div className="sheet-editor-overlay" role="dialog" aria-modal="true">
          <div className="sheet-editor-dialog">
            <CharacterEditor
              id={editingSheet.id}
              embedded
              onSaved={handleSheetSaved}
              onCancel={() => setEditingSheet(null)}
            />
          </div>
        </div>
      )}

      {receivedNotes.length > 0 && (
        <ReceivedNoteDialog
          key={receivedNotes[0].id}
          note={receivedNotes[0]}
          remaining={receivedNotes.length - 1}
          onClose={dismissReceivedNote}
        />
      )}

      {reveal && <ChallengeRollOverlay key={reveal.key} reveal={reveal} viewerId={viewerId} onClose={dismissReveal} />}

      {expandedImage && (
        <div className="image-lightbox" role="dialog" aria-modal="true" onClick={() => setExpandedImage(null)}>
          <img src={expandedImage} alt="Imagem ampliada" />
          <button className="btn btn-ghost" onClick={() => setExpandedImage(null)}>
            Fechar
          </button>
        </div>
      )}
    </section>
  );
}
