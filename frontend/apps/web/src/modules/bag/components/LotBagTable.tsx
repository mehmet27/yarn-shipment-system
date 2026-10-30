import type { LabeledBagView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Checkbox, Table, Text } from '@iplik/ui';

export type LotBagTableProps = {
  bags: readonly LabeledBagView[];
  selected: ReadonlySet<string>;
  onToggle: (uniqueKey: string) => void;
  onToggleAll: (uniqueKeys: string[]) => void;
};

/** Partinin çuvalları; yalnızca depodaki ve sevkiyattaki çuvalın etiketi seçilebilir. */
export function LotBagTable({ bags, selected, onToggle, onToggleAll }: LotBagTableProps) {
  const { t } = useTranslation();
  const printable = bags.filter(isPrintable).map((item) => item.bag.uniqueKey);
  const isAllSelected = printable.length > 0 && printable.every((key) => selected.has(key));
  if (bags.length === 0) return <Text c="dimmed">{t('bag.empty')}</Text>;
  return (
    <Table.ScrollContainer minWidth={320}>
      <Table striped>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>
              <Checkbox
                aria-label={t('bag.selectAll')}
                checked={isAllSelected}
                onChange={() => {
                  onToggleAll(isAllSelected ? [] : printable);
                }}
              />
            </Table.Th>
            <Table.Th>{t('bag.uniqueKey')}</Table.Th>
            <Table.Th>{t('bag.status')}</Table.Th>
            <Table.Th>{t('bag.prints')}</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {bags.map((item) => (
            <Table.Tr key={item.bag.id}>
              <Table.Td>
                <Checkbox
                  aria-label={t('bag.select', { key: item.bag.uniqueKey })}
                  checked={selected.has(item.bag.uniqueKey)}
                  disabled={!isPrintable(item)}
                  onChange={() => {
                    onToggle(item.bag.uniqueKey);
                  }}
                />
              </Table.Td>
              <Table.Td ff="monospace">{item.bag.uniqueKey}</Table.Td>
              <Table.Td>{t(`bag.statuses.${item.bag.status}`)}</Table.Td>
              <Table.Td>
                {item.printCount === 0
                  ? t('bag.notPrinted')
                  : t('bag.printedTimes', { count: item.printCount })}
              </Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}

export function isPrintable(item: LabeledBagView): boolean {
  return item.bag.status === 'CREATED' || item.bag.status === 'RESERVED';
}
