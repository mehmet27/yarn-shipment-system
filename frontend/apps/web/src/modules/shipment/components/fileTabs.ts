import { useState } from 'react';
import { useSearchParams } from 'react-router';

/** Sevkiyat dosyasının sekmeleri ve adresteki adları (`?sekme=cuvallar`); bağlantıyla doğrudan sekme açılabilir. */
export const FILE_TAB_SLUGS = {
  weights: 'kilolar',
  bags: 'cuvallar',
  history: 'gecmis',
  claims: 'itiraz',
  fabric: 'kumas',
  yarn: 'iplik-donusu',
  fire: 'fire',
  evidence: 'kanitlar',
} as const;

export type FileTab = keyof typeof FILE_TAB_SLUGS;

export const FILE_TAB_PARAM = 'sekme';
/** Sekmelerin sayfadaki kimliği; "sonraki işlem" düğmesi sekmeyi açıp buraya kaydırır. */
export const FILE_TABS_SECTION_ID = 'dosya-sekmeleri';
const DEFAULT_TAB: FileTab = 'weights';

const TABS = Object.keys(FILE_TAB_SLUGS).filter((key): key is FileTab => key in FILE_TAB_SLUGS);

export function isFileTab(value: string | null): value is FileTab {
  return TABS.some((tab) => tab === value);
}

/** Adresteki sekme adından sekmeyi bulur; ad yoksa ya da tanınmıyorsa ilk sekme (Kilolar). */
export function toFileTab(slug: string | null): FileTab {
  return TABS.find((tab) => FILE_TAB_SLUGS[tab] === slug) ?? DEFAULT_TAB;
}

/** Bir sekmeyi doğrudan açan adres eki, örn. `?sekme=kumas`. */
export function fileTabSearch(tab: FileTab): string {
  return `?${FILE_TAB_PARAM}=${FILE_TAB_SLUGS[tab]}`;
}

/**
 * Açık sekme hem ekranda hem adreste tutulur: sayfa yenilenince ve bağlantıyla gelince aynı sekme açılır. Sekmeye
 * basınca ekran hemen değişir (adresin güncellenmesi beklenmez); adres başka yerden değişirse (ör. "sonraki işlem"
 * düğmesi) açık sekme ona uyar.
 */
export function useFileTab(): readonly [FileTab, (tab: FileTab) => void] {
  const [params, setParams] = useSearchParams();
  const fromAddress = toFileTab(params.get(FILE_TAB_PARAM));
  const [tab, setLocalTab] = useState(fromAddress);
  const [seenAddress, setSeenAddress] = useState(fromAddress);
  if (fromAddress !== seenAddress) {
    setSeenAddress(fromAddress);
    setLocalTab(fromAddress);
  }
  const setTab = (next: FileTab) => {
    setLocalTab(next);
    setParams(
      (previous) => {
        const updated = new URLSearchParams(previous);
        updated.set(FILE_TAB_PARAM, FILE_TAB_SLUGS[next]);
        return updated;
      },
      { replace: true, preventScrollReset: true },
    );
  };
  return [tab, setTab];
}
