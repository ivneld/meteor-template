package com.meteor.sample.domain;

import java.util.Optional;

/**
 * 샘플 애그리거트의 리포지토리 포트. 구현은 storage 모듈에 있고, 도메인 객체만 주고받는다.
 */
public interface SampleRepository {

    Optional<Sample> findById(Long id);

    Sample save(Sample sample);

}
