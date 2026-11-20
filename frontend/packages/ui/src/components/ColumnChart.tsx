import { BarChart } from '@mantine/charts';
import { Box } from '@mantine/core';

export type ColumnChartSeries = {
  /** Satırdaki alan adı. */
  key: string;
  /** Açıklamada ve ipucunda görünen ad. */
  label: string;
  /** Mantine renk adı (`blue`, `orange`...). */
  color: string;
};

export type ColumnChartRow = Readonly<Record<string, string | number>>;

export type ColumnChartProps = {
  /** Ekran okuyucu için grafiğin adı. */
  label: string;
  rows: readonly ColumnChartRow[];
  /** Yatay eksendeki kategori alanı (ör. ay). */
  categoryKey: string;
  series: readonly ColumnChartSeries[];
  /** İpucundaki tam değer (ör. `34.032 kg`). */
  formatValue: (value: number) => string;
  /** Dikey eksendeki kısa değer (ör. `30.000`). */
  formatTick: (value: number) => string;
  testId?: string | undefined;
};

const HEIGHT = 260;
const MAX_BAR_WIDTH = 24;
const BAR_END_RADIUS = 4;
/** Veri ucu yuvarlak, taban düz. */
const BAR_RADIUS: [number, number, number, number] = [BAR_END_RADIUS, BAR_END_RADIUS, 0, 0];
const Y_AXIS_WIDTH = 56;

/**
 * Yan yana sütun grafiği (kategori × seri), tek dikey eksen. Renkler tema adlarıyla verilir, açık ve koyu
 * görünümde kendiliğinden uyar; seriler açıklamada adıyla da yazar. Rakamların tablosu çağıran tarafta gösterilir.
 */
export function ColumnChart(props: ColumnChartProps) {
  const { label, rows, categoryKey, series, formatValue, formatTick, testId } = props;
  return (
    <Box role="img" aria-label={label} data-testid={testId}>
      <BarChart
        h={HEIGHT}
        data={rows.map((row) => ({ ...row }))}
        dataKey={categoryKey}
        series={series.map((item) => ({
          name: item.key,
          label: item.label,
          color: `var(--mantine-color-${item.color}-filled)`,
        }))}
        valueFormatter={formatValue}
        withLegend={series.length > 1}
        legendProps={{ itemSorter: null }}
        tickLine="none"
        gridAxis="x"
        strokeDasharray="0"
        maxBarWidth={MAX_BAR_WIDTH}
        barProps={{ radius: BAR_RADIUS }}
        yAxisProps={{ width: Y_AXIS_WIDTH, tickFormatter: formatTick }}
      />
    </Box>
  );
}
