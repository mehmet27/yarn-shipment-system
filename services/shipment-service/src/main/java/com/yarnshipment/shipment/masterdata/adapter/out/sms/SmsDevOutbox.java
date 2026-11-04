package com.yarnshipment.shipment.masterdata.adapter.out.sms;

import com.yarnshipment.shipment.masterdata.domain.model.PhoneNumber;

/** Sahte göndericinin mesajı test sırasında görülebilsin diye kaydettiği yer; yalnızca yerel ortamda bean olur. */
interface SmsDevOutbox {

    void record(PhoneNumber to, String text);
}
