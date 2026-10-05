package com.meteor.support.client.example;

import com.meteor.support.client.example.model.ExampleClientResult;

import org.springframework.stereotype.Component;

/**
 * 외부 연동의 공개 진입점. 재시도·폴백·에러 변환 같은 정책은 이 클래스에 모은다.
 */
@Component
public class ExampleClient {

    private final ExampleApi exampleApi;

    public ExampleClient(ExampleApi exampleApi) {
        this.exampleApi = exampleApi;
    }

    public ExampleClientResult create(String message) {
        return exampleApi.create(new ExampleRequestDto(message)).toResult();
    }

}
