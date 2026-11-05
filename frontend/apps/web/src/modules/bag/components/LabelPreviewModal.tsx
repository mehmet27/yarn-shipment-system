import { useTranslation } from '@iplik/i18n';
import { Anchor, Button, Group, Modal, Text } from '@iplik/ui';
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
  // Telefon ve tablet tarayıcıları PDF'i sayfa içinde göstermez; orada düğmeler öne çıkar.
  const canEmbed = !('pdfViewerEnabled' in navigator) || navigator.pdfViewerEnabled;
  return (
    <Modal opened={url !== undefined} onClose={onClose} title={t('bag.previewTitle')} size="xl">
      {url !== undefined && (
        <>
          {canEmbed ? (
            <iframe
              ref={frame}
              src={url}
              title={t('bag.previewTitle')}
              style={{ width: '100%', height: '60vh', border: 0 }}
            />
          ) : (
            <Text>{t('bag.noEmbed')}</Text>
          )}
          <Group justify="flex-end" mt="md">
            <Anchor href={url} download={FILE_NAME}>
              {t('bag.download')}
            </Anchor>
            <Anchor href={url} target="_blank" rel="noopener">
              {t('bag.openTab')}
            </Anchor>
            {canEmbed && (
              <Button onClick={() => frame.current?.contentWindow?.print()}>
                {t('bag.printNow')}
              </Button>
            )}
          </Group>
        </>
      )}
    </Modal>
  );
}
