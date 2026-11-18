import { useTranslation } from '@iplik/i18n';
import { Button, Group, Text } from '@iplik/ui';

export type BrandingActionsProps = {
  isDirty: boolean;
  isSaving: boolean;
  onReset: () => void;
  onRevert: () => void;
  onSubmit: () => void;
};

/** Formun altı: kaydedilmemiş değişiklik notu, geri alma, varsayılana dönme ve kaydetme. */
export function BrandingActions(props: BrandingActionsProps) {
  const { t } = useTranslation();
  return (
    <Group justify="space-between" gap="sm">
      <Text size="sm" c="dimmed" role="status">
        {props.isDirty ? t('branding.unsaved') : ''}
      </Text>
      <Group gap="xs">
        {props.isDirty && (
          <Button variant="subtle" onClick={props.onRevert}>
            {t('branding.revert')}
          </Button>
        )}
        <Button variant="default" onClick={props.onReset}>
          {t('branding.reset')}
        </Button>
        <Button loading={props.isSaving} onClick={props.onSubmit}>
          {t('branding.submit')}
        </Button>
      </Group>
    </Group>
  );
}
