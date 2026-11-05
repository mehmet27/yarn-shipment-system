import { toProblem } from '@iplik/api-client';
import type { BrandingView } from '@iplik/api-client/branding';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, ColorInput, Group, Paper, SimpleGrid, Stack, TextInput } from '@iplik/ui';
import { DEFAULT_DRAFT, MAX_NAME_LENGTH } from '../brandingDraft';
import { useBrandingDraft } from '../useBrandingDraft';
import { BrandingPreview } from './BrandingPreview';
import { LogoField } from './LogoField';

export type BrandingFormProps = { branding: BrandingView };

function failureKey(error: unknown): string {
  return toProblem(error).code === 'VALIDATION_FAILED' ||
    toProblem(error).code === 'INVALID_MASTER_DATA'
    ? 'branding.errors.invalid'
    : 'branding.failed';
}

/** Marka ayarı: renk, firma adı ve logo; sağdaki önizleme kayıttan önce sonucu gösterir. */
export function BrandingForm({ branding }: BrandingFormProps) {
  const { t } = useTranslation();
  const { draft, error, isSaved, mutation, change, submit } = useBrandingDraft(branding);
  return (
    <SimpleGrid cols={{ base: 1, md: 2 }}>
      <Paper withBorder p="md">
        <Stack>
          <TextInput
            label={t('branding.companyName')}
            description={t('branding.companyNameHint')}
            maxLength={MAX_NAME_LENGTH}
            value={draft.companyName}
            error={error === 'name' ? t('branding.errors.name') : undefined}
            onChange={(event) => {
              change({ companyName: event.currentTarget.value });
            }}
          />
          <ColorInput
            label={t('branding.primaryColor')}
            description={t('branding.primaryColorHint')}
            format="hex"
            value={draft.primaryColor}
            error={error === 'color' ? t('branding.errors.color') : undefined}
            onChange={(primaryColor) => {
              change({ primaryColor });
            }}
          />
          <LogoField
            value={draft.logoDataUrl}
            onChange={(logoDataUrl) => {
              change({ logoDataUrl });
            }}
          />
          {mutation.isError && <Alert color="red">{t(failureKey(mutation.error))}</Alert>}
          {isSaved && <Alert color="green">{t('branding.saved')}</Alert>}
          <Group>
            <Button loading={mutation.isPending} onClick={submit}>
              {t('branding.submit')}
            </Button>
            <Button
              variant="default"
              onClick={() => {
                change(DEFAULT_DRAFT);
              }}
            >
              {t('branding.reset')}
            </Button>
          </Group>
        </Stack>
      </Paper>
      <BrandingPreview draft={draft} />
    </SimpleGrid>
  );
}
