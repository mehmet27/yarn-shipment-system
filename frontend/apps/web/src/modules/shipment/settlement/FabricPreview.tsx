import type { FabricPreview as Preview } from '@iplik/api-client/fabric';
import { useTranslation } from '@iplik/i18n';
import { Button, Group, Paper, Stack, Text } from '@iplik/ui';
import { FabricPreviewYarn } from './FabricPreviewYarn';
import { FabricShortages } from './FabricShortages';
import { formatKgText } from './format';

export type FabricPreviewProps = {
  preview: Preview;
  fabricType: string;
  netKg: string;
  isLoading: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

/**
 * Kaydetmeden önce: her iplikten kaç kilo hangi sevkiyattan düşecek. Kumaş girişi sonradan değiştirilemez; bu
 * yüzden satır içi onay istenir.
 */
export function FabricPreview(props: FabricPreviewProps) {
  const { t } = useTranslation();
  const { preview, fabricType, netKg, isLoading, onConfirm, onCancel } = props;
  return (
    <Paper withBorder p="sm" role="group" aria-label={t('settlement.fabric.confirmTitle')}>
      <Stack gap="sm">
        <Text fw={500}>{t('settlement.fabric.previewTitle')}</Text>
        {preview.yarns.map((yarn) => (
          <FabricPreviewYarn key={`${yarn.yarnType}|${yarn.lotId ?? ''}`} yarn={yarn} />
        ))}
        {preview.feasible ? (
          <>
            <Text size="sm">
              {t('settlement.fabric.confirmText', { kg: formatKgText(netKg), type: fabricType })}
            </Text>
            <Group grow>
              <Button loading={isLoading} onClick={onConfirm}>
                {t('settlement.fabric.confirmYes')}
              </Button>
              <Button variant="default" onClick={onCancel}>
                {t('settlement.fabric.confirmNo')}
              </Button>
            </Group>
          </>
        ) : (
          <FabricShortages preview={preview} onCancel={onCancel} />
        )}
      </Stack>
    </Paper>
  );
}
