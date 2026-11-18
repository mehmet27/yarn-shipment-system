package com.yarnshipment.assistant.adapter.out.data;

/** Canlı veri okunamadı (bağlantı kesildi ya da zaman aşımı); asistan kullanıcıya "şu an okunamadı" der. */
final class HelpDataReadException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    HelpDataReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
