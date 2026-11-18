import type { Resolution } from '@iplik/api-client';

/** Kararı yalnızca müdür verir ve yalnızca bu ikisini; haklı kararın faturası Faturalanacaklar'dan girilir. */
export const RESOLUTIONS: Resolution[] = ['CLAIM_JUSTIFIED', 'CLAIM_UNJUSTIFIED'];
