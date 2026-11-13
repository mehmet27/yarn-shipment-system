import type * as PdfMake from 'pdfmake/build/pdfmake';

type PdfMakeModule = typeof PdfMake;

function isPdfMake(candidate: unknown): candidate is PdfMakeModule {
  return (
    typeof candidate === 'object' &&
    candidate !== null &&
    'createPdf' in candidate &&
    'addVirtualFileSystem' in candidate
  );
}

/**
 * pdfmake UMD paketidir: Vite geliştirme sunucusunda dinamik içe aktarma işlevleri `default` altında
 * verir ve `pdfMake.addVirtualFileSystem` bulunamaz. Her iki biçimde de çalışan kütüphaneyi döner.
 */
export async function loadPdfMake(): Promise<PdfMakeModule> {
  const [module, fonts] = await Promise.all([
    import('pdfmake/build/pdfmake'),
    import('pdfmake/build/vfs_fonts'),
  ]);
  const candidate: unknown = 'default' in module ? module.default : module;
  const pdfMake = isPdfMake(candidate) ? candidate : module;
  pdfMake.addVirtualFileSystem(fonts.default);
  return pdfMake;
}
