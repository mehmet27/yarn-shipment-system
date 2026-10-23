package com.yarnshipment.shipment.masterdata.domain.model;

/** Giriş hesabının Keycloak rolü ve kaydı işaret eden token özniteliği. */
public enum LoginRole {
    DRIVER("driver", "driver_id"),
    SUBCONTRACTOR("subcontractor", "subcontractor_id");

    private final String realmRole;
    private final String ownerAttribute;

    LoginRole(String realmRole, String ownerAttribute) {
        this.realmRole = realmRole;
        this.ownerAttribute = ownerAttribute;
    }

    public String realmRole() {
        return realmRole;
    }

    /** Kullanıcıya yazılan ve token'a claim olarak giden öznitelik adı. */
    public String ownerAttribute() {
        return ownerAttribute;
    }
}
