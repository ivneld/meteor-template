package com.meteor.sample.storage;

import com.meteor.sample.domain.Sample;
import com.meteor.shared.SampleStatus;
import com.meteor.support.storage.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * 샘플 애그리거트의 영속 모델. 이 모듈 밖으로 나가지 않는다 (R-08). 도메인 객체와의 변환도 여기서 끝난다.
 */
@Entity
@Table(name = "sample")
class SampleEntity extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SampleStatus status;

    protected SampleEntity() {
    }

    private SampleEntity(Sample sample) {
        super(sample.getCreatedAt());
        this.name = sample.getName();
        this.status = sample.getStatus();
    }

    static SampleEntity from(Sample sample) {
        return new SampleEntity(sample);
    }

    /** 도메인 객체의 현재 상태를 엔티티에 반영한다. 식별자와 생성 시각은 바뀌지 않는다. */
    void apply(Sample sample) {
        this.name = sample.getName();
        this.status = sample.getStatus();
    }

    Sample toDomain() {
        return Sample.restore(getId(), name, status, getCreatedAt());
    }

}
