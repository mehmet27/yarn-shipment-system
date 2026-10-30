import { useTranslation } from '@iplik/i18n';
import { Anchor, Stack, Text, Title } from '@iplik/ui';
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
      <Stack gap={2}>
        <Title order={1} size="h3">
          {t('create.title')}
        </Title>
        <Text c="dimmed">{t('create.hint')}</Text>
      </Stack>
      <CreateShipmentForm
        onCreated={(shipmentId) => {
          void navigate(`/panel/sevkiyat/${shipmentId}`);
        }}
      />
    </Stack>
  );
}
