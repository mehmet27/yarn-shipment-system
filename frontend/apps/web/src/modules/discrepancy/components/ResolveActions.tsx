import { useTranslation } from '@iplik/i18n';
import { Button } from '@iplik/ui';
import type { ResolveChoice } from './resolveChoices';
import { JUSTIFIED_INVOICED } from './resolveChoices';
import { ResolveConfirm } from './ResolveConfirm';

export type ResolveActionsProps = {
  choice: ResolveChoice | undefined;
  /** "Haklı, faturalandı" onayında söylenen fatura numarası. */
  invoiceNo: string;
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
        text={
          choice === JUSTIFIED_INVOICED
            ? t('discrepancy.resolve.confirmTextInvoice', { no: props.invoiceNo.trim() })
            : t('discrepancy.resolve.confirmText', {
                resolution: t(`discrepancy.resolution.${choice}`),
              })
        }
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
