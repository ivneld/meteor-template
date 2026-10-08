package com.meteor.query.api;

import com.meteor.query.application.OrderDetailQuery;
import com.meteor.query.application.OrderDetailView;
import com.meteor.support.web.ApiControllerAdvice;
import com.meteor.test.api.RestDocsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.restdocs.payload.JsonFieldType;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderDetailControllerTest extends RestDocsTest {

    private OrderDetailQuery orderDetailQuery;

    @BeforeEach
    void setUp() {
        orderDetailQuery = mock(OrderDetailQuery.class);
        mockMvc = mockController(new OrderDetailController(orderDetailQuery), new ApiControllerAdvice());
    }

    @Test
    void orderDetail() throws Exception {
        when(orderDetailQuery.findById(eq(1L))).thenReturn(new OrderDetailView(1L, "keyboard", 2, 50_000, 100_000,
                "PAID", "kim", "kim@example.com", 100_000L, "CARD", "READY"));

        mockMvc.perform(get("/api/v1/query/orders/{orderId}", 1L))
            .andExpect(status().isOk())
            .andDo(document("query-order-detail", preprocessResponse(prettyPrint()),
                    pathParameters(parameterWithName("orderId").description("주문 ID")),
                    responseFields(fieldWithPath("orderId").type(JsonFieldType.NUMBER).description("주문 ID"),
                            fieldWithPath("productName").type(JsonFieldType.STRING).description("상품명"),
                            fieldWithPath("quantity").type(JsonFieldType.NUMBER).description("수량"),
                            fieldWithPath("unitPrice").type(JsonFieldType.NUMBER).description("단가(원)"),
                            fieldWithPath("totalAmount").type(JsonFieldType.NUMBER).description("총액(원)"),
                            fieldWithPath("orderStatus").type(JsonFieldType.STRING).description("주문 상태"),
                            fieldWithPath("memberName").type(JsonFieldType.STRING).description("주문자 이름"),
                            fieldWithPath("memberEmail").type(JsonFieldType.STRING).description("주문자 이메일"),
                            fieldWithPath("paidAmount").type(JsonFieldType.NUMBER)
                                .optional()
                                .description("결제 금액(원). 미결제면 null"),
                            fieldWithPath("paymentMethod").type(JsonFieldType.STRING)
                                .optional()
                                .description("결제 수단. 미결제면 null"),
                            fieldWithPath("shippingStatus").type(JsonFieldType.STRING)
                                .optional()
                                .description("배송 상태. 배송 전이면 null"))));
    }

}
