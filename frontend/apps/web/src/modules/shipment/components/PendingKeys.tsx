import { useTranslation } from '@iplik/i18n';
import { CloseButton, Group, Paper, Text } from '@iplik/ui';

/** Atanmayı bekleyen çuval anahtarları; her biri listeden çıkarılabilir. */
export function PendingKeys({
  keys,
  onRemove,
}: {
  keys: readonly string[];
  onRemove: (key: string) => void;
}) {
  const { t } = useTranslation();
  return keys.map((key) => (
    <Paper key={key} withBorder px="sm" py={4}>
      <Group justify="space-between">
        <Text>{key}</Text>
        <CloseButton
          aria-label={t('shipment.bags.assign.remove', { key })}
          onClick={() => {
            onRemove(key);
          }}
        />
      </Group>
    </Paper>
  ));
}
