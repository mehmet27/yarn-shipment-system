import { useTranslation } from '@iplik/i18n';
import { TypedEvidenceUpload } from './TypedEvidenceUpload';

export type SignedDocumentUploadProps = {
  shipmentId: string;
  /** Yükleme onaylanınca kanıt kimliği; teslim isteğine bağlanır. */
  onUploaded: (evidenceId: string) => void;
};

/** İmzalı irsaliye fotoğrafı/PDF'i: yükleyip onaylar, dönen kanıt kimliğini üst bileşene verir. */
export function SignedDocumentUpload({ shipmentId, onUploaded }: SignedDocumentUploadProps) {
  const { t } = useTranslation();
  return (
    <TypedEvidenceUpload
      shipmentId={shipmentId}
      evidenceType="SIGNED_DELIVERY_DOC"
      label={t('delivery.document.signed')}
      doneLabel={t('delivery.document.signedDone')}
      onUploaded={onUploaded}
    />
  );
}
