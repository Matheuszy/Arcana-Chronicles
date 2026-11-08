import { ReactNode } from 'react';
import { ReleaseNoteItem, ReleaseNotes } from '../../releaseNotes';
import './ReleasePoster.css';

interface ReleasePosterProps {
  notes: ReleaseNotes;
  headingId?: string;
  compact?: boolean;
  actions?: ReactNode;
}

const formatDate = (date: string) =>
  new Date(`${date}T12:00:00`).toLocaleDateString('pt-BR', { day: 'numeric', month: 'long', year: 'numeric' });

function NoteList({ label, items }: { label: string; items: ReleaseNoteItem[] }) {
  if (items.length === 0) return null;
  return (
    <section className="poster-section">
      <h3 className="poster-section-title">
        <span aria-hidden="true">★</span> {label} <span aria-hidden="true">★</span>
      </h3>
      <ul className="poster-list">
        {items.map((item) => (
          <li key={item.title}>
            <strong>{item.title}.</strong> {item.text}
          </li>
        ))}
      </ul>
    </section>
  );
}

export default function ReleasePoster({ notes, headingId, compact = false, actions }: ReleasePosterProps) {
  return (
    <article className={`release-poster ${compact ? 'release-poster--compact' : ''}`} aria-labelledby={headingId}>
      <span className="poster-nail poster-nail--left" aria-hidden="true" />
      <span className="poster-nail poster-nail--right" aria-hidden="true" />
      <p className="poster-kicker">Extra! Extra!</p>
      <h2 className="poster-headline" id={headingId}>
        Novidades na cidade
      </h2>
      <p className="poster-title">{notes.title}</p>
      <p className="poster-meta">
        Versão {notes.version} · {formatDate(notes.date)}
      </p>
      <div className="poster-rule" aria-hidden="true" />
      <p className="poster-intro">{notes.intro}</p>
      <NoteList label="Novidades" items={notes.news} />
      <NoteList label="Melhorias" items={notes.improvements} />
      <NoteList label="Correções" items={notes.fixes} />
      <p className="poster-signature">Assinado: o xerife do Arcana</p>
      {actions && <div className="poster-actions">{actions}</div>}
    </article>
  );
}
