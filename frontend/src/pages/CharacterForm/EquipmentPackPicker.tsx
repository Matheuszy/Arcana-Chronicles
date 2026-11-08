import { useEffect, useState } from 'react';
import Select from '../../components/ui/Select';
import { characterService } from '../../services/characterService';
import { Character, EquipmentPack } from '../../types/character';

interface EquipmentPackPickerProps {
  characterId?: string;
  appliedPack?: string | null;
  pendingPack?: string | null;
  dirty: boolean;
  onPendingChange: (pack: string | null) => void;
  onApplied: (character: Character) => void;
}

export default function EquipmentPackPicker({
  characterId,
  appliedPack,
  pendingPack,
  dirty,
  onPendingChange,
  onApplied,
}: EquipmentPackPickerProps) {
  const [packs, setPacks] = useState<EquipmentPack[]>([]);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [selected, setSelected] = useState(pendingPack ?? '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    characterService
      .listPacks()
      .then(setPacks)
      .catch((e: unknown) => setLoadError(e instanceof Error ? e.message : 'Não foi possível carregar os pacotes.'));
  }, []);

  const applied = packs.find((pack) => pack.chave === appliedPack);
  const preview = packs.find((pack) => pack.chave === selected);
  const creating = !characterId;

  const choose = (chave: string) => {
    setSelected(chave);
    setError(null);
    if (creating) onPendingChange(chave || null);
  };

  const apply = async () => {
    if (!characterId || !selected) return;
    setBusy(true);
    setError(null);
    try {
      onApplied(await characterService.applyPack(characterId, selected));
      setSelected('');
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Não foi possível aplicar o pacote.');
    } finally {
      setBusy(false);
    }
  };

  if (loadError) return <p className="text-list-warning">{loadError}</p>;

  return (
    <div className="equipment-pack">
      <h4 className="sheet-subtitle">Pacote de equipamento</h4>
      <p className="muted-hint">
        {applied
          ? `Pacote aplicado: ${applied.nome}. Trocar o pacote remove os itens dele que ainda estão na lista e mantém os que você adicionou.`
          : 'Escolha um dos pacotes do livro para preencher os itens de uma vez.'}
      </p>
      <div className="row add-item">
        <div className="field grow">
          <Select<string>
            label={applied ? 'Trocar por' : 'Pacote'}
            compact
            value={selected}
            placeholder="Nenhum"
            onChange={choose}
            options={[
              ...(creating ? [{ value: '', label: 'Nenhum' }] : []),
              ...packs.map((pack) => ({ value: pack.chave, label: pack.nome, disabled: pack.chave === appliedPack })),
            ]}
          />
        </div>
        {!creating && (
          <button
            type="button"
            className="btn btn-ghost"
            onClick={apply}
            disabled={!selected || busy || dirty}
            title={dirty ? 'Salve as alterações da ficha antes de aplicar um pacote.' : undefined}
          >
            {applied ? 'Trocar pacote' : 'Adicionar pacote'}
          </button>
        )}
      </div>
      {preview && (
        <p className="equipment-pack-preview">
          <strong>{preview.nome}:</strong> {preview.itens.join(', ')}
          {creating && ' · Os itens entram na ficha ao salvar.'}
        </p>
      )}
      {!creating && dirty && selected && (
        <p className="muted-hint">Salve as alterações da ficha antes de aplicar um pacote.</p>
      )}
      {error && (
        <p className="text-list-warning" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
