import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert } from '@iplik/ui';
import { errorKey } from '../errors';

/** Sunucunun hata koduna göre kullanıcıya ne yapacağını söyler. */
export function ResetError({ error }: { error: unknown }) {
  const { t } = useTranslation();
  return <Alert color="red">{t(errorKey(toProblem(error).code))}</Alert>;
}
