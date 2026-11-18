import { toProblem } from '@iplik/api-client';
import type { BrandingView } from '@iplik/api-client/branding';
import { useTranslation } from '@iplik/i18n';
import { Alert, SectionCard, SimpleGrid, Stack, TextInput } from '@iplik/ui';
import { DEFAULT_DRAFT, MAX_NAME_LENGTH } from '../brandingDraft';
import { useBrandingDraft } from '../useBrandingDraft';
import { BrandingActions } from './BrandingActions';
import { BrandingPreview } from './BrandingPreview';
import { ColorSection } from './ColorSection';
import { LogoField } from './LogoField';

export type BrandingFormProps = { branding: BrandingView };

function failureKey(error: unknown): string {
  return toProblem(error).code === 'VALIDATION_FAILED' ||
    toProblem(error).code === 'INVALID_MASTER_DATA'
    ? 'branding.errors.invalid'
    : 'branding.failed';
}

/** Marka ayarı: solda firma adı, logo ve renk; sağda kayıttan önce sonucu gösteren önizleme. */
export function BrandingForm({ branding }: BrandingFormProps) {
  const { t } = useTranslation();
  const form = useBrandingDraft(branding);
  const { draft, error, mutation, change } = form;
  return (
    <SimpleGrid cols={{ base: 1, md: 2 }} style={{ alignItems: 'start' }}>
      <Stack>
        <SectionCard title={t('branding.sections.identity')}>
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
            <LogoField
              value={draft.logoDataUrl}
              onChange={(logoDataUrl) => {
                change({ logoDataUrl });
              }}
            />
          </Stack>
        </SectionCard>
        <ColorSection
          value={draft.primaryColor}
          hasError={error === 'color'}
          onChange={(primaryColor) => {
            change({ primaryColor });
          }}
        />
        {mutation.isError && <Alert color="red">{t(failureKey(mutation.error))}</Alert>}
        {form.isSaved && <Alert color="green">{t('branding.saved')}</Alert>}
        <BrandingActions
          isDirty={form.isDirty}
          isSaving={mutation.isPending}
          onReset={() => {
            change(DEFAULT_DRAFT);
          }}
          onRevert={form.revert}
          onSubmit={form.submit}
        />
      </Stack>
      <BrandingPreview draft={draft} />
    </SimpleGrid>
  );
}
