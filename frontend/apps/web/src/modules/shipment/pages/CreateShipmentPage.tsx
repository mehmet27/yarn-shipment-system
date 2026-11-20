import { useTranslation } from '@iplik/i18n';
import { PageHero, Stack } from '@iplik/ui';
import { BackLink } from '@/modules/shared';
import { useNavigate } from 'react-router';
import { CreateShipmentForm } from '../create/CreateShipmentForm';

/** Yeni sevkiyat (taslak) oluşturma; kayıttan sonra sevkiyat dosyasına gidilir. */
export function CreateShipmentPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  return (
    <Stack>
      <BackLink to="/panel/sevkiyat">{t('create.back')}</BackLink>
      <PageHero title={t('create.title')} subtitle={t('create.hint')} />
      <CreateShipmentForm
        onCreated={(shipmentId) => {
          void navigate(`/panel/sevkiyat/${shipmentId}`);
        }}
      />
    </Stack>
  );
}
