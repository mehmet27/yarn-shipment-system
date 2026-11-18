import type { Resolution } from '@iplik/api-client';

/** Kalite ve müdür aynı iki kararı verir; haklı kararın faturası Faturalanacaklar'dan girilir. */
export const RESOLUTIONS: Resolution[] = ['CLAIM_JUSTIFIED', 'CLAIM_UNJUSTIFIED'];
