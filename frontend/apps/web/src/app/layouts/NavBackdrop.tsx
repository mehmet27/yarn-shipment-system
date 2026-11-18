import { Box } from '@iplik/ui';

const HEADER_HEIGHT_PX = 56;
const BACKDROP_Z_INDEX = 99;
const BACKDROP_STYLE = {
  position: 'fixed',
  top: HEADER_HEIGHT_PX,
  right: 0,
  bottom: 0,
  left: 0,
  zIndex: BACKDROP_Z_INDEX,
  background: 'var(--mantine-color-dark-9)',
  opacity: 0.25,
} as const;

/** Telefonda menü açıkken içeriğe dokunmak menüyü kapatır; menü içeriği daraltmaz, üstüne biner. */
export function NavBackdrop({ onClose }: { readonly onClose: () => void }) {
  return <Box hiddenFrom="sm" style={BACKDROP_STYLE} onClick={onClose} aria-hidden="true" />;
}
