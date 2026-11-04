import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack } from '@iplik/ui';
import { useState } from 'react';
import { useSaveTareStandard } from '../hooks/useTareStandards';
import type { TareStandardDraft } from '../tareStandardDraft';
import { toRequest, validateTareStandard } from '../tareStandardDraft';
import { TareStandardFields } from './TareStandardFields';

const EMPTY: TareStandardDraft = {
  siteId: '',
  packagingType: 'BAG',
  standardTareKg: '',
  maxDeviationPct: '',
};

/** Lokasyon ve ambalaj için standart dara ile izin verilen sapmayı kaydeder; varsa günceller. */
export function TareStandardForm() {
  const { t } = useTranslation();
  const save = useSaveTareStandard();
  const [draft, setDraft] = useState<TareStandardDraft>(EMPTY);
  const [isChecked, setChecked] = useState(false);
  const [isSaved, setSaved] = useState(false);
  const errors = isChecked ? validateTareStandard(draft) : {};

  function change(patch: Partial<TareStandardDraft>) {
    setDraft((current) => ({ ...current, ...patch }));
    setSaved(false);
  }

  function submit() {
    setChecked(true);
    if (Object.keys(validateTareStandard(draft)).length > 0) return;
    save.mutate(toRequest(draft), {
      onSuccess: () => {
        setSaved(true);
      },
    });
  }

  return (
    <form
      noValidate
      onSubmit={(event) => {
        event.preventDefault();
        submit();
      }}
    >
      <Stack>
        <TareStandardFields draft={draft} errors={errors} onChange={change} />
        <Button type="submit" loading={save.isPending} w="fit-content">
          {t('masterData.tareStandard.save')}
        </Button>
        {save.isError && (
          <Alert color="red" role="alert">
            {t('masterData.tareStandard.failed')}
          </Alert>
        )}
        {isSaved && <Alert color="green">{t('masterData.tareStandard.saved')}</Alert>}
      </Stack>
    </form>
  );
}
