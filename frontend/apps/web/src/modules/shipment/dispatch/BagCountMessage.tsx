import { useTranslation } from '@iplik/i18n';
import { Alert, Icon, Text } from '@iplik/ui';

export type BagCountMessageProps = { enteredCount: number; bagCount: number };

/** Tartıya yazılan çuval sayısı eklenen çuvalları tutmuyor. */
export function BagCountMessage({ enteredCount, bagCount }: BagCountMessageProps) {
  const { t } = useTranslation();
  return (
    <>
      <Text>{t('dispatch.bagMismatch.countBody', { enteredCount, bagCount })}</Text>
      <Alert color="red" icon={<Icon name="discrepancies" />}>
        {t('dispatch.bagMismatch.countBlocked')}
      </Alert>
    </>
  );
}
