package com.meteor.support.fixture;

import java.lang.reflect.Field;

import com.meteor.support.storage.BaseEntity;

/**
 * 컨텍스트 없이 쓰는 테스트(웹 슬라이스 등)에서 "저장된" 애그리거트를 만든다. 애그리거트가 곧 엔티티라 식별자는 저장할 때 JPA 가 채우므로, 저장소
 * 없이 식별자가 필요할 때만 이것으로 넣는다. main 코드에서는 쓰지 않는다.
 */
public final class Persisted {

    private Persisted() {
    }

    public static <T extends BaseEntity> T withId(T entity, Long id) {
        try {
            Field field = BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        }
        catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
