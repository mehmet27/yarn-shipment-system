import { useTranslation } from '@iplik/i18n';
import { Select, Stack, Text, TextInput, Title } from '@iplik/ui';
import { ListPagination, QueryState } from '@/modules/shipment';
import { AUDIT_PAGE_SIZE, ENTITY_TYPES, useAuditFilter } from '../hooks/useAuditFilter';
import { useAuditRecords } from '../hooks/useAuditLog';
import { AuditTable } from './AuditTable';
import { ChainCheckStatus } from './ChainCheckStatus';

/** Denetim kaydı (salt okuma): kim, ne zaman, hangi kayıt üzerinde ne yaptı. */
export function AuditLogPage() {
  const { t } = useTranslation();
  const filter = useAuditFilter();
  const query = useAuditRecords({
    page: filter.page - 1,
    size: AUDIT_PAGE_SIZE,
    ...(filter.entityType === undefined ? {} : { entityType: filter.entityType }),
    ...(filter.action === '' ? {} : { action: filter.action }),
  });
  return (
    <Stack>
      <Title order={1} size="h2">
        {t('audit.title')}
      </Title>
      <ChainCheckStatus />
      <Select
        label={t('audit.entityFilter')}
        placeholder={t('audit.all')}
        data={ENTITY_TYPES.map((value) => ({ value, label: t(`audit.entity.${value}`) }))}
        value={filter.entityType ?? null}
        onChange={(value) => {
          filter.setEntityType(ENTITY_TYPES.find((item) => item === value));
        }}
        clearable
      />
      <TextInput
        label={t('audit.actionFilter')}
        value={filter.action}
        onChange={(event) => {
          filter.setAction(event.currentTarget.value);
        }}
      />
      <QueryState query={query}>
        {(page) =>
          page.items.length === 0 ? (
            <Text c="dimmed">{t('audit.empty')}</Text>
          ) : (
            <Stack>
              <AuditTable records={page.items} />
              <ListPagination
                total={page.page.totalPages}
                value={filter.page}
                onChange={filter.setPage}
              />
            </Stack>
          )
        }
      </QueryState>
    </Stack>
  );
}
