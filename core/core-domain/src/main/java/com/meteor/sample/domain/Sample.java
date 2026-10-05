package com.meteor.sample.domain;

import java.time.LocalDateTime;

import com.meteor.shared.SampleStatus;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

/**
 * 샘플 애그리거트. 식별자를 가진 가변 객체이며, 상태 전이 규칙은 모두 이 클래스의 메서드에 있다.
 *
 * <p>
 * 영속성 기술(JPA)은 storage 어댑터가 맡는다. 이 클래스는 저장 방식을 모르며, 상태를 바꾼 뒤 저장하는 책임은 UseCase 에 있다.
 */
public class Sample {

    private final Long id;

    private final String name;

    private SampleStatus status;

    private final LocalDateTime createdAt;

    private Sample(Long id, String name, SampleStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** 새 샘플을 만든다. 식별자는 저장 시점에 부여된다. */
    public static Sample create(String name) {
        return new Sample(null, name, SampleStatus.ACTIVE, LocalDateTime.now());
    }

    /** 저장소에서 복원한다. storage 어댑터만 호출한다. */
    public static Sample restore(Long id, String name, SampleStatus status, LocalDateTime createdAt) {
        return new Sample(id, name, status, createdAt);
    }

    public void deactivate() {
        if (this.status == SampleStatus.INACTIVE) {
            throw new CoreException(ErrorCode.SAMPLE_ALREADY_INACTIVE).property("sampleId", id);
        }
        this.status = SampleStatus.INACTIVE;
    }

    public boolean isNew() {
        return id == null;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SampleStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}
