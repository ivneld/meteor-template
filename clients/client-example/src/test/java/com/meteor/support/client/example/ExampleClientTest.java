package com.meteor.support.client.example;

import com.meteor.ClientExampleContextTest;
import feign.FeignException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExampleClientTest extends ClientExampleContextTest {

    private final ExampleClient exampleClient;

    ExampleClientTest(ExampleClient exampleClient) {
        this.exampleClient = exampleClient;
    }

    @Test
    void feignClientIsWiredAndFailsAgainstUnreachableHost() {
        // local 프로필은 존재하지 않는 호스트(.invalid)를 바라보므로 Feign 예외로 끝나야 한다.
        assertThatThrownBy(() -> exampleClient.create("hello")).isInstanceOf(FeignException.class);
    }

}
