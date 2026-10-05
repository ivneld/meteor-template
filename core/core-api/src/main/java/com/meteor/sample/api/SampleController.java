package com.meteor.sample.api;

import com.meteor.sample.api.request.SampleCreateRequest;
import com.meteor.sample.api.response.SampleResponse;
import com.meteor.sample.application.SampleUseCase;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP ↔ Command/Result 변환만 한다. 엔드포인트 하나는 UseCase 메서드 하나를 호출한다 (R-03).
 */
@RestController
@RequestMapping("/api/v1/samples")
public class SampleController {

    private final SampleUseCase sampleUseCase;

    public SampleController(SampleUseCase sampleUseCase) {
        this.sampleUseCase = sampleUseCase;
    }

    @GetMapping("/{sampleId}")
    public SampleResponse getSample(@PathVariable Long sampleId) {
        return SampleResponse.from(sampleUseCase.find(sampleId));
    }

    @PostMapping
    public SampleResponse createSample(@RequestBody @Valid SampleCreateRequest request) {
        return SampleResponse.from(sampleUseCase.create(request.toCommand()));
    }

    @PostMapping("/{sampleId}/deactivate")
    public SampleResponse deactivateSample(@PathVariable Long sampleId) {
        return SampleResponse.from(sampleUseCase.deactivate(sampleId));
    }

}
