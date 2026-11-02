import { useTranslation } from '@iplik/i18n';
import { Anchor, Button, Group, Modal } from '@iplik/ui';
import { useEffect, useMemo, useRef } from 'react';

export type LabelPreviewModalProps = {
  pdf: Blob | undefined;
  onClose: () => void;
};

const FILE_NAME = 'cuval-etiketleri.pdf';

/**
 * Etiket PDF'ini sayfa içinde gösterir; yazdırma tarayıcının yazdırma penceresinden yapılır. Yeni
 * sekme açılmaz, çünkü istek bittikten sonra açılan pencereyi açılır pencere engelleyicisi durdurur.
 */
export function LabelPreviewModal({ pdf, onClose }: LabelPreviewModalProps) {
  const { t } = useTranslation();
  const frame = useRef<HTMLIFrameElement>(null);
  const url = useMemo(() => (pdf === undefined ? undefined : URL.createObjectURL(pdf)), [pdf]);
  useEffect(
    () => () => {
      if (url !== undefined) URL.revokeObjectURL(url);
    },
    [url],
  );
  return (
    <Modal opened={url !== undefined} onClose={onClose} title={t('bag.previewTitle')} size="xl">
      {url !== undefined && (
        <>
          <iframe
            ref={frame}
            src={url}
            title={t('bag.previewTitle')}
            style={{ width: '100%', height: '60vh', border: 0 }}
          />
          <Group justify="flex-end" mt="md">
            <Anchor href={url} download={FILE_NAME}>
              {t('bag.download')}
            </Anchor>
            <Button onClick={() => frame.current?.contentWindow?.print()}>
              {t('bag.printNow')}
            </Button>
          </Group>
        </>
      )}
    </Modal>
  );
}
