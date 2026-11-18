import { Box, Text } from '@iplik/ui';

/** Toplam net kg: girintili kutuda tek satır. */
export function TotalBox({ text }: { text: string }) {
  return (
    <Box
      p="sm"
      style={{
        background: 'var(--app-inset)',
        border: '1px solid var(--app-line)',
        borderRadius: 'var(--mantine-radius-md)',
      }}
    >
      <Text fw={600} fz="lg" data-testid="recipe-total">
        {text}
      </Text>
    </Box>
  );
}
