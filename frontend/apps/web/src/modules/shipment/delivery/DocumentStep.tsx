import { useTranslation } from '@iplik/i18n';
import { SignedDocumentUpload } from '@/modules/evidence';
import { Stack, Text, TextInput } from '@iplik/ui';
import type { DocumentDraft } from './deliveryDraft';
import type { StepProps } from './types';

const FIELDS = [
  { name: 'shipmentNo', type: 'text', inputMode: 'text' },
  { name: 'date', type: 'date', inputMode: 'none' },
  { name: 'netKg', type: 'text', inputMode: 'decimal' },
  { name: 'bagCount', type: 'text', inputMode: 'numeric' },
] as const satisfies readonly {
  name: Exclude<keyof DocumentDraft, 'signedDocumentEvidenceId'>;
  type: string;
  inputMode: string;
}[];

/** İrsaliyede yazanlar (isteğe bağlı); imzalı irsaliye fotoğrafı yüklenince kanıt kimliği tutanağa bağlanır. */
export function DocumentStep({ shipmentId, draft, onChange }: StepProps) {
  const { t } = useTranslation();
  return (
    <Stack>
      <Text size="sm" c="dimmed">
        {t('delivery.document.hint')}
      </Text>
      {FIELDS.map(({ name, type, inputMode }) => (
        <TextInput
          key={name}
          size="lg"
          type={type}
          inputMode={inputMode}
          label={t(`delivery.document.${name}`)}
          value={draft.document[name]}
          onChange={(event) => {
            onChange({ document: { ...draft.document, [name]: event.currentTarget.value } });
          }}
        />
      ))}
      <SignedDocumentUpload
        shipmentId={shipmentId}
        onUploaded={(evidenceId) => {
          onChange({ document: { ...draft.document, signedDocumentEvidenceId: evidenceId } });
        }}
      />
    </Stack>
  );
}
