/** Sayfadaki bölüme yumuşakça kaydırır; bölüm ekranda yoksa hiçbir şey yapmaz. */
export function scrollToSection(sectionId: string): void {
  document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

/** Sabit üst çubuğun altında kalmasın diye kaydırılan bölümlere verilen üst boşluk. */
export const SECTION_SCROLL_MARGIN_PX = 72;
