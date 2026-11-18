import { useTranslation } from '@iplik/i18n';
import { Box, Button, Group, Stack, Tabs, Text } from '@iplik/ui';
import { useState } from 'react';
import type { RecipeLineDraft } from './createDraft';
import { lineError } from './createDraft';
import { OrderFieldGrid } from './OrderFieldGrid';
import { YarnTabFields } from './YarnTabFields';

/** Sekme içeriği ek bir kutu açmaz: tema çerçevesi bu satırda kapatılır, alanlar doğrudan kartta durur. */
const PANEL_STYLE = {
  border: 0,
  borderRadius: 0,
  background: 'transparent',
  padding: 0,
  marginTop: 'var(--mantine-spacing-sm)',
} as const;

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
  // Sipariş sekmesi yalnızca tek eksik fiyat olduğunda öne alınır; iplik/kilo hatası varsa İplik sekmesi görünür kalır.
  const active = error === 'linePriceInvalid' ? 'order' : tab;
  return (
    <Box data-testid="recipe-line">
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
          <Tabs.Panel value="yarn" style={PANEL_STYLE}>
            <YarnTabFields line={line} index={index} error={error} onPatch={onPatch} />
          </Tabs.Panel>
          <Tabs.Panel value="order" style={PANEL_STYLE}>
            <OrderFieldGrid line={line} showErrors={showErrors} onPatch={onPatch} />
          </Tabs.Panel>
        </Tabs>
      </Stack>
    </Box>
  );
}
