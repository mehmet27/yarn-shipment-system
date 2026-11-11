import type { OpeningImportResponse } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert } from '@iplik/ui';
import type { UseMutationResult } from '@tanstack/react-query';
import type { ParsedLine } from '../openingImport';

type Props = {
  errors: ParsedLine[];
  mutation: Pick<UseMutationResult<OpeningImportResponse>, 'isError' | 'isSuccess' | 'data'>;
};

export function OpeningMessages({ errors, mutation }: Props) {
  const { t } = useTranslation();
  return (
    <>
      {errors.map((line) => (
        <Alert key={line.line} color="red" role="alert">
          {t('report.opening.lineError', {
            line: line.line,
            reason: t(`report.opening.error.${line.error ?? 'format'}`),
          })}
        </Alert>
      ))}
      {mutation.isError && (
        <Alert color="red" role="alert">
          {t('report.opening.failed')}
        </Alert>
      )}
      {mutation.isSuccess && (
        <Alert color="green" role="status">
          {t('report.opening.result', {
            imported: mutation.data?.imported ?? 0,
            skipped: mutation.data?.skipped ?? 0,
          })}
        </Alert>
      )}
    </>
  );
}
