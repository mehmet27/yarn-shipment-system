import type { ShipmentAction, ShipmentStatus } from '@iplik/api-client';
import type { FireStatus } from '@iplik/api-client/settlements';
import type { FileTab } from '../components/fileTabs';

/**
 * Bir sevkiyatın baştan sona adımları ve her adımı kimin yaptığı. Akışın kendisi sunucudadır (durumlar ve
 * `allowedActions`); burası yalnızca o akışı ekranda adım adım göstermek için okur, hiçbir kuralı değiştirmez.
 */
export const GUIDE_STEPS = ['CREATE', 'BAGS', 'WEIGH', 'DELIVER', 'FABRIC', 'FIRE'] as const;
export type GuideStep = (typeof GUIDE_STEPS)[number];

/** Adımı yapan kişi; adı `users.role.*` metinlerinden gelir (her ekranda aynı sözcük). */
export type GuideActor = 'MANAGER' | 'WAREHOUSE' | 'QUALITY';

export const STEP_ACTOR: Readonly<Record<GuideStep, GuideActor>> = {
  CREATE: 'MANAGER',
  BAGS: 'WAREHOUSE',
  WEIGH: 'WAREHOUSE',
  DELIVER: 'MANAGER',
  FABRIC: 'QUALITY',
  FIRE: 'MANAGER',
};

/**
 * Şu an beklenen iş. Adımlardan farkı: kantar uyuşmazlığı tartı adımında müdürü bekler, fire dosyası adımı
 * "fatura gir" ya da "dosyayı kapat" olabilir, tamamlanmış sevkiyatta fire dosyası okunamıyorsa `SETTLING` kalır.
 */
export type GuideTask =
  | 'BAGS'
  | 'WEIGH'
  | 'MISMATCH'
  | 'DELIVER'
  | 'FABRIC'
  | 'INVOICE'
  | 'CLOSE'
  | 'SETTLING'
  | 'DONE'
  | 'CANCELLED';

const STEP_COUNT = GUIDE_STEPS.length;

/** İşin adım çubuğundaki sırası (0'dan başlar); biten süreçte adım sayısı, iptalde -1. */
const TASK_STEP_INDEX: Readonly<Record<GuideTask, number>> = {
  BAGS: GUIDE_STEPS.indexOf('BAGS'),
  WEIGH: GUIDE_STEPS.indexOf('WEIGH'),
  MISMATCH: GUIDE_STEPS.indexOf('WEIGH'),
  DELIVER: GUIDE_STEPS.indexOf('DELIVER'),
  FABRIC: GUIDE_STEPS.indexOf('FABRIC'),
  SETTLING: GUIDE_STEPS.indexOf('FABRIC'),
  INVOICE: GUIDE_STEPS.indexOf('FIRE'),
  CLOSE: GUIDE_STEPS.indexOf('FIRE'),
  DONE: STEP_COUNT,
  CANCELLED: -1,
};

/** İşi yapacak kişi; biten, iptal edilen ve fire dosyası okunamayan sevkiyatta kimse beklenmez. */
const TASK_ACTOR: Readonly<Partial<Record<GuideTask, GuideActor>>> = {
  BAGS: 'WAREHOUSE',
  WEIGH: 'WAREHOUSE',
  MISMATCH: 'MANAGER',
  DELIVER: 'MANAGER',
  FABRIC: 'QUALITY',
  INVOICE: 'MANAGER',
  CLOSE: 'MANAGER',
};

/** Sevkiyat dosyasında kaydırılarak gidilen bölümlerin kimlikleri. */
export const WEIGHT_DECISION_SECTION_ID = 'kantar-karari';
export const COMPLETE_SECTION_ID = 'irsaliye-tamamla';

/** "Sonraki işlem" düğmesinin yaptığı şey: başka ekrana gider, dosyada sekme açar ya da bölüme kaydırır. */
export type GuideAction =
  | { kind: 'link'; to: string }
  | { kind: 'tab'; tab: FileTab }
  | { kind: 'scroll'; sectionId: string };

