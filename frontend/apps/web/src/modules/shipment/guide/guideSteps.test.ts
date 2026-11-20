import { describe, expect, it } from 'vitest';
import type { GuideAbilities } from './guideSteps';
import { GUIDE_STEPS, toGuideTask, toGuideView } from './guideSteps';

const NOBODY: GuideAbilities = { actions: [], canRecordFabric: false, canMarkInvoice: false };

describe('toGuideTask', () => {
  it('taslakta çuval yoksa önce çuval, varsa çıkış tartısı beklenir', () => {
    expect(toGuideTask({ status: 'DRAFT', hasBags: false })).toBe('BAGS');
    expect(toGuideTask({ status: 'DRAFT', hasBags: true })).toBe('WEIGH');
  });

  it('kantar uyuşmazlığı, yolda ve iptal durumlarını kendi işine çevirir', () => {
    expect(toGuideTask({ status: 'WEIGHT_MISMATCH', hasBags: true })).toBe('MISMATCH');
    expect(toGuideTask({ status: 'AWAITING_DELIVERY', hasBags: true })).toBe('DELIVER');
    expect(toGuideTask({ status: 'CANCELLED', hasBags: false })).toBe('CANCELLED');
  });

  it('tamamlanmış sevkiyatta fire dosyasının durumuna bakar', () => {
    const completed = { status: 'COMPLETED', hasBags: true } as const;
    expect(toGuideTask(completed)).toBe('SETTLING');
    expect(
      toGuideTask({ ...completed, fire: { status: 'AWAITING_FABRIC', remainingKg: '0.000' } }),
    ).toBe('FABRIC');
    expect(
      toGuideTask({ ...completed, fire: { status: 'TO_INVOICE', remainingKg: '12.500' } }),
    ).toBe('INVOICE');
    expect(
      toGuideTask({ ...completed, fire: { status: 'TO_INVOICE', remainingKg: '0.000' } }),
    ).toBe('CLOSE');
    expect(toGuideTask({ ...completed, fire: { status: 'INVOICED', remainingKg: '0.000' } })).toBe(
      'DONE',
    );
    expect(toGuideTask({ ...completed, fire: { status: 'CLOSED', remainingKg: '0.000' } })).toBe(
      'DONE',
    );
  });
});

describe('toGuideView', () => {
  it('sıra kullanıcıdaysa düğmeyi verir, değilse yalnızca kimin yapacağını söyler', () => {
    const input = { status: 'DRAFT', hasBags: false } as const;
    const own = toGuideView(input, { ...NOBODY, actions: ['ASSIGN_BAGS', 'RECORD_DISPATCH'] });
    expect(own).toMatchObject({ isOwn: true, actor: 'WAREHOUSE', stepIndex: 1 });
    expect(own.action).toEqual({ kind: 'tab', tab: 'bags' });
    const other = toGuideView(input, { ...NOBODY, actions: ['CANCEL'] });
    expect(other).toMatchObject({ isOwn: false, actor: 'WAREHOUSE', action: undefined });
  });

  it('kantar uyuşmazlığı tartı adımında müdürü bekler', () => {
    const view = toGuideView(
      { status: 'WEIGHT_MISMATCH', hasBags: true },
      { ...NOBODY, actions: ['RESOLVE_WEIGHT_MISMATCH'] },
    );
    expect(view).toMatchObject({ isOwn: true, actor: 'MANAGER' });
    expect(view.stepIndex).toBe(GUIDE_STEPS.indexOf('WEIGH'));
  });

  it('kumaş ve fatura adımlarında izne bakar; faturalanacak kiloyu yalnızca fatura adımında taşır', () => {
    const fabric = { status: 'AWAITING_FABRIC', remainingKg: '0.000' } as const;
    const invoice = { status: 'TO_INVOICE', remainingKg: '12.500' } as const;
    const completed = { status: 'COMPLETED', hasBags: true } as const;
    expect(
      toGuideView({ ...completed, fire: fabric }, { ...NOBODY, canRecordFabric: true }),
    ).toMatchObject({ isOwn: true, actor: 'QUALITY', remainingKg: undefined });
    expect(
      toGuideView({ ...completed, fire: invoice }, { ...NOBODY, canMarkInvoice: true }),
    ).toMatchObject({
      isOwn: true,
      actor: 'MANAGER',
      remainingKg: '12.500',
      action: { kind: 'link', to: '/panel/faturalama' },
    });
    expect(toGuideView({ ...completed, fire: invoice }, NOBODY).isOwn).toBe(false);
  });

  it('biten süreçte bütün adımlar tamamdır ve kimse beklenmez', () => {
    const view = toGuideView(
      { status: 'COMPLETED', hasBags: true, fire: { status: 'CLOSED', remainingKg: '0.000' } },
      { ...NOBODY, canMarkInvoice: true },
    );
    expect(view).toMatchObject({ stepIndex: GUIDE_STEPS.length, isOwn: false, actor: undefined });
  });
});
