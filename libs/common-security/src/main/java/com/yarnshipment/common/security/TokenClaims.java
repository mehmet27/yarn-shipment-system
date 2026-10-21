package com.yarnshipment.common.security;

/** Keycloak token'ındaki claim adları (D-4, yetkilendirme analizi). Realm mapper'ları bu adları üretir. */
public final class TokenClaims {

    public static final String ROLES = "roles";
    public static final String SUBCONTRACTOR_ID = "subcontractor_id";
    public static final String DRIVER_ID = "driver_id";
    public static final String SITE_IDS = "site_ids";
    public static final String PREFERRED_USERNAME = "preferred_username";
    public static final String AMR = "amr";
    public static final String ACR = "acr";

    private TokenClaims() {}
}
