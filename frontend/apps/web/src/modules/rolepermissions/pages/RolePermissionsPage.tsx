import { useTranslation } from '@iplik/i18n';
import { Alert, Center, Loader, PageHero, Stack, Tabs } from '@iplik/ui';
import { RolePermissionEditor } from '../components/RolePermissionEditor';
import { useRolePermissions } from '../hooks/useRolePermissions';

/** Yönetim > Yetkiler: rollerin ekran ve işlem izinleri (yalnızca PERMISSION_MANAGE). Yönetici rolü sabittir. */
export function RolePermissionsPage() {
  const { t } = useTranslation();
  const query = useRolePermissions();
  const view = query.data;
  return (
    <Stack>
      <PageHero title={t('permissions.title')} subtitle={t('permissions.intro')} />
      {view !== undefined && (
        <Tabs
          defaultValue={view.roles.find((row) => row.editable)?.role ?? null}
          keepMounted={false}
          variant="pills"
        >
          <Tabs.List
            aria-label={t('permissions.roleTabs')}
            style={{ flexWrap: 'nowrap', overflowX: 'auto' }}
          >
            {view.roles.map((row) => (
              <Tabs.Tab
                key={row.role}
                value={row.role}
                style={{ whiteSpace: 'nowrap', flexShrink: 0 }}
              >
                {t(`users.role.${row.role}`)}
              </Tabs.Tab>
            ))}
          </Tabs.List>
          {view.roles.map((row) => (
            <Tabs.Panel key={row.role} value={row.role} pt="md">
              <RolePermissionEditor
                key={row.role}
                row={row}
                lockedPermissions={view.lockedPermissions}
              />
            </Tabs.Panel>
          ))}
        </Tabs>
      )}
      {view === undefined && query.isError && (
        <Alert color="red">{t('permissions.loadFailed')}</Alert>
      )}
      {view === undefined && !query.isError && (
        <Center py="xl">
          <Loader />
        </Center>
      )}
    </Stack>
  );
}
