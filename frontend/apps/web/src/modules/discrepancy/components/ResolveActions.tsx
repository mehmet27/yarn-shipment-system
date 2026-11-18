import type { Resolution } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button } from '@iplik/ui';
import { ResolveConfirm } from './ResolveConfirm';

export type ResolveActionsProps = {
  resolution: Resolution | undefined;
  isConfirming: boolean;
  canSubmit: boolean;
  isPending: boolean;
  onAsk: () => void;
  onConfirm: () => void;
  onCancel: () => void;
};

/** "Karara bağla" önce satır içi onay sorar; karar geri alınamaz. */
export function ResolveActions(props: ResolveActionsProps) {
  const { t } = useTranslation();
  if (props.isConfirming && props.resolution !== undefined) {
    return (
      <ResolveConfirm
        resolutionLabel={t(`discrepancy.resolution.${props.resolution}`)}
        loading={props.isPending}
        onConfirm={props.onConfirm}
        onCancel={props.onCancel}
      />
    );
  }
  return (
    <Button disabled={!props.canSubmit} onClick={props.onAsk}>
      {t('discrepancy.resolve.submit')}
    </Button>
  );
}
