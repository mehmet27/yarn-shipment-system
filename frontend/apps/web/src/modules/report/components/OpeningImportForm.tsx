import { todayIsoDate, useTranslation } from '@iplik/i18n';
import { Button, SectionCard, SimpleGrid, Stack, Text, Textarea, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { useCurrentUser } from '@/modules/auth';
import { useOpeningImport } from '../hooks/useFaconReports';
import { usePartyNames } from '../hooks/usePartyNames';
import { OpeningMessages } from './OpeningMessages';
import { OpeningPreview } from './OpeningPreview';
import { parseOpeningLines } from '../openingImport';

const today = () => todayIsoDate();

/** Admin/Müdür: 'fasoncu;iplik;kg' satırlarını yapıştırıp önizleyerek açılış bakiyesi yükler. */
export function OpeningImportForm() {
  const { t } = useTranslation();
  const { data: me } = useCurrentUser();
  const { idOf } = usePartyNames();
  const mutation = useOpeningImport();
  const [input, setInput] = useState('');
  const [asOf, setAsOf] = useState(today);
  // Açılış yükleme izni backend'de Müdür rolüne bağlıdır; bunun için ayrı bir izin anahtarı yok.
  const isAllowed = me?.roles.includes('MANAGER') ?? false;
  if (!isAllowed) return null;
  const lines = parseOpeningLines(input, idOf);
  const valid = lines.flatMap((line) => (line.row === undefined ? [] : [line.row]));
  const errors = lines.filter((line) => line.error !== undefined);
  return (
    <SectionCard title={t('report.opening.title')} icon="upload">
      <Stack>
        <Text size="sm" c="dimmed">
          {t('report.opening.hint')}
        </Text>
        <SimpleGrid cols={{ base: 1, sm: 3 }}>
          <TextInput
            type="date"
            label={t('report.opening.asOf')}
            value={asOf}
            onChange={(event) => {
              setAsOf(event.currentTarget.value);
            }}
          />
        </SimpleGrid>
        <Textarea
          label={t('report.opening.data')}
          minRows={5}
          value={input}
          onChange={(event) => {
            setInput(event.currentTarget.value);
          }}
        />
        {valid.length > 0 && <OpeningPreview rows={valid} />}
        <OpeningMessages errors={errors} mutation={mutation} />
        <Button
          w="fit-content"
          disabled={valid.length === 0 || errors.length > 0 || asOf === ''}
          loading={mutation.isPending}
          onClick={() => {
            mutation.mutate({ asOf, rows: valid });
          }}
        >
          {t('report.opening.submit', { count: valid.length })}
        </Button>
      </Stack>
    </SectionCard>
  );
}
