import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { characterService } from '../../services/characterService';
import {
  attributeModifier,
  Attributes,
  Character,
  CharacterKind,
  EMPTY_ATTRIBUTES,
} from '../../types/character';
import './CharacterForm.css';

const ATTRIBUTE_KEYS: (keyof Attributes)[] = [
  'forca',
  'destreza',
  'constituicao',
  'inteligencia',
  'sabedoria',
  'carisma',
];

const EMPTY_CHARACTER: Character = {
  name: '',
  kind: 'PLAYER',
  level: 1,
  hpMax: 10,
  hpCurrent: 10,
  armorClass: 10,
  attributes: { ...EMPTY_ATTRIBUTES },
  backstory: '',
  personalityPrompt: '',
};

export default function CharacterForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [form, setForm] = useState<Character>(EMPTY_CHARACTER);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!id) return;
    characterService.getById(id).then(setForm);
  }, [id]);

  const isNpcOrMonster = form.kind !== 'PLAYER';

  const setField = <K extends keyof Character>(key: K, value: Character[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const setAttribute = (key: keyof Attributes, value: number) =>
    setForm((prev) => ({ ...prev, attributes: { ...prev.attributes, [key]: value } }));

  const modifierFor = (key: keyof Attributes) => {
    const mod = attributeModifier(form.attributes[key]);
    return mod >= 0 ? `+${mod}` : `${mod}`;
  };

  const save = async () => {
    if (!form.name.trim()) {
      alert('Dê um nome ao personagem antes de salvar.');
      return;
    }
    setSaving(true);
    try {
      if (id) {
        await characterService.update(id, form);
      } else {
        await characterService.create(form);
      }
      navigate('/fichas');
    } catch (e) {
      setSaving(false);
    }
  };

  return (
    <section className="page">
      <p className="eyebrow">{id ? 'Editando ficha' : 'Nova ficha'}</p>
      <h1>{id ? 'Ajustar personagem' : 'Forjar novo personagem'}</h1>

      <form
        className="form card"
        onSubmit={(e) => {
          e.preventDefault();
          save();
        }}
      >
        <div className="row">
          <div className="field grow">
            <label htmlFor="name">Nome</label>
            <input
              id="name"
              type="text"
              value={form.name}
              onChange={(e) => setField('name', e.target.value)}
              placeholder="ex: Arannis, o Feiticeiro"
            />
          </div>

          <div className="field">
            <label htmlFor="kind">Tipo</label>
            <select
              id="kind"
              value={form.kind}
              onChange={(e) => setField('kind', e.target.value as CharacterKind)}
            >
              <option value="PLAYER">Jogador</option>
              <option value="NPC_FRIENDLY">NPC Aliado</option>
              <option value="MONSTER">Monstro</option>
              <option value="BOSS">Chefe de Fase</option>
            </select>
          </div>
        </div>

        <div className="row">
          <div className="field">
            <label htmlFor="level">Nível</label>
            <input
              id="level"
              type="number"
              min={1}
              value={form.level}
              onChange={(e) => setField('level', Number(e.target.value))}
            />
          </div>
          <div className="field">
            <label htmlFor="hpMax">HP máximo</label>
            <input
              id="hpMax"
              type="number"
              min={1}
              value={form.hpMax}
              onChange={(e) => setField('hpMax', Number(e.target.value))}
            />
          </div>
          <div className="field">
            <label htmlFor="hpCurrent">HP atual</label>
            <input
              id="hpCurrent"
              type="number"
              min={0}
              value={form.hpCurrent}
              onChange={(e) => setField('hpCurrent', Number(e.target.value))}
            />
          </div>
          <div className="field">
            <label htmlFor="armorClass">Classe de Armadura</label>
            <input
              id="armorClass"
              type="number"
              min={0}
              value={form.armorClass}
              onChange={(e) => setField('armorClass', Number(e.target.value))}
            />
          </div>
        </div>

        <h3 className="section-title">Atributos</h3>
        <div className="attrs-grid">
          {ATTRIBUTE_KEYS.map((key) => (
            <div className="field attr-field" key={key}>
              <label htmlFor={key}>{key}</label>
              <input
                id={key}
                type="number"
                min={1}
                max={30}
                value={form.attributes[key]}
                onChange={(e) => setAttribute(key, Number(e.target.value))}
              />
              <span className="modifier">{modifierFor(key)}</span>
            </div>
          ))}
        </div>

        <div className="field">
          <label htmlFor="backstory">História / descrição</label>
          <textarea
            id="backstory"
            rows={3}
            value={form.backstory}
            onChange={(e) => setField('backstory', e.target.value)}
            placeholder="Quem é esse personagem?"
          />
        </div>

        {isNpcOrMonster && (
          <div className="field">
            <label htmlFor="personalityPrompt">Personalidade para a IA (usada no chat da mesa)</label>
            <textarea
              id="personalityPrompt"
              rows={3}
              value={form.personalityPrompt}
              onChange={(e) => setField('personalityPrompt', e.target.value)}
              placeholder="ex: Fale como um anão rabugento, desconfiado de forasteiros, que só ajuda por moedas de ouro."
            />
          </div>
        )}

        <div className="actions">
          <button type="button" className="btn btn-ghost" onClick={() => navigate('/fichas')}>
            Cancelar
          </button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Salvando...' : 'Salvar ficha'}
          </button>
        </div>
      </form>
    </section>
  );
}
