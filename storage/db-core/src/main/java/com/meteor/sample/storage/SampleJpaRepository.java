package com.meteor.sample.storage;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data 인터페이스. 어댑터 밖으로 노출하지 않는다.
 */
interface SampleJpaRepository extends JpaRepository<SampleEntity, Long> {

}
