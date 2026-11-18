import { Text } from '@iplik/ui';

/** Toplam net kg: özet kartının başında tek satır (kart zaten çerçeveli, ek kutu yok). */
export function TotalBox({ text }: { text: string }) {
  return (
    <Text fw={600} fz="xl" data-testid="recipe-total">
      {text}
    </Text>
  );
}
