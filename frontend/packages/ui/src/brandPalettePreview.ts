import type { MantineColorsTuple } from '@mantine/core';

/** Önizlemenin bir görünümü için renkler. Uygulamanın o anki görünümünden bağımsızdır; bu yüzden adla değil kodla yazılır. */
export type PreviewScheme = {
  readonly key: 'light' | 'dark';
  readonly surface: string;
  readonly frame: string;
  readonly line: string;
  readonly text: string;
  readonly dimmed: string;
  readonly activeRow: string;
  readonly button: string;
  readonly buttonText: string;
  /** Kurulum renginin bu görünümde kullanılan tonu (paletteki sıra). */
  readonly accentShade: number;
};

const LIGHT_ACCENT_SHADE = 7;
const DARK_ACCENT_SHADE = 2;

export const PREVIEW_SCHEMES: readonly PreviewScheme[] = [
  {
    key: 'light',
    surface: '#ffffff',
    frame: '#fafafa',
    line: '#e4e4e7',
    text: '#09090b',
    dimmed: '#71717a',
    activeRow: '#ededee',
    button: '#18181b',
    buttonText: '#ffffff',
    accentShade: LIGHT_ACCENT_SHADE,
  },
  {
    key: 'dark',
    surface: '#09090b',
    frame: '#0c0c0e',
    line: '#27272a',
    text: '#fafafa',
    dimmed: '#a1a1aa',
    activeRow: '#1f1f22',
    button: '#fafafa',
    buttonText: '#18181b',
    accentShade: DARK_ACCENT_SHADE,
  },
];

export function accentOf(palette: MantineColorsTuple, scheme: PreviewScheme): string {
  return palette[scheme.accentShade] ?? scheme.text;
}

/** Hazır renkler: açık zeminde okunur, kurumsal tonlar. */
export const PRESET_COLORS = [
  { key: 'navy', value: '#17527a' },
  { key: 'green', value: '#1f6b45' },
  { key: 'teal', value: '#0f6b6b' },
  { key: 'burgundy', value: '#8a1f33' },
  { key: 'purple', value: '#5b3a9c' },
  { key: 'orange', value: '#b4540a' },
  { key: 'gray', value: '#3f3f46' },
] as const;
