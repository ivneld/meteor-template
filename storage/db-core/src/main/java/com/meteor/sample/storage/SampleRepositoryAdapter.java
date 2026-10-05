package com.meteor.sample.storage;

import java.util.Optional;

import com.meteor.sample.domain.Sample;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Repository;

/**
 * {@link SampleRepository} 의 JPA 구현. 조회·변환·저장을 캡슐화하고 도메인 객체만 주고받는다.
 *
 * <p>
 * 더티 체킹에 기대지 않는다. UseCase 가 도메인 객체를 바꿨으면 {@link #save(Sample)} 를 명시적으로 호출해야 한다.
 */
@Repository
class SampleRepositoryAdapter implements SampleRepository {

    private final SampleJpaRepository sampleJpaRepository;

    SampleRepositoryAdapter(SampleJpaRepository sampleJpaRepository) {
        this.sampleJpaRepository = sampleJpaRepository;
    }

    @Override
    public Optional<Sample> findById(Long id) {
        return sampleJpaRepository.findById(id).map(SampleEntity::toDomain);
    }

    @Override
    public Sample save(Sample sample) {
        SampleEntity entity;
        if (sample.isNew()) {
            entity = SampleEntity.from(sample);
        }
        else {
            entity = sampleJpaRepository.findById(sample.getId())
                .orElseThrow(() -> new CoreException(ErrorCode.SAMPLE_NOT_FOUND).property("sampleId", sample.getId()));
            entity.apply(sample);
        }
        return sampleJpaRepository.save(entity).toDomain();
    }

}
