import ReleasePoster from '../../components/releaseNotes/ReleasePoster';
import { RELEASE_NOTES } from '../../releaseNotes';
import './ReleaseNotes.css';

export default function ReleaseNotesPage() {
  return (
    <section className="page release-notes-page">
      <header className="page-head">
        <p className="eyebrow">Mural da cidade</p>
        <h1>Notas de atualização</h1>
        <p className="muted">Tudo o que chegou de novo no Arcana, da versão mais recente para a mais antiga.</p>
      </header>
      <div className="release-notes-board">
        {RELEASE_NOTES.map((notes) => (
          <ReleasePoster
            key={notes.version}
            notes={notes}
            headingId={`release-${notes.version}`}
            compact
          />
        ))}
      </div>
    </section>
  );
}
