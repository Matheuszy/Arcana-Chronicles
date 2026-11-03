import { PRESENCE_LABEL, PresenceState } from '../../types/friend';
import { CandleIcon } from './Icons';

export default function PresenceCandle({ state }: { state: PresenceState }) {
  return (
    <span className={`presence-candle presence-candle--${state.toLowerCase()}`}>
      <CandleIcon lit={state !== 'OFFLINE'} />
      {PRESENCE_LABEL[state]}
    </span>
  );
}
