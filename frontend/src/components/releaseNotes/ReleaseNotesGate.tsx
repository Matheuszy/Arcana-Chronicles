import { useCallback, useEffect, useRef, useState } from 'react';
import { LATEST_RELEASE } from '../../releaseNotes';
import { releaseNotesService } from '../../services/releaseNotesService';
import ReleasePoster from './ReleasePoster';

interface ReleaseNotesGateProps {
  userId: number;
  paused: boolean;
}

export default function ReleaseNotesGate({ userId, paused }: ReleaseNotesGateProps) {
  const [pending, setPending] = useState(false);
  const closeRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    let active = true;
    releaseNotesService
      .lastSeen()
      .then((version) => {
        if (active) setPending(version !== LATEST_RELEASE.version);
      })
      .catch(() => undefined);
    return () => {
      active = false;
    };
  }, [userId]);

  const visible = pending && !paused;

  const close = useCallback(() => {
    setPending(false);
    releaseNotesService.markSeen(LATEST_RELEASE.version).catch(() => undefined);
  }, []);

  useEffect(() => {
    if (!visible) return;
    closeRef.current?.focus({ preventScroll: true });
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') close();
    };
    document.addEventListener('keydown', onKey);
    const overflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = overflow;
    };
  }, [visible, close]);

  if (!visible) return null;

  return (
    <div className="poster-overlay" role="dialog" aria-modal="true" aria-labelledby="release-poster-title">
      <div className="poster-overlay-scroll">
        <ReleasePoster
          notes={LATEST_RELEASE}
          headingId="release-poster-title"
          actions={
            <button ref={closeRef} type="button" className="poster-button" onClick={close}>
              Entendido, parceiro!
            </button>
          }
        />
      </div>
    </div>
  );
}
