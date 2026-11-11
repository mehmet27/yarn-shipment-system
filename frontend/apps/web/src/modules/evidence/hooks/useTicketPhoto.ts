import type { EvidenceType } from '@iplik/api-client';
import { useState } from 'react';
import { checkImageQuality } from '../imageQuality';
import type { QualityIssue } from '../imageQuality';
import type { TicketOutcome } from '../ticketTypes';
import { fileProblem, PHOTO_MIME_TYPES } from '../uploadTypes';
import type { FileProblem } from '../uploadTypes';
import { useUploadEvidence } from './useUploadEvidence';

/** Çekim durumu: seçilen dosya, kalite uyarıları ve yükleme; her çekim yeni bir kanıt kaydıdır. */
export function useTicketPhoto(
  shipmentId: string,
  evidenceType: EvidenceType,
  onOutcome: (outcome: TicketOutcome | undefined) => void,
) {
  const mutation = useUploadEvidence(shipmentId);
  const [file, setFile] = useState<File>();
  const [issues, setIssues] = useState<readonly QualityIssue[]>([]);
  const [problem, setProblem] = useState<FileProblem>();
  const pick = (picked: File) => {
    const found = fileProblem(picked, PHOTO_MIME_TYPES);
    setProblem(found);
    if (found !== undefined) {
      setFile(undefined);
      return;
    }
    setFile(picked);
    onOutcome(undefined);
    void checkImageQuality(picked).then(setIssues);
  };
  const send = (target: File) => {
    mutation.mutate(
      { file: target, evidenceType },
      {
        onSuccess: () => {
          setFile(undefined);
          onOutcome({ kind: 'PHOTO' });
        },
      },
    );
  };
  return { mutation, file, issues, problem, pick, send };
}
