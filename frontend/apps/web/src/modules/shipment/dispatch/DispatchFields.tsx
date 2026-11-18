import { useTranslation } from '@iplik/i18n';
import { SimpleGrid, Stack, Text, TextInput } from '@iplik/ui';
import { formatKg } from '@iplik/units';
import { TicketCapture } from '@/modules/evidence';
import type { DispatchDraft, DispatchWeighing } from './dispatchDraft';

const FIELDS = [
  // Metin "önce boş araç (dara), sonra dolu araç (brüt)" der; alanlar da bu sırayla gelir.
  { name: 'tareKg', inputMode: 'decimal' },
  { name: 'grossKg', inputMode: 'decimal' },
  { name: 'bagCount', inputMode: 'numeric' },
  { name: 'shortfallReason', inputMode: 'text' },
] as const satisfies readonly { name: Exclude<keyof DispatchDraft, 'ticket'>; inputMode: string }[];

export type DispatchFieldsProps = {
  draft: DispatchDraft;
  onChange: (draft: DispatchDraft) => void;
  /** Geçerli tartı; yoksa net ve fiş alanı gösterilmez. */
  weighing: DispatchWeighing | undefined;
  /** Eksik kilo nedeni boş bırakıldı (istemci ya da sunucu uyarısı). */
  isReasonMissing: boolean;
};

/** Tartı alanları, hesaplanan net ve kantar fişi fotoğrafı. */
export function DispatchFields({
  draft,
  onChange,
  weighing,
  isReasonMissing,
}: DispatchFieldsProps) {
  const { t } = useTranslation();
  return (
    <Stack>
      <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
        {FIELDS.map(({ name, inputMode }) => (
          <TextInput
            key={name}
            inputMode={inputMode}
            {...(name === 'shortfallReason'
              ? { maxLength: 500, style: { gridColumn: '1 / -1' } }
              : {})}
            {...(name === 'grossKg' || name === 'tareKg' ? { rightSection: 'kg' } : {})}
            label={t(`dispatch.${name}`)}
            {...(name === 'shortfallReason' && isReasonMissing
              ? { error: t('dispatch.shortfallReasonRequired') }
              : {})}
            value={draft[name]}
            onChange={(event) => {
              onChange({ ...draft, [name]: event.currentTarget.value });
            }}
          />
        ))}
      </SimpleGrid>
      {weighing !== undefined && (
        <>
          <Text fw={600} fz="xl">
            {t('dispatch.net', { net: formatKg(weighing.netKg) })}
          </Text>
          <TicketCapture
            file={draft.ticket}
            onFileChange={(ticket) => {
              onChange({ ...draft, ticket });
            }}
          />
        </>
      )}
    </Stack>
  );
}
