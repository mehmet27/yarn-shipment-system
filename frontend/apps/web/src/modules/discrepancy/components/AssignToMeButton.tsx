import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack } from '@iplik/ui';
import { useCurrentUser } from '@/modules/auth';
import { useAssignDiscrepancy } from '../hooks/useDecideDiscrepancy';

/** Kullanıcı listesi ucu olmadığından atama "Bana ata" ile kendi kimliğine yapılır. */
export function AssignToMeButton({ discrepancyId }: { discrepancyId: string }) {
  const { t } = useTranslation();
  const user = useCurrentUser();
  const mutation = useAssignDiscrepancy(discrepancyId);
  const userId = user.data?.userId;
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
