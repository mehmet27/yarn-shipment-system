import { useTranslation } from '@iplik/i18n';
import { PageHero, Stack, Tabs } from '@iplik/ui';
import { useSearchParams } from 'react-router';
import { InvoicedTab } from '../components/InvoicedTab';
import { ToInvoiceTab } from '../components/ToInvoiceTab';

const TABS = ['toInvoice', 'invoiced'] as const;
type InvoicingTab = (typeof TABS)[number];
/** Adres çubuğundaki sekme adı: `?sekme=faturalananlar`; yoksa Faturalanacaklar açılır. */
const TAB_PARAM = 'sekme';
const INVOICED_PARAM = 'faturalananlar';

/** Faturalama: Faturalanacaklar (bekleyen dosyalar, fatura girişi) ve Faturalananlar (girilmiş faturalar). */
export function InvoicingPage() {
  const { t } = useTranslation();
  const [params, setParams] = useSearchParams();
  const tab: InvoicingTab = params.get(TAB_PARAM) === INVOICED_PARAM ? 'invoiced' : 'toInvoice';
  return (
    <Stack>
      <PageHero
        title={t('settlement.invoicing.title')}
        subtitle={t('settlement.invoicing.subtitle')}
      />
      <Tabs
        value={tab}
        onChange={(next) => {
          setParams(next === 'invoiced' ? { [TAB_PARAM]: INVOICED_PARAM } : {}, { replace: true });
        }}
      >
        <Tabs.List>
          {TABS.map((item) => (
            <Tabs.Tab key={item} value={item}>
              {t(`settlement.invoicing.tabs.${item}`)}
            </Tabs.Tab>
          ))}
        </Tabs.List>
        <Tabs.Panel value="toInvoice" pt="md">
          {tab === 'toInvoice' && <ToInvoiceTab />}
        </Tabs.Panel>
        <Tabs.Panel value="invoiced" pt="md">
          {tab === 'invoiced' && <InvoicedTab />}
        </Tabs.Panel>
      </Tabs>
    </Stack>
  );
}
