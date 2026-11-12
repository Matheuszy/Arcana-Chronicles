import { useState } from 'react';
import { RELEASE_NOTES } from '../../releaseNotes';
import ReleaseScroll from './ReleaseScroll';
import './ReleaseNotes.css';

export default function ReleaseNotesPage() {
  const [openVersion, setOpenVersion] = useState<string | null>(null);

  const toggle = (version: string) => {
    setOpenVersion((current) => (current === version ? null : version));
  };

  return (
    <section className="page release-notes-page">
      <header className="page-head">
        <p className="eyebrow">Mural da cidade</p>
        <h1>Notas de atualização</h1>
        <p className="muted">
          Tudo o que chegou de novo no Arcana, da versão mais recente para a mais antiga. Toque num pergaminho para
          desenrolá-lo.
        </p>
      </header>
      <div className="release-notes-board">
        {RELEASE_NOTES.map((notes) => (
          <ReleaseScroll
            key={notes.version}
            notes={notes}
            open={openVersion === notes.version}
            onToggle={() => toggle(notes.version)}
          />
        ))}
      </div>
    </section>
  );
}
