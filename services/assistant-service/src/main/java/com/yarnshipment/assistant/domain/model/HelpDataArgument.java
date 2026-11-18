package com.yarnshipment.assistant.domain.model;

/** Asistanın canlı veri sorgusuna ekleyebildiği süzgeçler; modelin kullandığı ad ile uygulama ucundaki ad. */
public enum HelpDataArgument {
    START("baslangic", "from"),
    END("bitis", "to"),
    SUBCONTRACTOR("fasoncu_id", "subcontractorId"),
    SEARCH("ara", "q"),
    STATUS("durum", "status"),
    PAGE("sayfa", "page"),
    /** Sevkiyat numarası; sorgu adresine yazılmaz, sevkiyatın kimliğini bulmak için kullanılır. */
    SHIPMENT_NO("sevkiyat_no", "");

    private final String toolName;
    private final String queryName;

    HelpDataArgument(String toolName, String queryName) {
        this.toolName = toolName;
        this.queryName = queryName;
    }

    /** Modelin kullandığı ad. */
    public String toolName() {
        return toolName;
    }

    /** Uygulama ucundaki sorgu parametresi adı; boşsa adrese yazılmaz. */
    public String queryName() {
        return queryName;
    }
}
