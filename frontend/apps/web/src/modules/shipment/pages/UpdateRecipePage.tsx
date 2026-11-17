import { useTranslation } from '@iplik/i18n';
import { Alert, Anchor, PageHero, Stack } from '@iplik/ui';
import { Link, useNavigate, useParams } from 'react-router';
import { QueryState } from '../components/QueryState';
import { useShipment } from '../hooks/useShipment';
import { RecipeEditor } from '../recipe/RecipeEditor';

/** "İplik güncelle" hedefi: kantar uyuşmazlığındaki sevkiyatın reçetesi düzeltilir, tartıyı tutunca sevkiyat yola çıkar. */
export function UpdateRecipePage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { shipmentId = '' } = useParams();
  const query = useShipment(shipmentId);
  const back = `/panel/sevkiyat/${shipmentId}`;
  return (
    <Stack>
      <Anchor component={Link} to={back}>
        {t('recipeEdit.back')}
      </Anchor>
      <QueryState query={query}>
        {(shipment) => (
          <Stack>
            <PageHero title={t('recipeEdit.title')} subtitle={shipment.shipmentNo} />
            {shipment.status === 'WEIGHT_MISMATCH' ? (
              <RecipeEditor
                shipment={shipment}
                onDone={() => {
                  void navigate(back);
                }}
              />
            ) : (
              <Alert color="yellow">{t('recipeEdit.notMismatch')}</Alert>
            )}
          </Stack>
        )}
      </QueryState>
    </Stack>
  );
}
