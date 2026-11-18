import { useTranslation } from '@iplik/i18n';
import { TextInput } from '@iplik/ui';

const FIELD_MAX_WIDTH_PX = 240;

/** Fire oranı yüzde olarak girilir (ör. 4 ya da 4,5). */
export function FireRateField(props: {
  value: string;
  errorCode: string | undefined;
  onChange: (value: string) => void;
}) {
  const { t } = useTranslation();
  return (
    <TextInput
      label={t('create.fireRate')}
      description={t('create.fireRateHint')}
      inputMode="decimal"
      withAsterisk
      maw={FIELD_MAX_WIDTH_PX}
      value={props.value}
      {...(props.errorCode === undefined ? {} : { error: t(`create.errors.${props.errorCode}`) })}
      onChange={(event) => {
        props.onChange(event.currentTarget.value);
      }}
    />
  );
}
