import { useTranslation } from '@iplik/i18n';
import { ActionIcon, Icon, TextInput } from '@iplik/ui';
import { useState } from 'react';

const MAX_SEARCH_LENGTH = 100;
const SEARCH_WIDTH_PX = 320;

export type ShipmentSearchBoxProps = {
  label: string;
  /** Üst şeritte yer dar: etiket görünmez, ekran okuyucuya okunur. */
  isLabelHidden?: boolean;
  placeholder: string;
  initialValue?: string;
  onSearch: (text: string) => void;
};

/** Arama kutusu: metin Enter'la ya da büyüteç düğmesiyle gönderilir; her tuşta sunucuya gidilmez. */
export function ShipmentSearchBox({
  label,
  isLabelHidden = false,
  placeholder,
  initialValue = '',
  onSearch,
}: ShipmentSearchBoxProps) {
  const { t } = useTranslation();
  const [text, setText] = useState(initialValue);
  return (
    <form
      role="search"
      onSubmit={(event) => {
        event.preventDefault();
        onSearch(text.trim());
      }}
    >
      <TextInput
        type="search"
        label={isLabelHidden ? undefined : label}
        aria-label={isLabelHidden ? label : undefined}
        placeholder={placeholder}
        value={text}
        maxLength={MAX_SEARCH_LENGTH}
        w={{ base: '100%', sm: SEARCH_WIDTH_PX }}
        onChange={(event) => {
          setText(event.currentTarget.value);
        }}
        rightSection={
          <ActionIcon type="submit" variant="subtle" aria-label={t('shipment.list.searchSubmit')}>
            <Icon name="search" />
          </ActionIcon>
        }
      />
    </form>
  );
}
