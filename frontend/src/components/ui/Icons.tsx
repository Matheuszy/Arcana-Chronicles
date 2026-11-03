import { ReactNode } from 'react';

interface IconProps {
  className?: string;
  title?: string;
}

function Icon({ className, title, children }: IconProps & { children: ReactNode }) {
  return (
    <svg
      className={`rpg-icon ${className ?? ''}`}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.6}
      strokeLinecap="round"
      strokeLinejoin="round"
      role={title ? 'img' : undefined}
      aria-hidden={title ? undefined : true}
      aria-label={title}
    >
      {children}
    </svg>
  );
}

export function D20Icon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M12 2l9 5v10l-9 5-9-5V7z" />
      <path d="M12 8.5l5 7.5H7z" />
      <path d="M3 7l9 1.5L21 7M3 7l4 9-4 1M21 7l-4 9 4 1M7 16l5 6 5-6" />
    </Icon>
  );
}

export function QuillIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M20 3C12 4 7.5 9 6 17" />
      <path d="M20 3c.5 6-3.5 11.5-11 13.5" />
      <path d="M9.5 9.5l3 1M8 13l3.5.5" />
      <path d="M6 17l-2 4" />
    </Icon>
  );
}

export function HelmIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M4.5 19v-6a7.5 7.5 0 0 1 15 0v6h-4.5v-4.5h-6V19z" />
      <path d="M12 5.5v9" />
      <path d="M8 11h8" />
    </Icon>
  );
}

export function SwordShieldIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M13 4l6 2.5V11c0 4.5-2.6 7.4-6 9-3.4-1.6-6-4.5-6-9V6.5z" />
      <path d="M3 3l9 9M5 9l4-4M10.5 12.5l1.5 1.5" />
    </Icon>
  );
}

export function SeerShieldIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M12 3l7 3v5c0 5-3 8.2-7 10-4-1.8-7-5-7-10V6z" />
      <path d="M7.5 11.5c2.5-3 6.5-3 9 0-2.5 3-6.5 3-9 0z" />
      <circle cx="12" cy="11.5" r="1.3" fill="currentColor" stroke="none" />
    </Icon>
  );
}

export function SkullIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M12 3a7 7 0 0 0-7 7c0 2.6 1.3 4.4 3 5.4V19h8v-3.6c1.7-1 3-2.8 3-5.4a7 7 0 0 0-7-7z" />
      <circle cx="9.2" cy="10.5" r="1.5" fill="currentColor" stroke="none" />
      <circle cx="14.8" cy="10.5" r="1.5" fill="currentColor" stroke="none" />
      <path d="M10.5 19v-2M13.5 19v-2" />
    </Icon>
  );
}

export function CrownIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M3.5 8l4.5 4 4-6.5 4 6.5 4.5-4-2 10.5h-13z" />
      <path d="M6.5 21h11" />
    </Icon>
  );
}

export function OpenHandIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M8 13V6a1.5 1.5 0 0 1 3 0v5" />
      <path d="M11 10.5V4.5a1.5 1.5 0 0 1 3 0v6" />
      <path d="M14 10.5V6a1.5 1.5 0 0 1 3 0v7c0 4.2-2.8 8-6.8 8-2.6 0-4.4-1.6-5.4-3.8L3.4 13a1.5 1.5 0 0 1 2.6-1.4L8 14" />
    </Icon>
  );
}

export function CrossedSwordsIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M4 4l11 11M13 17l4-4M16 16l3.5 3.5" />
      <path d="M20 4L9 15M11 17l-4-4M8 16l-3.5 3.5" />
    </Icon>
  );
}

export function TorchIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path className="rpg-flame" d="M12 2.5c2.2 2 3.3 3.7 3.3 5.6a3.3 3.3 0 0 1-6.6 0c0-1.3.6-2.3 1.6-3.3.2 1 .7 1.6 1.4 1.8-.3-1.6-.1-2.8.3-4.1z" fill="currentColor" stroke="none" />
      <path d="M8 11.5h8l-1.3 2H9.3z" />
      <path d="M10 13.5l1 8h2l1-8" />
    </Icon>
  );
}

export function HourglassIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M6 3h12M6 21h12" />
      <path d="M7.5 3c0 5 9 5 9 9s-9 4-9 9M16.5 3c0 5-9 5-9 9s9 4 9 9" />
    </Icon>
  );
}

export function DoorIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M4 21h16" />
      <path d="M6 21V5.5A2.5 2.5 0 0 1 8.5 3h7A2.5 2.5 0 0 1 18 5.5V21" />
      <circle cx="14.5" cy="12.5" r="0.9" fill="currentColor" stroke="none" />
    </Icon>
  );
}

export function LockIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <rect x="5" y="10.5" width="14" height="10" rx="1.5" />
      <path d="M8 10.5V7.5a4 4 0 0 1 8 0v3" />
      <path d="M12 14.5v2.5" />
    </Icon>
  );
}

export function CandleIcon({ lit, ...props }: IconProps & { lit: boolean }) {
  return (
    <Icon {...props}>
      {lit ? (
        <path className="rpg-flame" d="M12 2.5c1.8 1.8 2.6 3.2 2.6 4.6a2.6 2.6 0 0 1-5.2 0c0-1.4.8-2.8 2.6-4.6z" fill="currentColor" stroke="none" />
      ) : (
        <path d="M12 8c-1-1.2 1-2 0-3.3s1-2 .4-2.7" />
      )}
      <path d="M12 9.5v1.5" />
      <rect x="8.5" y="11" width="7" height="8.5" rx="1" />
      <path d="M6 21h12" />
    </Icon>
  );
}
