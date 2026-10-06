package com.meteor.shipping.api;

import com.meteor.shared.Address;
import com.meteor.shipping.application.ShippingResult;
import com.meteor.shipping.application.ShippingUseCase;
import com.meteor.shipping.domain.ShippingStatus;
import com.meteor.support.web.ApiControllerAdvice;
import com.meteor.test.api.RestDocsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.restdocs.payload.JsonFieldType;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShippingControllerTest extends RestDocsTest {

    private ShippingUseCase shippingUseCase;

    @BeforeEach
    void setUp() {
        shippingUseCase = mock(ShippingUseCase.class);
        mockMvc = mockController(new ShippingController(shippingUseCase), new ApiControllerAdvice());
    }

    @Test
    void ship() throws Exception {
        when(shippingUseCase.ship(eq(1L))).thenReturn(
                new ShippingResult(1L, 1L, new Address("Seoul", "Teheran-ro 1", "06000"), ShippingStatus.SHIPPED));

        mockMvc.perform(post("/api/v1/shippings/{shippingId}/ship", 1L))
            .andExpect(status().isOk())
            .andDo(document("shipping-ship", preprocessResponse(prettyPrint()),
                    pathParameters(parameterWithName("shippingId").description("배송 ID")),
                    responseFields(fieldWithPath("id").type(JsonFieldType.NUMBER).description("배송 ID"),
                            fieldWithPath("orderId").type(JsonFieldType.NUMBER).description("주문 ID"),
                            fieldWithPath("address.city").type(JsonFieldType.STRING).description("시"),
                            fieldWithPath("address.street").type(JsonFieldType.STRING).description("도로명"),
                            fieldWithPath("address.zipCode").type(JsonFieldType.STRING).description("우편번호"),
                            fieldWithPath("status").type(JsonFieldType.STRING).description("배송 상태"))));
    }

}
