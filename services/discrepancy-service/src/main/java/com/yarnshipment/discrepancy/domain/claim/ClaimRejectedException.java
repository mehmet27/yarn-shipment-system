package com.yarnshipment.discrepancy.domain.claim;

/** İtiraz kuralı tutmadı; neden {@link ClaimRejection} ile taşınır. */
public final class ClaimRejectedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ClaimRejection rejection;

    public ClaimRejectedException(ClaimRejection rejection, String detail) {
        super("Quality claim rejected reason=" + rejection + " " + detail);
        this.rejection = rejection;
    }

    public ClaimRejection rejection() {
        return rejection;
    }
}
