import { Box, Group } from '@mantine/core';

export type MiniProgressProps = {
  /** Toplam adım sayısı. */
  total: number;
  /** Tamamlanan adım sayısı. */
  done: number;
  color: string;
  /** Ekran okuyucu için metin, örn. "3 / 6 adım". */
  label: string;
};

/** Satır içi ilerleme göstergesi: küçük, renkli dilimler. */
export function MiniProgress({ total, done, color, label }: MiniProgressProps) {
  return (
    <Group gap={3} wrap="nowrap" role="img" aria-label={label}>
      {Array.from({ length: total }, (_, index) => (
        <Box
          key={index}
          data-testid="mini-progress-segment"
          w={14}
          h={4}
          bg={index < done ? `${color}.7` : 'gray.3'}
          style={{ borderRadius: 2 }}
        />
      ))}
    </Group>
  );
}
