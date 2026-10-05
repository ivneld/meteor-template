package com.meteor.sample.application;

import com.meteor.sample.domain.Sample;
import com.meteor.sample.storage.SampleRepository;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 샘플 컨텍스트의 유스케이스. 트랜잭션 경계는 이 계층에만 있다 (R-02).
 *
 * <p>
 * 하는 일은 "꺼내고, 도메인 메서드를 부르고, 저장한다" 뿐이다. 도메인 상태로 분기하는 규칙은 애그리거트에 둔다 (S-01).
 */
@Service
public class SampleUseCase {

    private final SampleRepository sampleRepository;

    public SampleUseCase(SampleRepository sampleRepository) {
        this.sampleRepository = sampleRepository;
    }

    @Transactional(readOnly = true)
    public SampleResult find(Long sampleId) {
        return SampleResult.from(load(sampleId));
    }

    @Transactional
    public SampleResult create(SampleCreateCommand command) {
        Sample saved = sampleRepository.save(Sample.create(command.name()));
        return SampleResult.from(saved);
    }

    @Transactional
    public SampleResult deactivate(Long sampleId) {
        Sample sample = load(sampleId);
        sample.deactivate();
        return SampleResult.from(sampleRepository.save(sample));
    }

    private Sample load(Long sampleId) {
        return sampleRepository.findById(sampleId)
            .orElseThrow(() -> new CoreException(ErrorCode.SAMPLE_NOT_FOUND).property("sampleId", sampleId));
    }

}
