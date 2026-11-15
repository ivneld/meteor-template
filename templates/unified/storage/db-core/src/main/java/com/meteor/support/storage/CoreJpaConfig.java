package com.meteor.support.storage;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * com.meteor 아래의 엔티티와 Spring Data 인터페이스를 스캔한다. 둘 다 core-api 의 com.meteor.domain 에
 * 있다(애그리거트와 *Repository).
 */
@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = "com.meteor")
@EnableJpaRepositories(basePackages = "com.meteor")
class CoreJpaConfig {

}
