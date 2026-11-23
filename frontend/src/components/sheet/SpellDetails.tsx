import { Spell } from '../../types/character';
import './SpellDetails.css';

interface SpellDetailsProps {
  spell: Spell;
}

export default function SpellDetails({ spell }: SpellDetailsProps) {
  const components = [spell.components, spell.material && `(${spell.material})`].filter(Boolean).join(' ');
  const duration = [spell.concentration ? 'Concentração' : null, spell.duration].filter(Boolean).join(', ');
  const facts = [
    { label: 'Escola', value: spell.school },
    { label: 'Tempo de conjuração', value: spell.castingTime },
    { label: 'Alcance', value: spell.range },
    { label: 'Componentes', value: components },
    { label: 'Duração', value: duration },
    { label: 'Ritual', value: spell.ritual ? 'Sim' : null },
    { label: 'Classes', value: spell.classes },
  ].filter((fact) => fact.value);

  return (
    <div className="spell-details">
      {facts.length > 0 && (
        <dl className="spell-facts">
          {facts.map((fact) => (
            <div key={fact.label} className="spell-fact">
              <dt>{fact.label}</dt>
              <dd>{fact.value}</dd>
            </div>
          ))}
        </dl>
      )}
      {spell.description && <p className="spell-text">{spell.description}</p>}
      {spell.higherLevels && (
        <p className="spell-text">
          <strong>Em níveis superiores. </strong>
          {spell.higherLevels}
        </p>
      )}
    </div>
  );
}
