import { useTranslation } from '@iplik/i18n';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { usePermission } from '@/modules/auth';
import { useQuickSearchTarget } from '../hooks/useQuickSearchTarget';
import { ShipmentSearchBox } from './ShipmentSearchBox';

export type QuickSearchProps = {
  /** Telefonda yan menü içindeyken aramadan sonra menüyü kapatır. */
  onNavigate?: () => void;
};

/**
 * Her ekrandan hızlı arama: sevkiyat numarası, fasoncu adı, şoför ya da plaka yazılır. Tek sevkiyat bulunursa doğrudan
 * dosyası açılır; birden çok ya da hiç bulunmazsa sevkiyat listesi o aramayla açılır. Sevkiyat göremeyen role görünmez.
 */
export function QuickSearch({ onNavigate }: QuickSearchProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const canView = usePermission('SHIPMENT_VIEW');
  const findTarget = useQuickSearchTarget();
  // Her aramadan sonra kutu boşalsın diye sayaç anahtar olarak verilir.
  const [searchCount, setSearchCount] = useState(0);
  if (!canView) return null;
  return (
    <ShipmentSearchBox
      key={searchCount}
      label={t('nav.quickSearch')}
      isLabelHidden
      placeholder={t('nav.quickSearchPlaceholder')}
      onSearch={(text) => {
        if (text === '') return;
        void findTarget(text).then((target) => {
          setSearchCount((count) => count + 1);
          onNavigate?.();
          return navigate(target);
        });
      }}
    />
  );
}
