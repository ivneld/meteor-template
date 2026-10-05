package com.meteor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 이 모듈만 단독으로 컨텍스트 테스트할 때 사용하는 진입점. com.meteor 아래 storage 설정과 어댑터를 스캔한다.
 */
@ConfigurationPropertiesScan
@SpringBootApplication
public class CoreDbTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreDbTestApplication.class, args);
    }

}
