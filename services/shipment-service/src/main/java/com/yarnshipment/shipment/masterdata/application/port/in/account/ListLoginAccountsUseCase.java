package com.yarnshipment.shipment.masterdata.application.port.in.account;

import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;

/**
 * Giriş hesabı olan kullanıcıların sayfalı listesi (yalnızca yönetici). Arama giriş adı, ad ve e-postada yapılır;
 * şifre hiçbir zaman dönmez.
 */
public interface ListLoginAccountsUseCase {

    MasterDataPage<LoginAccountSummary> listLoginAccounts(MasterDataQuery query);
}
