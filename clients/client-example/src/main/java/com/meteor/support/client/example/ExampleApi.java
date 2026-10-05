package com.meteor.support.client.example;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 외부 API 의 HTTP 계약. 모듈 밖으로는 노출하지 않고 {@link ExampleClient} 를 통해서만 사용한다.
 *
 * <p>
 * 특정 컨텍스트 전용 어댑터라면 {@code com.meteor.<context>.clients} 패키지에 둔다.
 */
@FeignClient(value = "example-api", url = "${client.example.url}")
interface ExampleApi {

    @PostMapping(value = "/v1/examples", consumes = MediaType.APPLICATION_JSON_VALUE)
    ExampleResponseDto create(@RequestBody ExampleRequestDto request);

}
