import { useTranslation } from '@iplik/i18n';
import { ActionIcon, Group, NavIcon, Paper, Text } from '@iplik/ui';
import { HelpComposer } from './HelpComposer';
import { HelpMessages } from './HelpMessages';
import type { ChatEntry, HelpChatError } from './useHelpChat';

const TOUCH_TARGET_PX = 44;
// Düğmenin (48) üstünde durur; dar ekranda kenarlardan 16 boşluk kalır.
const PANEL_STYLE = {
  position: 'fixed',
  right: 16,
  bottom: 76,
  zIndex: 300,
  width: 'min(380px, calc(100vw - 32px))',
  height: 'min(520px, calc(100dvh - 160px))',
  display: 'flex',
  flexDirection: 'column',
} as const;

type HelpPanelProps = {
  entries: readonly ChatEntry[];
  error: HelpChatError | null;
  isSending: boolean;
  onSend: (question: string) => void;
  onClose: () => void;
};

/** Sohbet penceresi. Konuşma üstteki bileşende durur; pencere kapanıp açılınca kaybolmaz. */
export function HelpPanel({ entries, error, isSending, onSend, onClose }: HelpPanelProps) {
  const { t } = useTranslation();
  return (
    <Paper
      shadow="lg"
      radius="md"
      withBorder
      p="sm"
      style={PANEL_STYLE}
      role="dialog"
      aria-label={t('assistant.title')}
    >
      <Group justify="space-between" wrap="nowrap" mb="xs">
        <Text fw={600}>{t('assistant.title')}</Text>
        <ActionIcon
          variant="subtle"
          color="gray"
          size={TOUCH_TARGET_PX}
          onClick={onClose}
          aria-label={t('assistant.close')}
        >
          <NavIcon name="close" />
        </ActionIcon>
      </Group>
      <HelpMessages entries={entries} error={error} isSending={isSending} />
      <Text size="xs" c="dimmed" mb="xs">
        {t('assistant.notice')}
      </Text>
      <HelpComposer isSending={isSending} onSend={onSend} />
    </Paper>
  );
}
