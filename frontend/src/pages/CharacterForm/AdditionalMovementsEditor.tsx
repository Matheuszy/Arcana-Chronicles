import Select from '../../components/ui/Select';
import NumberInput from '../../components/ui/NumberInput';
import {
  AdditionalMovement,
  MAX_ADDITIONAL_MOVEMENTS,
  MAX_MOVEMENT,
  MAX_MOVEMENT_ORIGIN_LENGTH,
  MOVEMENT_TYPE_LABEL,
  MOVEMENT_TYPES,
  MovementType,
} from '../../types/character';

interface AdditionalMovementsEditorProps {
  movements: AdditionalMovement[];
  onChange: (movements: AdditionalMovement[]) => void;
}

export default function AdditionalMovementsEditor({ movements, onChange }: AdditionalMovementsEditorProps) {
  const update = (index: number, changes: Partial<AdditionalMovement>) =>
    onChange(movements.map((movement, i) => (i === index ? { ...movement, ...changes } : movement)));

  const add = () => onChange([...movements, { tipo: 'VOO', metros: 9, origem: '', temporario: false }]);

  const remove = (index: number) => onChange(movements.filter((_, i) => i !== index));

  return (
    <div className="additional-movements">
      <h4 className="sheet-subtitle">Deslocamentos adicionais</h4>
      {movements.length === 0 && <p className="muted-hint">Nenhum deslocamento além do base.</p>}
      {movements.map((movement, index) => (
        <div key={index} className="row movement-row">
          <div className="field">
            <Select<MovementType>
              label="Tipo"
              compact
              required
              value={movement.tipo}
              onChange={(tipo) => update(index, { tipo })}
              options={MOVEMENT_TYPES.map((tipo) => ({ value: tipo, label: MOVEMENT_TYPE_LABEL[tipo] }))}
            />
          </div>
          <div className="field">
            <label htmlFor={`movement-meters-${index}`} className="required-mark">Metros</label>
            <NumberInput
              id={`movement-meters-${index}`}
              required
              min={0}
              max={MAX_MOVEMENT}
              step={1}
              value={movement.metros}
              onChange={(metros) => update(index, { metros })}
            />
          </div>
          <div className="field grow">
            <label htmlFor={`movement-origin-${index}`}>Origem</label>
            <input
              id={`movement-origin-${index}`}
              type="text"
              maxLength={MAX_MOVEMENT_ORIGIN_LENGTH}
              value={movement.origem ?? ''}
              onChange={(e) => update(index, { origem: e.target.value })}
              placeholder="ex: Magia Voo, Botas de escalada"
            />
          </div>
          <label className="movement-temporary">
            <input
              type="checkbox"
              checked={movement.temporario}
              onChange={(e) => update(index, { temporario: e.target.checked })}
            />
            Temporário
          </label>
          <button
            type="button"
            className="btn-remove"
            onClick={() => remove(index)}
            aria-label={`Remover deslocamento de ${MOVEMENT_TYPE_LABEL[movement.tipo]}`}
          >
            ✕
          </button>
        </div>
      ))}
      <button
        type="button"
        className="btn btn-ghost"
        onClick={add}
        disabled={movements.length >= MAX_ADDITIONAL_MOVEMENTS}
      >
        + Adicionar deslocamento ({movements.length}/{MAX_ADDITIONAL_MOVEMENTS})
      </button>
    </div>
  );
}
