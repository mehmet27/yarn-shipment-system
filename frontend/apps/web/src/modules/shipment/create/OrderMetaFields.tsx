import { useTranslation } from '@iplik/i18n';
import { SimpleGrid, Text, TextInput } from '@iplik/ui';
import type { CreateDraft, CreateFieldErrors } from './createDraft';
import { SHIPMENT_DATE_HINT_ID, ShipmentDateField } from './ShipmentDateField';

type OrderMetaFieldsProps = {
  draft: CreateDraft;
  onChange: (draft: CreateDraft) => void;
  errors: CreateFieldErrors;
};

/** Sevkiyat tarihi, ilgili kişi ve sipariş no aynı satırda; tarih açıklaması satırın altında tek çizgide durur. */
export function OrderMetaFields({ draft, onChange, errors }: OrderMetaFieldsProps) {
  const { t } = useTranslation();
  return (
    <>
      <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="sm" mt="sm">
        <ShipmentDateField draft={draft} onChange={onChange} errors={errors} />
        <TextInput
          label={t('create.contactPerson')}
          value={draft.contactPerson}
          maxLength={200}
          onChange={(event) => {
            onChange({ ...draft, contactPerson: event.currentTarget.value });
          }}
        />
        <TextInput
          label={t('create.orderNo')}
          value={draft.orderNo}
          maxLength={100}
          onChange={(event) => {
            onChange({ ...draft, orderNo: event.currentTarget.value });
          }}
        />
      </SimpleGrid>
      <Text id={SHIPMENT_DATE_HINT_ID} c="dimmed" size="sm" mt="xs">
        {t('create.shipmentDateHint')}
      </Text>
    </>
  );
}
