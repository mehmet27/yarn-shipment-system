import { useTranslation } from '@iplik/i18n';
import { Alert } from '@iplik/ui';

/** Sevkiyat günü gelmeden çıkış fotoğrafı eklenemez; nedenini söyler (tarih sayfanın üstünde yazılıdır). */
export function NotDueAlert() {
  const { t } = useTranslation();
  return <Alert color="yellow">{t('evidence.notDue')}</Alert>;
}
