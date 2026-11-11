import type { WorkArea } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Select } from '@iplik/ui';

const WORK_AREAS: readonly WorkArea[] = ['CENTRAL', 'SUBCONTRACTOR'];

export type WorkAreaSelectProps = {
  value: WorkArea | undefined;
  onChange: (area: WorkArea | undefined) => void;
  error?: string | undefined;
};

/** Kalite kullanıcısının çalışma alanı: merkez fabrikanın kumaşı ya da fasoncuların kumaşı. */
export function WorkAreaSelect({ value, onChange, error }: WorkAreaSelectProps) {
  const { t } = useTranslation();
  return (
    <Select
      label={t('users.field.workArea')}
      description={t('users.workAreaHint')}
      data={WORK_AREAS.map((area) => ({ value: area, label: t(`users.workArea.${area}`) }))}
      value={value ?? null}
      onChange={(next) => {
        onChange(WORK_AREAS.find((area) => area === next));
      }}
      allowDeselect={false}
      withAsterisk
      error={error}
    />
  );
}
