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

/** Tarayıcının kendisinin koyduğu, elle verilince konsola "unsafe header" düşen başlıklar (H-21). */
const BROWSER_MANAGED_HEADERS = new Set(['content-length', 'host', 'connection']);

function settableHeaders(headers: Record<string, string>): Record<string, string> {
  return Object.fromEntries(
    Object.entries(headers).filter(([name]) => !BROWSER_MANAGED_HEADERS.has(name.toLowerCase())),
  );
}

/**
 * Dosyayı imzalı adrese doğrudan yükler: gateway'den geçmez, bu yüzden oturum çerezi ve CSRF başlığı
 * gönderilmez (ortak istemci yerine çıplak axios). Bilet cevabındaki başlıklar, tarayıcının kendi
 * koyduğu Content-Length gibileri dışında aynen verilir.
 */
export async function uploadToStorage(
  url: string,
  headers: Record<string, string>,
  file: File,
): Promise<void> {
  try {
    await axios.put(url, file, { headers: settableHeaders(headers), withCredentials: false });
  } catch (cause) {
    throw new StorageUploadError('Depo yüklemesi başarısız', { cause });
  }
}
