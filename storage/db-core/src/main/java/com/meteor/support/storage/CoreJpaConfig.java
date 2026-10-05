package com.meteor.support.storage;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 컨텍스트별 storage 패키지(com.meteor.&lt;context&gt;.storage)를 모두 스캔한다.
 */
@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = "com.meteor")
@EnableJpaRepositories(basePackages = "com.meteor")
class CoreJpaConfig {

}
