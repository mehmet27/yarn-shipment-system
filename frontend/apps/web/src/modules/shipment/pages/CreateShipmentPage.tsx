import { useTranslation } from '@iplik/i18n';
import { Anchor, PageHero, Paper, Stack } from '@iplik/ui';
import { Link, useNavigate } from 'react-router';
import { CreateShipmentForm } from '../create/CreateShipmentForm';

/** Yeni sevkiyat (taslak) oluşturma; kayıttan sonra sevkiyat dosyasına gidilir. */
export function CreateShipmentPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  return (
    <Stack>
      <Anchor component={Link} to="/panel/sevkiyat">
        {t('create.back')}
      </Anchor>
      <PageHero title={t('create.title')} subtitle={t('create.hint')} />
      <Paper withBorder p="lg" radius="md" maw={560}>
        <CreateShipmentForm
          onCreated={(shipmentId) => {
            void navigate(`/panel/sevkiyat/${shipmentId}`);
          }}
        />
      </Paper>
    </Stack>
  );
}
