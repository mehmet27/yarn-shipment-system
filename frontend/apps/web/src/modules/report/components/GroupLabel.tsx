import { Text } from '@iplik/ui';

/** Aynı kartta ikinci bir grubu başlatan girintili ince şerit. */
export function GroupLabel({ children }: { children: string }) {
  return (
    <Text
      fw={600}
      size="sm"
      px="md"
      py="xs"
      bg="var(--app-inset)"
      style={{
        borderBlock: '1px solid var(--app-line)',
      }}
    >
      {children}
    </Text>
  );
}
