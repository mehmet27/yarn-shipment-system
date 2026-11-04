import { useState } from 'react';
import type { DateRange } from '../components/DateRangeInputs';
import { isValidRange } from '../components/DateRangeInputs';
import type { PeriodDays } from '../period';
import { DEFAULT_PERIOD, toPeriodParams } from '../period';

const EMPTY_RANGE: DateRange = { from: '', to: '' };

/** Hazır dönem ya da geçerli özel aralık; ikisi de doluyken aralık, bitişten sonra başlangıçta hata verilir. */
export function useReportPeriod() {
  const [days, setDays] = useState<PeriodDays>(DEFAULT_PERIOD);
  const [range, setRange] = useState<DateRange>(EMPTY_RANGE);
  const isRangeValid = isValidRange(range);
  const hasRangeError = range.from !== '' && range.to !== '' && !isRangeValid;
  const params = isRangeValid ? { from: range.from, to: range.to } : toPeriodParams(days);
  return { days, setDays, range, setRange, hasRangeError, params };
}
