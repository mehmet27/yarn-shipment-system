import { useTranslation } from '@iplik/i18n';
import { Button } from '@iplik/ui';
import { useState } from 'react';
import type { ManagedKind, RecordRow } from '../kinds';
import { LoginAccountDialog } from './LoginAccountDialog';

/** Yalnızca etkin şoför ve fasoncu kartında görünür; diğer türlerde giriş hesabı yoktur. */
export function LoginAccountButton({ kind, record }: { kind: ManagedKind; record: RecordRow }) {
  const { t } = useTranslation();
  const [isOpen, setOpen] = useState(false);
  if ((kind !== 'driver' && kind !== 'subcontractor') || !record.isActive) return null;
  return (
    <>
      <Button
        variant="default"
        onClick={() => {
          setOpen(true);
        }}
      >
        {t('masterData.loginAccount.open')}
      </Button>
      {isOpen && (
        <LoginAccountDialog
          kind={kind}
          recordId={record.id}
          onClose={() => {
            setOpen(false);
          }}
        />
      )}
    </>
  );
}
