package com.yarnshipment.shipment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.weight.ResolveWeightMismatchUseCase;
import com.yarnshipment.shipment.application.port.in.weight.UpdateRecipeCommand;
import com.yarnshipment.shipment.application.port.in.weight.UpdateRecipeUseCase;
import com.yarnshipment.shipment.domain.weight.RecipeWeightMismatchException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Reçete düzeltme ucunun sözleşmesi: izin, istek eşlemesi ve tartı tutmayınca 409. */
@WebMvcTest(controllers = ShipmentWeightDecisionController.class)
class ShipmentRecipeControllerTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000201");
    private static final String URI = "/api/v1/shipments/" + SHIPMENT + "/recipe";
    private static final String BODY = """
            {"reason": "Reçete düzeltildi",
             "lines": [
               {"yarnTypeId": "0192f0a0-0000-7000-8000-0000000000a1", "netKg": "1000.000", "unitPrice": "12.5"},
               {"yarnTypeId": "0192f0a0-0000-7000-8000-0000000000a2", "netKg": "982.500", "unitPrice": "10"}]}""";

    private final MockMvcTester mvc;

    @MockitoBean
    private UpdateRecipeUseCase updateRecipeUseCase;

    @MockitoBean
    private ResolveWeightMismatchUseCase resolveUseCase;

    @Autowired
    ShipmentRecipeControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Müdür reçeteyi gönderir; satırlar sıra numarasıyla use case'e iletilir")
    void shouldUpdateRecipe() {
        MvcTestResult result = put(TestTokens.token("manager").build(), BODY);

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        ArgumentCaptor<UpdateRecipeCommand> command = ArgumentCaptor.forClass(UpdateRecipeCommand.class);
        verify(updateRecipeUseCase).updateRecipe(command.capture());
        assertThat(command.getValue().lines()).hasSize(2);
        assertThat(command.getValue().lines().get(1).lineNo()).isEqualTo(2);
        assertThat(command.getValue().reason()).isEqualTo("Reçete düzeltildi");
    }

    @Test
    @DisplayName("Toplam tartıyı tutmuyorsa 409 RECIPE_WEIGHT_MISMATCH ve kilolar döner")
    void shouldReportMismatch() {
        doThrow(new RecipeWeightMismatchException(
                        new BigDecimal("1990.000"), new BigDecimal("1982.500"), new BigDecimal("5.000")))
                .when(updateRecipeUseCase)
                .updateRecipe(any());

        MvcTestResult result = put(TestTokens.token("manager").build(), BODY);

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("RECIPE_WEIGHT_MISMATCH");
        assertThat(result).bodyJson().extractingPath("$.recipeKg").isEqualTo("1990.000");
        assertThat(result).bodyJson().extractingPath("$.measuredKg").isEqualTo("1982.500");
        assertThat(result).bodyJson().extractingPath("$.toleranceKg").isEqualTo("5.000");
    }

    @Test
    @DisplayName("İptal izni olmayan rol 403 alır")
    void shouldForbidWarehouse() {
        assertThat(put(TestTokens.token("warehouse").build(), BODY)).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(updateRecipeUseCase);
    }

    private MvcTestResult put(Jwt jwt, String body) {
        return mvc.put()
                .uri(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }
}
