import type { Resolution } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button } from '@iplik/ui';
import { ResolveConfirm } from './ResolveConfirm';

export type ResolveActionsProps = {
  choice: Resolution | undefined;
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
  const { choice } = props;
  if (props.isConfirming && choice !== undefined) {
    return (
      <ResolveConfirm
        text={t('discrepancy.resolve.confirmText', {
          resolution: t(`discrepancy.resolution.${choice}`),
        })}
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
