import { useTranslation } from '@iplik/i18n';
import { Text } from '@iplik/ui';

const SIGNATURES = ['prepared', 'checked', 'approved'] as const;

/** Hazırlayan, kontrol eden ve onaylayan imza alanları. */
export function PrintSignatures() {
  const { t } = useTranslation();
  return (
    <div className="print-signatures">
      {SIGNATURES.map((key) => (
        <div key={key} className="print-signature">
          <Text size="sm" fw={600}>
            {t(`report.export.signature.${key}`)}
          </Text>
          <div className="print-signature-line" />
          <Text size="xs">{t('report.export.signature.dateLine')}</Text>
        </div>
      ))}
    </div>
  );
}
