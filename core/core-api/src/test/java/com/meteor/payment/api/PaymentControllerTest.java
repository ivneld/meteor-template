package com.meteor.payment.api;

import com.meteor.payment.api.request.PaymentPayRequest;
import com.meteor.payment.domain.Payment;
import com.meteor.payment.application.PaymentUseCase;
import com.meteor.payment.enums.PaymentMethod;
import com.meteor.shared.Money;
import com.meteor.support.web.ApiControllerAdvice;
import com.meteor.test.api.RestDocsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest extends RestDocsTest {

    private PaymentUseCase paymentUseCase;

    @BeforeEach
    void setUp() {
        paymentUseCase = mock(PaymentUseCase.class);
        mockMvc = mockController(new PaymentController(paymentUseCase), new ApiControllerAdvice());
    }

    @Test
    void pay() throws Exception {
        when(paymentUseCase.pay(any())).thenReturn(Payment.restore(1L, 1L, Money.of(100_000), PaymentMethod.CARD));

        mockMvc.perform(post("/api/v1/payments").contentType(MediaType.APPLICATION_JSON)
            .content(JsonMapper.builder().build().writeValueAsString(new PaymentPayRequest(1L, PaymentMethod.CARD))))
            .andExpect(status().isOk())
            .andDo(document("payment-pay", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(fieldWithPath("orderId").type(JsonFieldType.NUMBER).description("주문 ID"),
                            fieldWithPath("method").type(JsonFieldType.STRING).description("결제 수단")),
                    responseFields(fieldWithPath("id").type(JsonFieldType.NUMBER).description("결제 ID"),
                            fieldWithPath("orderId").type(JsonFieldType.NUMBER).description("주문 ID"),
                            fieldWithPath("amount").type(JsonFieldType.NUMBER).description("승인 금액(원)"),
                            fieldWithPath("method").type(JsonFieldType.STRING).description("결제 수단"))));
    }

}
