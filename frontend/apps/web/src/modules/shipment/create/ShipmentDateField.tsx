import { todayIsoDate, useTranslation } from '@iplik/i18n';
import { TextInput } from '@iplik/ui';
import type { CreateDraft, CreateFieldErrors } from './createDraft';

type ShipmentDateFieldProps = {
  draft: CreateDraft;
  onChange: (draft: CreateDraft) => void;
  errors: CreateFieldErrors;
};

/** Sevkiyat günü: müdür girer, depo bu günden önce çuval atayamaz ve çıkış tartısı giremez. */
export function ShipmentDateField({ draft, onChange, errors }: ShipmentDateFieldProps) {
  const { t } = useTranslation();
  return (
    <TextInput
      type="date"
      label={t('create.shipmentDate')}
      description={t('create.shipmentDateHint')}
      value={draft.shipmentDate}
      min={todayIsoDate()}
      required
      {...(errors.shipmentDate === undefined
        ? {}
        : { error: t(`create.errors.${errors.shipmentDate}`) })}
      onChange={(event) => {
        onChange({ ...draft, shipmentDate: event.currentTarget.value });
      }}
    />
  );
}
