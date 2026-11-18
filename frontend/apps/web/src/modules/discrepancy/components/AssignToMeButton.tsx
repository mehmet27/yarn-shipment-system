import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack, Text } from '@iplik/ui';
import { useCurrentUser } from '@/modules/auth';
import { useAssignDiscrepancy } from '../hooks/useDecideDiscrepancy';

/** Kullanıcı listesi ucu olmadığından atama "Bana ata" ile kendi kimliğine yapılır. */
export function AssignToMeButton({
  discrepancyId,
  assigneeId,
}: {
  discrepancyId: string;
  /** Uyuşmazlığın şu anki sorumlusu; kullanıcının kendisiyse düğme yerine kısa metin görünür. */
  assigneeId: string | undefined;
}) {
  const { t } = useTranslation();
  const user = useCurrentUser();
  const mutation = useAssignDiscrepancy(discrepancyId);
  const userId = user.data?.userId;
  if (userId !== undefined && assigneeId === userId) {
    return <Text size="sm">{t('discrepancy.assign.mine')}</Text>;
  }
  return (
    <Stack>
      {mutation.isError && (
        <Alert color="red">
          {t(
            toProblem(mutation.error).code === 'DISCREPANCY_NOT_OPEN'
              ? 'discrepancy.assign.notOpen'
              : 'discrepancy.assign.failed',
          )}
        </Alert>
      )}
      <Button
        variant="default"
        disabled={userId === undefined}
        loading={mutation.isPending}
        onClick={() => {
          if (userId !== undefined) mutation.mutate(userId);
        }}
      >
        {t('discrepancy.assign.toMe')}
      </Button>
    </Stack>
  );
}
