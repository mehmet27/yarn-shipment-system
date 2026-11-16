import { useState } from 'react';
import { checkImageQuality } from '../imageQuality';
import type { QualityIssue } from '../imageQuality';
import { fileProblem, PHOTO_MIME_TYPES } from '../uploadTypes';
import type { FileProblem } from '../uploadTypes';

/**
 * Çekim durumu: seçilen dosya ve kalite uyarıları. Fotoğraf burada yüklenmez; işlem onaylanınca (ör. çıkış tartısı
 * "Kaydet") formun sahibi yükler.
 */
export function useTicketPhoto(
  file: File | undefined,
  onFileChange: (file: File | undefined) => void,
) {
  const [issues, setIssues] = useState<readonly QualityIssue[]>([]);
  const [problem, setProblem] = useState<FileProblem>();
  const pick = (picked: File) => {
    const found = fileProblem(picked, PHOTO_MIME_TYPES);
    setProblem(found);
    if (found !== undefined) {
      onFileChange(undefined);
      setIssues([]);
      return;
    }
    onFileChange(picked);
    void checkImageQuality(picked).then(setIssues);
  };
  return { file, issues, problem, pick };
}
