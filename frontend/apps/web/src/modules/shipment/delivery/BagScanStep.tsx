import { useTranslation } from '@iplik/i18n';
import { Button, CloseButton, Group, Paper, Stack, Text, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { addBagKey } from './deliveryDraft';
import type { StepProps } from './types';

/** Barkod okuyucu klavye gibi yazar ve Enter gönderir; aynı alan elle girişte de kullanılır. */
export function BagScanStep({ draft, onChange }: StepProps) {
  const { t } = useTranslation();
  const [input, setInput] = useState('');
  const add = () => {
    onChange({ bagKeys: addBagKey(draft.bagKeys, input) });
    setInput('');
  };
  return (
    <Stack>
      <Group align="flex-end" wrap="nowrap">
        <TextInput
          size="lg"
          style={{ flex: 1 }}
          label={t('delivery.bags.key')}
          value={input}
          onChange={(event) => {
            setInput(event.currentTarget.value);
          }}
          onKeyDown={(event) => {
            if (event.key === 'Enter') add();
          }}
        />
        <Button size="lg" onClick={add}>
          {t('delivery.bags.add')}
        </Button>
      </Group>
      <Text fw={700}>{t('delivery.bags.scanned', { count: draft.bagKeys.length })}</Text>
      <Stack gap={4}>
        {draft.bagKeys.map((key) => (
          <Paper key={key} withBorder px="sm" py={4}>
            <Group justify="space-between">
              <Text>{key}</Text>
              <CloseButton
                aria-label={t('delivery.bags.remove', { key })}
                onClick={() => {
                  onChange({ bagKeys: draft.bagKeys.filter((item) => item !== key) });
                }}
              />
            </Group>
          </Paper>
        ))}
      </Stack>
    </Stack>
  );
}
