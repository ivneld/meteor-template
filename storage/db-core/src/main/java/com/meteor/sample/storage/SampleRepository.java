package com.meteor.sample.storage;

import java.util.Optional;

import com.meteor.sample.domain.Sample;

/**
 * 샘플 애그리거트의 저장소. 도메인 객체만 주고받으며, JPA 엔티티와의 변환은 이 모듈 안에서 끝난다.
 *
 * <p>
 * application 계층은 이 인터페이스만 본다. 구현({@code SampleRepositoryAdapter})과 Spring Data 인터페이스는
 * package-private 이다.
 */
public interface SampleRepository {

    Optional<Sample> findById(Long id);

    Sample save(Sample sample);

}
