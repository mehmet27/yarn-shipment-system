import { useTranslation } from '@iplik/i18n';
import { Button, Group, Paper, Stack, Tabs, Text } from '@iplik/ui';
import { useState } from 'react';
import type { RecipeLineDraft } from './createDraft';
import { lineError, priceValid } from './createDraft';
import { OrderFieldGrid } from './OrderFieldGrid';
import { YarnTabFields } from './YarnTabFields';

type RecipeLineRowProps = {
  line: RecipeLineDraft;
  index: number;
  removable: boolean;
  showErrors: boolean;
  onPatch: (patch: Partial<RecipeLineDraft>) => void;
  onRemove: () => void;
};

/** Bir reçete satırı iki sekmedir: İplik (cins, lot, net kg) ve Sipariş (Excel formundaki kumaş/LYC/en/fiyat). */
export function RecipeLineRow({
  line,
  index,
  removable,
  showErrors,
  onPatch,
  onRemove,
}: RecipeLineRowProps) {
  const { t } = useTranslation();
  const error = showErrors ? lineError(line) : undefined;
  const [tab, setTab] = useState<string>('yarn');
  const active = showErrors && !priceValid(line.unitPrice) ? 'order' : tab;
  return (
    <Paper withBorder p="sm" radius="md">
      <Stack gap="xs">
        <Group justify="space-between" wrap="nowrap">
          <Text fw={600}>{t('create.recipe.lineTitle', { index: index + 1 })}</Text>
          {removable && (
            <Button variant="subtle" color="red" size="compact-sm" onClick={onRemove}>
              {t('create.recipe.remove', { index: index + 1 })}
            </Button>
          )}
        </Group>
        <Tabs
          value={active}
          onChange={(value) => {
            setTab(value ?? 'yarn');
          }}
        >
          <Tabs.List>
            <Tabs.Tab value="yarn">{t('create.recipe.tabYarn')}</Tabs.Tab>
            <Tabs.Tab value="order">{t('create.recipe.tabOrder')}</Tabs.Tab>
          </Tabs.List>
          <Tabs.Panel value="yarn" pt="xs">
            <YarnTabFields line={line} index={index} error={error} onPatch={onPatch} />
          </Tabs.Panel>
          <Tabs.Panel value="order" pt="xs">
            <OrderFieldGrid line={line} showErrors={showErrors} onPatch={onPatch} />
          </Tabs.Panel>
        </Tabs>
      </Stack>
    </Paper>
  );
}
