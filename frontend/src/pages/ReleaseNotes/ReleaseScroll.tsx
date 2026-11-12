import { TransitionEvent, useEffect, useRef } from 'react';
import ReleasePoster from '../../components/releaseNotes/ReleasePoster';
import { ReleaseNotes } from '../../releaseNotes';

interface ReleaseScrollProps {
  notes: ReleaseNotes;
  open: boolean;
  onToggle: () => void;
}

const formatDate = (date: string) =>
  new Date(`${date}T12:00:00`).toLocaleDateString('pt-BR', { day: 'numeric', month: 'long', year: 'numeric' });

const prefersReducedMotion = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches;

export default function ReleaseScroll({ notes, open, onToggle }: ReleaseScrollProps) {
  const scrollRef = useRef<HTMLElement>(null);
  const bodyId = `release-scroll-body-${notes.version}`;

  const bringIntoView = (behavior: ScrollBehavior) => {
    scrollRef.current?.scrollIntoView({ behavior, block: 'start' });
  };

  useEffect(() => {
    if (open && prefersReducedMotion()) bringIntoView('auto');
  }, [open]);

  const handleUnrolled = (event: TransitionEvent<HTMLDivElement>) => {
    if (!open || event.target !== event.currentTarget || event.propertyName !== 'grid-template-rows') return;
    bringIntoView('smooth');
  };

  return (
    <article ref={scrollRef} className={`release-scroll ${open ? 'is-open' : ''}`}>
      <button
        type="button"
        className="scroll-head"
        aria-expanded={open}
        aria-controls={bodyId}
        onClick={onToggle}
      >
        <span className="scroll-rod" aria-hidden="true" />
        <span className="scroll-label">
          <span className="scroll-version">Versão {notes.version}</span>
          <span className="scroll-title">{notes.title}</span>
          <span className="scroll-date">{formatDate(notes.date)}</span>
          <span className="scroll-hint">
            {open ? 'Enrolar pergaminho' : 'Desenrolar pergaminho'}
            <span className="scroll-hint-arrow" aria-hidden="true">▾</span>
          </span>
        </span>
      </button>
      <div id={bodyId} className="scroll-body" onTransitionEnd={handleUnrolled}>
        <div className="scroll-body-inner">
          <ReleasePoster notes={notes} headingId={`release-${notes.version}`} compact />
        </div>
      </div>
      <span className="scroll-rod scroll-rod--bottom" aria-hidden="true" />
    </article>
  );
}
