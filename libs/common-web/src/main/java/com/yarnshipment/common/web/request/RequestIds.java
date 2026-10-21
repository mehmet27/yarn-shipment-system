package com.yarnshipment.common.web.request;

/** İstek kimliğinin başlık, log bağlamı ve istek özniteliği adları. */
public final class RequestIds {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    public static final String ATTRIBUTE = RequestIds.class.getName() + ".requestId";

    private RequestIds() {}
}
