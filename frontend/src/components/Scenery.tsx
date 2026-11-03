export type SceneryVariant = 'portal' | 'mapa' | 'grimorio' | 'estalagem' | 'mesa';

const EMBER_COUNT = 12;
const SCENES_WITH_EMBERS: SceneryVariant[] = ['portal', 'mapa'];

export function sceneryForPath(pathname: string): SceneryVariant {
  if (/^\/mesas\/[^/]+/.test(pathname)) return 'mesa';
  if (pathname.startsWith('/mesas')) return 'mapa';
  if (pathname.startsWith('/fichas')) return 'grimorio';
  if (pathname.startsWith('/conta')) return 'estalagem';
  return 'portal';
}

export default function Scenery({ variant }: { variant: SceneryVariant }) {
  return (
    <div className={`scenery scenery--${variant}`} aria-hidden="true">
      <div className="scenery-motif" />
      {SCENES_WITH_EMBERS.includes(variant) && (
        <div className="scenery-embers">
          {Array.from({ length: EMBER_COUNT }, (_, index) => (
            <span key={index} className="scenery-ember" />
          ))}
        </div>
      )}
    </div>
  );
}
