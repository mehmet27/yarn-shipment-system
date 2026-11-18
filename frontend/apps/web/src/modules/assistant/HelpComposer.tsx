import { useTranslation } from '@iplik/i18n';
import { Button, Stack, Textarea } from '@iplik/ui';
import { useState } from 'react';

const MAX_QUESTION_LENGTH = 1000;
const ROWS = 2;
const TOUCH_TARGET_PX = 44;

type HelpComposerProps = {
  isSending: boolean;
  onSend: (question: string) => void;
};

/** Soru kutusu: Enter gönderir, Shift+Enter yeni satır açar. */
export function HelpComposer({ isSending, onSend }: HelpComposerProps) {
  const { t } = useTranslation();
  const [draft, setDraft] = useState('');
  const question = draft.trim();
  function submit(): void {
    if (question === '' || isSending) {
      return;
    }
    onSend(question);
    setDraft('');
  }
  return (
    <Stack gap="xs">
      <Textarea
        value={draft}
        onChange={(event) => {
          setDraft(event.currentTarget.value);
        }}
        onKeyDown={(event) => {
          if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            submit();
          }
        }}
        placeholder={t('assistant.placeholder')}
        aria-label={t('assistant.placeholder')}
        rows={ROWS}
        maxLength={MAX_QUESTION_LENGTH}
      />
      <Button onClick={submit} disabled={question === '' || isSending} mih={TOUCH_TARGET_PX}>
        {t('assistant.send')}
      </Button>
    </Stack>
  );
}
