import { useTranslation } from '@iplik/i18n';
import { TypedEvidenceUpload } from './TypedEvidenceUpload';

export type WaybillUploadProps = {
  shipmentId: string;
  /** Yükleme onaylanınca kanıt kimliği; tamamlama isteğine bağlanır. */
  onUploaded: (evidenceId: string) => void;
};

/** İrsaliye fotoğrafı/PDF'i: yükleyip onaylar, dönen kanıt kimliğini üst bileşene verir. */
export function WaybillUpload({ shipmentId, onUploaded }: WaybillUploadProps) {
  const { t } = useTranslation();
  return (
    <TypedEvidenceUpload
      shipmentId={shipmentId}
      evidenceType="WAYBILL"
      label={t('shipment.complete.waybillFile')}
      doneLabel={t('shipment.complete.waybillUploaded')}
      onUploaded={onUploaded}
    />
  );
}
