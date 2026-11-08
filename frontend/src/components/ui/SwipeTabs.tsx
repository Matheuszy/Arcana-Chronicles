import { ReactNode, TouchEvent, useEffect, useId, useRef } from 'react';
import Tabs, { panelId, tabId, TabDefinition } from './Tabs';

interface SwipeTabsProps<T extends string> {
  tabs: TabDefinition<T>[];
  active: T;
  onChange: (id: T) => void;
  label: string;
  idPrefix?: string;
  className?: string;
  children: ReactNode;
}

const SWIPE_DISTANCE = 60;
const SWIPE_IGNORED = 'input, textarea, select, button, a, [contenteditable="true"], [data-no-swipe]';

export default function SwipeTabs<T extends string>({
  tabs,
  active,
  onChange,
  label,
  idPrefix,
  className = '',
  children,
}: SwipeTabsProps<T>) {
  const generated = useId();
  const group = idPrefix ?? generated;
  const barRef = useRef<HTMLDivElement>(null);
  const touchStart = useRef<{ x: number; y: number } | null>(null);
  const activeIndex = tabs.findIndex((tab) => tab.id === active);
  const shownIndex = useRef(activeIndex);
  const direction = useRef<'next' | 'previous' | null>(null);
  if (shownIndex.current !== activeIndex) {
    direction.current = activeIndex > shownIndex.current ? 'next' : 'previous';
    shownIndex.current = activeIndex;
  }
  const firstRender = useRef(true);

  useEffect(() => {
    if (firstRender.current) {
      firstRender.current = false;
      return;
    }
    const bar = barRef.current;
    bar?.querySelector<HTMLElement>('[aria-selected="true"]')?.scrollIntoView({ block: 'nearest', inline: 'center' });
    if (bar && bar.getBoundingClientRect().top < 0) bar.scrollIntoView({ block: 'start' });
  }, [activeIndex]);

  const goTo = (index: number) => {
    const target = tabs[index];
    if (target) onChange(target.id);
  };

  const startSwipe = (event: TouchEvent<HTMLDivElement>) => {
    const target = event.target as HTMLElement;
    if (event.touches.length !== 1 || target.closest(SWIPE_IGNORED)) {
      touchStart.current = null;
      return;
    }
    touchStart.current = { x: event.touches[0].clientX, y: event.touches[0].clientY };
  };

  const endSwipe = (event: TouchEvent<HTMLDivElement>) => {
    const start = touchStart.current;
    touchStart.current = null;
    if (!start) return;
    const dx = event.changedTouches[0].clientX - start.x;
    const dy = event.changedTouches[0].clientY - start.y;
    if (Math.abs(dx) < SWIPE_DISTANCE || Math.abs(dx) < Math.abs(dy) * 1.5) return;
    goTo(dx < 0 ? activeIndex + 1 : activeIndex - 1);
  };

  return (
    <div className={`swipe-tabs ${className}`.trim()}>
      <div className="swipe-tabs-bar" ref={barRef}>
        <Tabs tabs={tabs} active={active} onChange={onChange} label={label} idPrefix={group} />
        <p className="swipe-tabs-position" aria-hidden="true">
          {activeIndex + 1} de {tabs.length} · deslize para trocar de aba
        </p>
      </div>
      <div
        key={active}
        id={panelId(group, active)}
        role="tabpanel"
        aria-labelledby={tabId(group, active)}
        className={`swipe-tabs-panel ${direction.current ? `swipe-tabs-panel--${direction.current}` : ''}`}
        onTouchStart={startSwipe}
        onTouchEnd={endSwipe}
        onTouchCancel={() => {
          touchStart.current = null;
        }}
      >
        {children}
      </div>
    </div>
  );
}
