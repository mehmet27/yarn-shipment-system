import { useTranslation } from '@iplik/i18n';
import type { CreateDraft } from './createDraft';
import type { OrderFormTexts } from './orderFormPdf';
import { downloadOrderForm } from './orderFormPdf';

const NOTE_COUNT = 11;
const WIDTH_TYPES = { TUBE: 'TÜP', MAY: 'MAY', OPEN_WIDTH: 'AÇIK EN' };

/** Sipariş formu PDF'ini çevirilerle birlikte indiren fonksiyon; tarih bugündür. */
export function useOrderFormPdf(): (draft: CreateDraft) => Promise<void> {
  const { t } = useTranslation();
  return (draft) => {
    const texts: OrderFormTexts = {
      company: t('create.pdf.company'),
      companyLine: t('create.pdf.companyLine'),
      address: t('create.pdf.address'),
      firm: t('create.pdf.firm'),
      orderNo: t('create.pdf.orderNo'),
      contact: t('create.pdf.contact'),
      date: t('create.pdf.date'),
      columns: Array.from({ length: 7 }, (_, index) => t(`create.pdf.columns.${String(index)}`)),
      total: t('create.pdf.total'),
      notesTitle: t('create.pdf.notesTitle'),
      notes: Array.from({ length: NOTE_COUNT }, (_, index) =>
        t(`create.pdf.notes.${String(index)}`),
      ),
      orderApproval: t('create.pdf.orderApproval'),
      producer: t('create.pdf.producer'),
      approver: t('create.pdf.approver'),
      widthTypes: WIDTH_TYPES,
    };
    const date = new Intl.DateTimeFormat('tr-TR').format(new Date()).replaceAll('.', '/');
    return downloadOrderForm(draft, { texts, date, fileName: t('create.pdf.fileName') });
  };
}
