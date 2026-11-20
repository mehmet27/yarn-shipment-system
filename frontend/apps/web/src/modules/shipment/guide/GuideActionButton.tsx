import { Button, Icon } from '@iplik/ui';
import { Link } from 'react-router';
import { FILE_TABS_SECTION_ID, useFileTab } from '../components/fileTabs';
import type { GuideAction } from './guideSteps';
import { scrollToSection } from './scrollToSection';

const BUTTON_WIDTH = { base: '100%', sm: 'auto' } as const;

/** Sıradaki adımın tek düğmesi: başka ekrana götürür, dosyada ilgili sekmeyi açar ya da forma kaydırır. */
export function GuideActionButton({ action, label }: { action: GuideAction; label: string }) {
  const [, setTab] = useFileTab();
  const icon = <Icon name="arrowRight" size={16} />;
  if (action.kind === 'link') {
    return (
      <Button
        component={Link}
        to={action.to}
        relative="path"
        w={BUTTON_WIDTH}
        rightSection={icon}
        data-testid="guide-action"
      >
        {label}
      </Button>
    );
  }
  const open = () => {
    if (action.kind === 'tab') setTab(action.tab);
    scrollToSection(action.kind === 'tab' ? FILE_TABS_SECTION_ID : action.sectionId);
  };
  return (
    <Button w={BUTTON_WIDTH} rightSection={icon} onClick={open} data-testid="guide-action">
      {label}
    </Button>
  );
}
