import axios from 'axios';

export {
  confirmEvidenceUpload,
  EvidenceSource,
  EvidenceType,
  ReadingProblem,
  readEvidenceTicket,
  requestEvidenceUpload,
  TicketReadingStatus,
} from './generated/evidenceService';
export type {
  EvidenceView,
  RequestUploadRequest,
  TicketReadingView,
  UploadTicketView,
} from './generated/evidenceService';

/** Depoya yükleme başarısız; gateway hatası (Problem) değildir. */
export class StorageUploadError extends Error {}

/**
 * Dosyayı imzalı adrese doğrudan yükler: gateway'den geçmez, bu yüzden oturum çerezi ve CSRF başlığı
 * gönderilmez (ortak istemci yerine çıplak axios). Başlıklar bilet cevabındaki gibi aynen verilir.
 */
export async function uploadToStorage(
  url: string,
  headers: Record<string, string>,
  file: File,
): Promise<void> {
  try {
    await axios.put(url, file, { headers, withCredentials: false });
  } catch (cause) {
    throw new StorageUploadError('Depo yüklemesi başarısız', { cause });
  }
}
