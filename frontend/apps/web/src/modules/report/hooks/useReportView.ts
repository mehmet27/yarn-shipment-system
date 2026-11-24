import { useSearchParams } from 'react-router';
import { useRememberedView } from '@/modules/shared';
import type { DateRange } from '../components/DateRangeInputs';
import { isValidRange } from '../components/DateRangeInputs';
import type { PeriodDays } from '../period';
import { DEFAULT_PERIOD, PERIOD_DAYS, toPeriodParams } from '../period';
import type { ReportKind } from './usePartyReport';
import { REPORT_KINDS } from './usePartyReport';

const DEFAULT_KIND: ReportKind = 'summary';
/** Özel tarih aralığı adreste durur (bağlantı paylaşılabilir) ama hatırlanmaz: haftalar sonra eski aralık yanıltır. */
const REMEMBERED_KEYS = ['sekme', 'gun', 'uyusmazlikli'] as const;

type Changes = Record<string, string | undefined>;

/**
 * Raporlar ekranının görünümü adreste tutulur (FE-API-08): sekme, hazır dönem, özel aralık ve "yalnızca uyuşmazlıklı"
 * anahtarı. Sayfa yenilenince ya da bağlantı paylaşılınca aynı rapor açılır; kişinin son seçimi bir sonraki girişte
 * hazır gelir. Hazır dönem ya da geçerli özel aralık; ikisi de doluyken aralık geçerlidir.
 */
export function useReportView() {
  const [searchParams, setSearchParams] = useSearchParams();
  useRememberedView('raporlar', REMEMBERED_KEYS);
  const kind = REPORT_KINDS.find((item) => item === searchParams.get('sekme')) ?? DEFAULT_KIND;
  const days = PERIOD_DAYS.find((item) => item === searchParams.get('gun')) ?? DEFAULT_PERIOD;
  const range: DateRange = {
    from: searchParams.get('bas') ?? '',
    to: searchParams.get('bit') ?? '',
  };
  const isRangeValid = isValidRange(range);

  const update = (changes: Changes) => {
    setSearchParams(
      (current) => {
        const next = new URLSearchParams(current);
        Object.entries(changes).forEach(([key, value]) => {
          if (value === undefined || value === '') next.delete(key);
          else next.set(key, value);
        });
        return next;
      },
      { replace: true },
    );
  };

  return {
    kind,
    setKind: (next: ReportKind) => {
      update({ sekme: next === DEFAULT_KIND ? undefined : next });
    },
    days,
    setDays: (next: PeriodDays) => {
      update({ gun: next === DEFAULT_PERIOD ? undefined : next });
    },
    range,
    setRange: (next: DateRange) => {
      update({ bas: next.from, bit: next.to });
    },
    hasRangeError: range.from !== '' && range.to !== '' && !isRangeValid,
    isOnlyDiscrepant: searchParams.get('uyusmazlikli') === '1',
    setIsOnlyDiscrepant: (next: boolean) => {
      update({ uyusmazlikli: next ? '1' : undefined });
    },
    params: isRangeValid ? { from: range.from, to: range.to } : toPeriodParams(days),
  };
}
