package com.meteor.sample.api;

import java.time.LocalDateTime;

import com.meteor.sample.api.request.SampleCreateRequest;
import com.meteor.sample.application.SampleResult;
import com.meteor.sample.application.SampleUseCase;
import com.meteor.shared.SampleStatus;
import com.meteor.shared.error.CoreException;
import com.meteor.shared.error.ErrorCode;
import com.meteor.support.web.ApiControllerAdvice;
import com.meteor.test.api.RestDocsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SampleControllerTest extends RestDocsTest {

    private static final String PROBLEM_JSON = "application/problem+json";

    private SampleUseCase sampleUseCase;

    @BeforeEach
    void setUp() {
        sampleUseCase = mock(SampleUseCase.class);
        mockMvc = mockController(new SampleController(sampleUseCase), new ApiControllerAdvice());
    }

    @Test
    void getSample() throws Exception {
        when(sampleUseCase.find(eq(1L))).thenReturn(result(SampleStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/samples/{sampleId}", 1L).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andDo(document("sample-get", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    pathParameters(parameterWithName("sampleId").description("샘플 ID")), sampleResponseFields()));
    }

    @Test
    void getSampleNotFound() throws Exception {
        when(sampleUseCase.find(eq(999L)))
            .thenThrow(new CoreException(ErrorCode.SAMPLE_NOT_FOUND).property("sampleId", 999L));

        mockMvc.perform(get("/api/v1/samples/{sampleId}", 999L).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(header().string("Content-Type", PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("S001"))
            .andExpect(jsonPath("$.sampleId").value(999))
            .andDo(document("sample-get-not-found", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    pathParameters(parameterWithName("sampleId").description("샘플 ID")),
                    responseFields(fieldWithPath("type").type(JsonFieldType.STRING).description("에러 코드 URI"),
                            fieldWithPath("title").type(JsonFieldType.STRING).description("에러 제목"),
                            fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                            fieldWithPath("instance").type(JsonFieldType.STRING).description("요청 경로"),
                            fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드"),
                            fieldWithPath("sampleId").type(JsonFieldType.NUMBER).description("조회한 샘플 ID"))));
    }

    @Test
    void createSample() throws Exception {
        when(sampleUseCase.create(any())).thenReturn(result(SampleStatus.ACTIVE));

        mockMvc.perform(post("/api/v1/samples").contentType(MediaType.APPLICATION_JSON).content(toJson("meteor")))
            .andExpect(status().isOk())
            .andDo(document("sample-create", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(fieldWithPath("name").type(JsonFieldType.STRING).description("샘플 이름")),
                    sampleResponseFields()));
    }

    @Test
    void createSampleInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/samples").contentType(MediaType.APPLICATION_JSON).content(toJson("")))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("C001"))
            .andExpect(jsonPath("$.errors[0].field").value("name"))
            .andDo(document("sample-create-invalid", preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    responseFields(fieldWithPath("type").type(JsonFieldType.STRING).description("에러 코드 URI"),
                            fieldWithPath("title").type(JsonFieldType.STRING).description("에러 제목"),
                            fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                            fieldWithPath("detail").type(JsonFieldType.STRING).description("상세 설명"),
                            fieldWithPath("instance").type(JsonFieldType.STRING).description("요청 경로"),
                            fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드"),
                            fieldWithPath("errors").type(JsonFieldType.ARRAY).description("필드 검증 오류 목록"),
                            fieldWithPath("errors[].field").type(JsonFieldType.STRING).description("필드 이름"),
                            fieldWithPath("errors[].message").type(JsonFieldType.STRING).description("검증 메시지"))));
    }

    @Test
    void deactivateSample() throws Exception {
        when(sampleUseCase.deactivate(eq(1L))).thenReturn(result(SampleStatus.INACTIVE));

        mockMvc.perform(post("/api/v1/samples/{sampleId}/deactivate", 1L).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INACTIVE"))
            .andDo(document("sample-deactivate", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    pathParameters(parameterWithName("sampleId").description("샘플 ID")), sampleResponseFields()));
    }

    @Test
    void deactivateSampleConflict() throws Exception {
        when(sampleUseCase.deactivate(eq(1L)))
            .thenThrow(new CoreException(ErrorCode.SAMPLE_ALREADY_INACTIVE).property("sampleId", 1L));

        mockMvc.perform(post("/api/v1/samples/{sampleId}/deactivate", 1L).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("S002"))
            .andDo(document("sample-deactivate-conflict", preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint())));
    }

    private static SampleResult result(SampleStatus status) {
        return new SampleResult(1L, "meteor", status, LocalDateTime.now());
    }

    private static ResponseFieldsSnippet sampleResponseFields() {
        return responseFields(fieldWithPath("id").type(JsonFieldType.NUMBER).description("샘플 ID"),
                fieldWithPath("name").type(JsonFieldType.STRING).description("샘플 이름"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("샘플 상태"),
                fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성 시각"));
    }

    private String toJson(String name) {
        return JsonMapper.builder().build().writeValueAsString(new SampleCreateRequest(name));
    }

}
