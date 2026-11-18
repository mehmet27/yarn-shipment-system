import { useTranslation } from '@iplik/i18n';
import { Box, Group, Loader, Paper, Stack, Text } from '@iplik/ui';
import { useEffect, useRef } from 'react';
import type { ChatEntry, HelpChatError } from './useHelpChat';

const MESSAGES_STYLE = { flex: 1, minHeight: 0, overflowY: 'auto' } as const;
const TEXT_STYLE = { whiteSpace: 'pre-wrap' } as const;

type HelpMessagesProps = {
  entries: readonly ChatEntry[];
  error: HelpChatError | null;
  isSending: boolean;
};

/** Karşılama, konuşma, bekleme ve hata satırları; yeni satır gelince en alta kayar. */
export function HelpMessages({ entries, error, isSending }: HelpMessagesProps) {
  const { t } = useTranslation();
  const endRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    endRef.current?.scrollIntoView({ block: 'end' });
  }, [entries, error, isSending]);
  return (
    <Box style={MESSAGES_STYLE} mb="xs">
      <Stack gap="xs">
        <Text size="sm" style={TEXT_STYLE}>
          {t('assistant.welcome')}
        </Text>
        {entries.map((entry) => (
          <Paper key={entry.id} p="xs" radius="md" withBorder>
            <Text size="xs" c="dimmed">
              {t(entry.role === 'user' ? 'assistant.you' : 'assistant.helper')}
            </Text>
            <Text size="sm" style={TEXT_STYLE}>
              {entry.text}
            </Text>
          </Paper>
        ))}
        {isSending && (
          <Group gap="xs">
            <Loader size="xs" />
            <Text size="sm" c="dimmed">
              {t('assistant.thinking')}
            </Text>
          </Group>
        )}
        {error !== null && (
          <Text size="sm" c="red" role="alert">
            {t(`assistant.errors.${error}`)}
          </Text>
        )}
        <div ref={endRef} />
      </Stack>
    </Box>
  );
}
