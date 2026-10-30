import { useTranslation } from '@iplik/i18n';
import { Alert } from '@iplik/ui';
import { useCallback, useEffect, useState } from 'react';

/** Son başarılı kayıt; `at` her kayıtta değişir, böylece aynı mesaj yeniden gösterilir. */
export type Saved = { isNew: boolean; at: number };

const HIDE_AFTER_MS = 5000;

export function useSavedNotice() {
  const [saved, setSaved] = useState<Saved>();
  const show = useCallback((isNew: boolean) => {
    setSaved({ isNew, at: Date.now() });
  }, []);
  const onDismiss = useCallback(() => {
    setSaved(undefined);
  }, []);
  return { saved, show, onDismiss };
}

/** Kayıttan sonra pencere kapanınca listenin üstünde birkaç saniye görünen başarı bildirimi. */
export function SavedNotice({
  saved,
  onDismiss,
}: {
  saved: Saved | undefined;
  onDismiss: () => void;
}) {
  const { t } = useTranslation();
  useEffect(() => {
    if (saved === undefined) return undefined;
    const timer = setTimeout(onDismiss, HIDE_AFTER_MS);
    return () => {
      clearTimeout(timer);
    };
  }, [saved, onDismiss]);
  if (saved === undefined) return null;
  return (
    <Alert
      color="green"
      role="status"
      withCloseButton
      closeButtonLabel={t('common.close')}
      onClose={onDismiss}
    >
      {t(saved.isNew ? 'masterData.created' : 'masterData.updated')}
    </Alert>
  );
}
