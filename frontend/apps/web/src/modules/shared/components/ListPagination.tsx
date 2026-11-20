import { useTranslation } from '@iplik/i18n';
import { Pagination } from '@iplik/ui';

export type ListPaginationProps = {
  total: number;
  value: number;
  onChange: (page: number) => void;
};

const CONTROL_LABEL = {
  first: 'common.pagination.first',
  previous: 'common.pagination.previous',
  next: 'common.pagination.next',
  last: 'common.pagination.last',
} as const;

/** Listelerin ortak sayfalaması; önceki/sonraki okları ekran okuyucuya adıyla okunur (H-27). */
export function ListPagination({ total, value, onChange }: ListPaginationProps) {
  const { t } = useTranslation();
  return (
    <Pagination
      total={total}
      value={value}
      onChange={onChange}
      getControlProps={(control) => ({ 'aria-label': t(CONTROL_LABEL[control]) })}
    />
  );
}
