import { ASSISTANT_PATH, getAssistantStatus } from '@iplik/api-client/assistant';
import { useTranslation } from '@iplik/i18n';
import { Box, Button, NavIcon, useDisclosure, useMediaQuery } from '@iplik/ui';
import { useQuery } from '@tanstack/react-query';
import { HelpPanel } from './HelpPanel';
import { SHORT_SCREEN_QUERY } from './shortScreen';
import { useHelpChat } from './useHelpChat';

const BUTTON_HEIGHT_PX = 48;
const WRAPPER_STYLE = { position: 'fixed', right: 16, bottom: 16, zIndex: 300 } as const;

/**
 * Sağ alttaki "Yardım" balonu ve sohbet penceresi. Sunucuda API anahtarı tanımlı değilse hiç görünmez. Asistan yalnızca
 * bilgi verir; hiçbir kaydı değiştirmez.
 */
export function HelpAssistant() {
  const { t } = useTranslation();
  const { data: status } = useQuery({
    queryKey: [ASSISTANT_PATH, 'status'],
    queryFn: getAssistantStatus,
    staleTime: Infinity,
  });
  const [isOpen, { toggle, close }] = useDisclosure();
  const { entries, error, isSending, send } = useHelpChat();
  const isShortScreen = useMediaQuery(SHORT_SCREEN_QUERY);
  if (status?.available !== true) {
    return null;
  }
  return (
    <>
      {isOpen && (
        <HelpPanel
          entries={entries}
          error={error}
          isSending={isSending}
          onSend={send}
          onClose={close}
        />
      )}
      <Box style={WRAPPER_STYLE} display={isOpen && isShortScreen ? 'none' : undefined}>
        <Button
          onClick={toggle}
          radius="xl"
          h={BUTTON_HEIGHT_PX}
          leftSection={<NavIcon name="info" />}
          aria-expanded={isOpen}
        >
          {t('assistant.open')}
        </Button>
      </Box>
    </>
  );
}