const TASK_ACTION: Readonly<Partial<Record<GuideTask, GuideAction>>> = {
  BAGS: { kind: 'tab', tab: 'bags' },
  WEIGH: { kind: 'link', to: 'cikis' },
  MISMATCH: { kind: 'scroll', sectionId: WEIGHT_DECISION_SECTION_ID },
  DELIVER: { kind: 'scroll', sectionId: COMPLETE_SECTION_ID },
  FABRIC: { kind: 'tab', tab: 'fabric' },
  INVOICE: { kind: 'link', to: '/panel/faturalama' },
  CLOSE: { kind: 'tab', tab: 'fire' },
};

export type FireProgress = { status: FireStatus; remainingKg: string };

export type GuideInput = {
  status: ShipmentStatus;
  /** Taslakta çuval eklendi mi; çuval eklenmeden çıkış tartısı girilemez. */
  hasBags: boolean;
  /** Tamamlanmış sevkiyatın fire dosyası; kullanıcı göremiyorsa ya da dosya yoksa boş. */
  fire?: FireProgress | undefined;
};

function toFireTask(fire: FireProgress | undefined): GuideTask {
  if (fire === undefined) return 'SETTLING';
  switch (fire.status) {
    case 'AWAITING_FABRIC':
      return 'FABRIC';
    case 'TO_INVOICE':
      return Number(fire.remainingKg) > 0 ? 'INVOICE' : 'CLOSE';
    case 'INVOICED':
    case 'CLOSED':
      return 'DONE';
  }
}

/** Sevkiyatın durumundan şu an beklenen işi bulur. */
export function toGuideTask({ status, hasBags, fire }: GuideInput): GuideTask {
  switch (status) {
    case 'DRAFT':
      return hasBags ? 'WEIGH' : 'BAGS';
    case 'WEIGHT_MISMATCH':
      return 'MISMATCH';
    case 'AWAITING_DELIVERY':
      return 'DELIVER';
    case 'COMPLETED':
      return toFireTask(fire);
    case 'CANCELLED':
      return 'CANCELLED';
  }
}

export type GuideAbilities = {
  /** Sunucunun bu kullanıcıya bu sevkiyatta izin verdiği işlemler. */
  actions: readonly ShipmentAction[];
  canRecordFabric: boolean;
  canMarkInvoice: boolean;
};

const NEVER_OWN = () => false;

/** İşin kullanıcıya açık olup olmadığını söyleyen denetimler; yalnızca sunucunun verdiği işlem ve izinlere bakar. */
const OWNERSHIP: Readonly<Record<GuideTask, (abilities: GuideAbilities) => boolean>> = {
  BAGS: ({ actions }) => actions.includes('ASSIGN_BAGS'),
  WEIGH: ({ actions }) => actions.includes('RECORD_DISPATCH'),
  MISMATCH: ({ actions }) => actions.includes('RESOLVE_WEIGHT_MISMATCH'),
  DELIVER: ({ actions }) => actions.includes('COMPLETE'),
  FABRIC: ({ canRecordFabric }) => canRecordFabric,
  INVOICE: ({ canMarkInvoice }) => canMarkInvoice,
  CLOSE: ({ canMarkInvoice }) => canMarkInvoice,
  SETTLING: NEVER_OWN,
  DONE: NEVER_OWN,
  CANCELLED: NEVER_OWN,
};

/** İş oturumdaki kullanıcının mı (rol adına değil, izne bakılır). */
export function isOwnTask(task: GuideTask, abilities: GuideAbilities): boolean {
  return OWNERSHIP[task](abilities);
}

export type ShipmentGuideView = {
  task: GuideTask;
  /** Adım çubuğundaki geçerli adım; biten süreçte adım sayısı, iptalde -1. */
  stepIndex: number;
  actor: GuideActor | undefined;
  /** Sıra oturumdaki kullanıcıda mı. */
  isOwn: boolean;
  /** Sıra kullanıcıdaysa basacağı tek düğme. */
  action: GuideAction | undefined;
  /** Fatura adımında faturası henüz kesilmemiş kilo. */
  remainingKg: string | undefined;
};

export function toGuideView(input: GuideInput, abilities: GuideAbilities): ShipmentGuideView {
  const task = toGuideTask(input);
  const isOwn = isOwnTask(task, abilities);
  return {
    task,
    stepIndex: TASK_STEP_INDEX[task],
    actor: TASK_ACTOR[task],
    isOwn,
    action: isOwn ? TASK_ACTION[task] : undefined,
    remainingKg: task === 'INVOICE' ? input.fire?.remainingKg : undefined,
  };
}
